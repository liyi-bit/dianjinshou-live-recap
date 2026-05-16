package com.dianjinshou.modules.shortclip.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.shortclip.entity.ShortClip;
import com.dianjinshou.modules.shortclip.mapper.ShortClipMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClipExportServiceTest {

    @Mock
    private ShortClipMapper shortClipMapper;
    @Mock
    private StorageService storageService;

    private ClipExportService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace("com.dianjinshou.modules.shortclip.mapper.ShortClipMapper2");
            TableInfoHelper.initTableInfo(assistant, ShortClip.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new ClipExportService(shortClipMapper, storageService, new StorageProperties());
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void batchExport_success() {
        ShortClip clip = buildClip(1L, 100L, "completed");
        when(shortClipMapper.selectById(1L)).thenReturn(clip);

        String key = service.batchExport(Arrays.asList(1L));

        assertNotNull(key);
        assertTrue(key.startsWith("exports/"));
    }

    @Test
    void batchExport_emptyList_rejected() {
        assertThrows(BusinessException.class, () -> service.batchExport(Collections.emptyList()));
    }

    @Test
    void batchExport_clipNotCompleted_rejected() {
        ShortClip clip = buildClip(1L, 100L, "processing");
        when(shortClipMapper.selectById(1L)).thenReturn(clip);

        assertThrows(BusinessException.class, () -> service.batchExport(Arrays.asList(1L)));
    }

    @Test
    void uploadToCloud_success() {
        ShortClip clip = buildClip(1L, 100L, "completed");
        clip.setStorageKey(null);
        when(shortClipMapper.selectById(1L)).thenReturn(clip);
        when(shortClipMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.uploadToCloud(1L);

        verify(shortClipMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void uploadToCloud_alreadyUploaded_rejected() {
        ShortClip clip = buildClip(1L, 100L, "completed");
        clip.setStorageKey("already/uploaded");
        when(shortClipMapper.selectById(1L)).thenReturn(clip);

        assertThrows(BusinessException.class, () -> service.uploadToCloud(1L));
    }

    private ShortClip buildClip(Long id, Long orgId, String status) {
        ShortClip clip = new ShortClip();
        clip.setId(id);
        clip.setUserId(1L);
        clip.setOrgId(orgId);
        clip.setClipName("测试切片");
        clip.setStatus(status);
        return clip;
    }
}
