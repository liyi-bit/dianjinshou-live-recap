package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.vo.ShortClipStatsVO;
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

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminShortClipServiceTest {

    @Mock
    private ShortClipMapper shortClipMapper;

    private AdminShortClipService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace("com.dianjinshou.modules.shortclip.mapper.ShortClipMapper3");
            TableInfoHelper.initTableInfo(assistant, ShortClip.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new AdminShortClipService(shortClipMapper);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listAll_asAdmin_success() {
        setCurrentUser(1L, "admin", 100L);
        Page<ShortClip> page = new Page<>(1, 20, 1);
        ShortClip clip = new ShortClip();
        clip.setId(1L);
        clip.setOrgId(100L);
        clip.setClipName("测试");
        clip.setStatus("completed");
        page.setRecords(Arrays.asList(clip));
        when(shortClipMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<ShortClipVO> result = service.listAll(1, 20, null, null);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals("测试", result.getItems().get(0).getClipName());
    }

    @Test
    void listAll_nonAdmin_rejected() {
        setCurrentUser(1L, "operator", 100L);

        assertThrows(BusinessException.class, () -> service.listAll(1, 20, null, null));
    }

    @Test
    void getStats_asAdmin_success() {
        setCurrentUser(1L, "super_admin", 100L);
        when(shortClipMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
        ShortClip clip = new ShortClip();
        clip.setFileSize(1024L);
        when(shortClipMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(clip));

        ShortClipStatsVO stats = service.getStats();

        assertNotNull(stats);
        assertEquals(10L, stats.getTotalClips());
        assertEquals(1024L, stats.getTotalFileSize());
    }
}
