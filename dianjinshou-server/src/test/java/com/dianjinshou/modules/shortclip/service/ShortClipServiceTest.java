package com.dianjinshou.modules.shortclip.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.shortclip.dto.CreateShortClipRequest;
import com.dianjinshou.modules.shortclip.entity.ShortClip;
import com.dianjinshou.modules.shortclip.mapper.ShortClipMapper;
import com.dianjinshou.modules.shortclip.vo.ShortClipVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortClipServiceTest {

    @Mock
    private ShortClipMapper shortClipMapper;
    @Mock
    private StorageService storageService;

    private StorageProperties storageProperties;
    private ShortClipService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("com.dianjinshou.modules.shortclip.mapper.ShortClipMapper");
        TableInfoHelper.initTableInfo(assistant, ShortClip.class);
    }

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        service = new ShortClipService(shortClipMapper, storageService, storageProperties);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createClip_success() {
        when(shortClipMapper.insert(any(ShortClip.class))).thenAnswer(inv -> {
            ShortClip c = inv.getArgument(0);
            c.setId(1L);
            return 1;
        });

        CreateShortClipRequest req = new CreateShortClipRequest();
        req.setRecordingId(10L);
        req.setStartTime(60);
        req.setEndTime(120);
        req.setClipName("精彩片段");

        ShortClipVO result = service.createClip(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(60, result.getDuration());
        assertEquals("pending", result.getStatus());
    }

    @Test
    void createClip_tooShort_rejected() {
        CreateShortClipRequest req = new CreateShortClipRequest();
        req.setRecordingId(10L);
        req.setStartTime(60);
        req.setEndTime(62); // only 2 seconds
        req.setClipName("短片");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createClip(req));
        assertTrue(ex.getMessage().contains("3秒"));
    }

    @Test
    void createClip_tooLong_rejected() {
        CreateShortClipRequest req = new CreateShortClipRequest();
        req.setRecordingId(10L);
        req.setStartTime(0);
        req.setEndTime(400); // 400 seconds > 300
        req.setClipName("长片");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createClip(req));
        assertTrue(ex.getMessage().contains("5分钟"));
    }

    @Test
    void createClip_nameTooLong_rejected() {
        CreateShortClipRequest req = new CreateShortClipRequest();
        req.setRecordingId(10L);
        req.setStartTime(10);
        req.setEndTime(30);
        req.setClipName("这个切片名字特别特别特别特别特别长超过十五个字");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createClip(req));
        assertTrue(ex.getMessage().contains("15个字符"));
    }

    @Test
    void listClips_withFilters() {
        Page<ShortClip> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(shortClipMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<ShortClipVO> result = service.listClips(1, 20, 10L, "completed");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void getClip_success() {
        ShortClip clip = buildClip(1L, 100L);
        when(shortClipMapper.selectById(1L)).thenReturn(clip);

        ShortClipVO result = service.getClip(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getClip_notFound() {
        when(shortClipMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getClip(999L));
    }

    @Test
    void getClip_crossOrg_rejected() {
        ShortClip clip = buildClip(1L, 200L);
        when(shortClipMapper.selectById(1L)).thenReturn(clip);
        assertThrows(BusinessException.class, () -> service.getClip(1L));
    }

    @Test
    void deleteClip_success() {
        ShortClip clip = buildClip(1L, 100L);
        when(shortClipMapper.selectById(1L)).thenReturn(clip);
        when(shortClipMapper.deleteById(1L)).thenReturn(1);

        service.deleteClip(1L);
        verify(shortClipMapper).deleteById(1L);
    }

    private ShortClip buildClip(Long id, Long orgId) {
        ShortClip clip = new ShortClip();
        clip.setId(id);
        clip.setUserId(1L);
        clip.setOrgId(orgId);
        clip.setRecordingId(10L);
        clip.setSourceType("recording");
        clip.setSourceId(10L);
        clip.setClipName("测试");
        clip.setStartTime(60);
        clip.setEndTime(120);
        clip.setDuration(60);
        clip.setResolution("original");
        clip.setOutputFormat("mp4");
        clip.setFileSize(1024L);
        clip.setStatus("completed");
        return clip;
    }
}
