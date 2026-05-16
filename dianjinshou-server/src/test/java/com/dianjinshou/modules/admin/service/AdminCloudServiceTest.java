package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.vo.CloudStatsVO;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.entity.ShareLink;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.ShareLinkMapper;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.ShareLinkVO;
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
class AdminCloudServiceTest {

    @Mock private CloudFileMapper cloudFileMapper;
    @Mock private ShareLinkMapper shareLinkMapper;

    private AdminCloudService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.admin.cloud.CloudFileMapper");
            TableInfoHelper.initTableInfo(a1, CloudFile.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.admin.cloud.ShareLinkMapper");
            TableInfoHelper.initTableInfo(a2, ShareLink.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new AdminCloudService(cloudFileMapper, shareLinkMapper);
        setCurrentUser(1L, "super_admin", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getStats_success() {
        CloudFile f1 = new CloudFile();
        f1.setFileSize(1000L);
        f1.setFileType("recording");
        CloudFile f2 = new CloudFile();
        f2.setFileSize(2000L);
        f2.setFileType("clip");

        when(cloudFileMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(cloudFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(f1, f2));
        when(shareLinkMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        CloudStatsVO stats = service.getStats();

        assertEquals(2L, stats.getTotalFiles());
        assertEquals(3000L, stats.getTotalSize());
        assertEquals(1L, stats.getRecordingCount());
        assertEquals(1L, stats.getClipCount());
        assertEquals(5L, stats.getActiveShareCount());
    }

    @Test
    void getStats_nonAdmin_throws() {
        setCurrentUser(2L, "operator", 100L);
        assertThrows(BusinessException.class, () -> service.getStats());
    }

    @Test
    void disableShare_success() {
        ShareLink link = new ShareLink();
        link.setId(1L);
        link.setStatus("active");
        when(shareLinkMapper.selectById(1L)).thenReturn(link);
        when(shareLinkMapper.updateById(any(ShareLink.class))).thenReturn(1);

        service.disableShare(1L);

        assertEquals("disabled", link.getStatus());
        verify(shareLinkMapper).updateById(link);
    }

    @Test
    void disableShare_notFound_throws() {
        when(shareLinkMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.disableShare(999L));
    }
}
