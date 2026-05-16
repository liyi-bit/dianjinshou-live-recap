package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.storage.dto.CreateShareRequest;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.entity.ShareLink;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.ShareLinkMapper;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock private ShareLinkMapper shareLinkMapper;
    @Mock private CloudFileMapper cloudFileMapper;
    @Mock private StorageService storageService;
    @Mock private StorageProperties storageProperties;

    private ShareLinkService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.storage.mapper.ShareLinkMapper");
            TableInfoHelper.initTableInfo(a1, ShareLink.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.storage.mapper.CloudFileMapper3");
            TableInfoHelper.initTableInfo(a2, CloudFile.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new ShareLinkService(shareLinkMapper, cloudFileMapper, storageService, storageProperties);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createShare_success() {
        CloudFile file = new CloudFile();
        file.setId(1L);
        file.setFileName("test.mp4");
        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(shareLinkMapper.insert(any(ShareLink.class))).thenReturn(1);

        CreateShareRequest req = new CreateShareRequest();
        req.setPassword("1234");
        req.setExpireHours(24);

        ShareLinkVO result = service.createShare(1L, req);

        assertNotNull(result);
        assertNotNull(result.getShareCode());
        assertTrue(result.isHasPassword());
        assertEquals("test.mp4", result.getFileName());
    }

    @Test
    void createShare_fileNotFound_throws() {
        when(cloudFileMapper.selectById(999L)).thenReturn(null);
        CreateShareRequest req = new CreateShareRequest();
        assertThrows(BusinessException.class, () -> service.createShare(999L, req));
    }

    @Test
    void accessShare_success() {
        ShareLink link = new ShareLink();
        link.setId(1L);
        link.setShareCode("abc12345");
        link.setCloudFileId(1L);
        link.setStatus("active");
        link.setViewCount(0);
        link.setDownloadCount(0);
        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);
        when(shareLinkMapper.updateById(any(ShareLink.class))).thenReturn(1);

        CloudFile file = new CloudFile();
        file.setId(1L);
        file.setFileName("video.mp4");
        file.setFileSize(5000L);
        file.setContentType("video/mp4");
        file.setBucket("recordings");
        file.setStorageKey("key123");
        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(storageProperties.getPresignedUrlExpireSeconds()).thenReturn(3600);
        when(storageService.getPresignedUrl(anyString(), anyString(), anyInt())).thenReturn("https://download-url");

        ShareLinkService.ShareAccessResult result = service.accessShare("abc12345", null);

        assertEquals("video.mp4", result.getFileName());
        assertEquals("https://download-url", result.getDownloadUrl());
    }

    @Test
    void accessShare_wrongPassword_throws() {
        ShareLink link = new ShareLink();
        link.setShareCode("abc12345");
        link.setStatus("active");
        link.setPassword("1234");
        link.setViewCount(0);
        link.setDownloadCount(0);
        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);

        assertThrows(BusinessException.class, () -> service.accessShare("abc12345", "wrong"));
    }

    @Test
    void accessShare_expired_throws() {
        ShareLink link = new ShareLink();
        link.setShareCode("abc12345");
        link.setStatus("active");
        link.setExpiresAt(LocalDateTime.now().minusHours(1));
        link.setViewCount(0);
        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);
        when(shareLinkMapper.updateById(any(ShareLink.class))).thenReturn(1);

        assertThrows(BusinessException.class, () -> service.accessShare("abc12345", null));
    }

    @Test
    void cancelShare_otherUser_throws() {
        ShareLink link = new ShareLink();
        link.setId(1L);
        link.setUserId(999L);
        when(shareLinkMapper.selectById(1L)).thenReturn(link);

        assertThrows(BusinessException.class, () -> service.cancelShare(1L));
    }
}
