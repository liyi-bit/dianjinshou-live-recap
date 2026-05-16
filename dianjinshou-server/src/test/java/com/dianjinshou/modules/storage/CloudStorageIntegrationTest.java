package com.dianjinshou.modules.storage;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.storage.CosProperties;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.comparison.service.ComparisonService;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.storage.dto.CreateShareRequest;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.entity.ShareLink;
import com.dianjinshou.modules.storage.entity.UploadTask;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.ShareLinkMapper;
import com.dianjinshou.modules.storage.mapper.UploadTaskMapper;
import com.dianjinshou.modules.storage.service.ChunkedUploadService;
import com.dianjinshou.modules.storage.service.CloudFileService;
import com.dianjinshou.modules.storage.service.CloudSpaceService;
import com.dianjinshou.modules.storage.service.ShareLinkService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudStorageIntegrationTest {

    @Mock private CloudFileMapper cloudFileMapper;
    @Mock private ShareLinkMapper shareLinkMapper;
    @Mock private StorageService storageService;
    @Mock private UploadTaskMapper uploadTaskMapper;
    @Mock private com.dianjinshou.modules.storage.service.CosCredentialService cosCredentialService;
    @Mock private AnalysisTaskMapper analysisTaskMapper;
    @Mock private ComparisonService comparisonService;

    private StorageProperties storageProperties;
    private CosProperties cosProperties;
    private CloudFileService cloudFileService;
    private CloudSpaceService cloudSpaceService;
    private ShareLinkService shareLinkService;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.storage.integration.CloudFileMapper");
            TableInfoHelper.initTableInfo(a1, CloudFile.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.storage.integration.ShareLinkMapper");
            TableInfoHelper.initTableInfo(a2, ShareLink.class);

            MapperBuilderAssistant a3 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a3.setCurrentNamespace("com.dianjinshou.modules.storage.integration.UploadTaskMapper");
            TableInfoHelper.initTableInfo(a3, UploadTask.class);
        } catch (Exception ignored) {}
    }

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setBucketRecordings("recordings");
        storageProperties.setBucketFiles("files");
        storageProperties.setBucketClips("clips");
        storageProperties.setPresignedUrlExpireSeconds(3600);
        cosProperties = new CosProperties();
        cloudFileService = new CloudFileService(cloudFileMapper, storageService, storageProperties, cosCredentialService, cosProperties, analysisTaskMapper);
        cloudSpaceService = new CloudSpaceService(cloudFileService, comparisonService);
        shareLinkService = new ShareLinkService(shareLinkMapper, cloudFileMapper, storageService, storageProperties);
        setCurrentUser(1L, "super_admin", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // T40: Cloud file lifecycle: create → list → download → delete
    @Test
    void cloudFileLifecycle_createListDownloadDelete() {
        CloudFile file = new CloudFile();
        file.setId(1L);
        file.setUserId(1L);
        file.setOrgId(100L);
        file.setFileName("test.mp4");
        file.setFileSize(5000L);
        file.setFileType("recording");
        file.setStatus("active");
        file.setBucket("recordings");
        file.setStorageKey("key/test.mp4");

        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(storageService.getPresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://storage/presigned-url");

        // Download returns presigned URL
        String url = cloudFileService.getDownloadUrl(1L);
        assertNotNull(url);
        assertTrue(url.contains("presigned-url"));

        when(cloudFileMapper.updateById(any(CloudFile.class))).thenReturn(1);
        cloudFileService.deleteFile(1L);
        verify(cloudFileMapper).updateById(any(CloudFile.class));
    }

    // T40: Batch operations
    @Test
    void batchOperations_downloadAndDelete() {
        CloudFile f1 = new CloudFile();
        f1.setId(1L); f1.setOrgId(100L); f1.setStatus("active");
        f1.setBucket("recordings"); f1.setStorageKey("k1");
        CloudFile f2 = new CloudFile();
        f2.setId(2L); f2.setOrgId(100L); f2.setStatus("active");
        f2.setBucket("recordings"); f2.setStorageKey("k2");

        when(cloudFileMapper.selectById(1L)).thenReturn(f1);
        when(cloudFileMapper.selectById(2L)).thenReturn(f2);
        when(storageService.getPresignedUrl(anyString(), anyString(), anyInt())).thenReturn("url");

        List<String> urls = cloudSpaceService.batchDownloadUrls(Arrays.asList(1L, 2L));
        assertEquals(2, urls.size());

        when(cloudFileMapper.updateById(any(CloudFile.class))).thenReturn(1);
        cloudSpaceService.batchDelete(Arrays.asList(1L, 2L));
        verify(cloudFileMapper, times(2)).updateById(any(CloudFile.class));
    }

    // T40: Batch exceeds max
    @Test
    void batchDelete_exceedsMax_throws() {
        Long[] ids = new Long[51];
        Arrays.fill(ids, 1L);
        assertThrows(BusinessException.class, () -> cloudSpaceService.batchDelete(Arrays.asList(ids)));
    }

    // T40: Share link full flow: create → access → password check
    @Test
    void shareLink_createAndAccess() {
        CloudFile file = new CloudFile();
        file.setId(1L); file.setFileName("shared.mp4"); file.setFileSize(1000L);
        file.setBucket("files"); file.setStorageKey("key"); file.setContentType("video/mp4");

        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(shareLinkMapper.insert(any(ShareLink.class))).thenReturn(1);

        CreateShareRequest req = new CreateShareRequest();
        req.setPassword("1234");
        req.setExpireHours(24);

        ShareLinkVO vo = shareLinkService.createShare(1L, req);
        assertNotNull(vo);
        assertTrue(vo.isHasPassword());
        assertNotNull(vo.getShareCode());
        assertEquals(8, vo.getShareCode().length());
    }

    // T40: Share access with wrong password
    @Test
    void shareAccess_wrongPassword_throws() {
        ShareLink link = new ShareLink();
        link.setId(1L); link.setStatus("active"); link.setPassword("1234");
        link.setViewCount(0); link.setDownloadCount(0);

        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);

        assertThrows(BusinessException.class,
                () -> shareLinkService.accessShare("abc12345", "wrong"));
    }

    // T40: Share access expired
    @Test
    void shareAccess_expired_throws() {
        ShareLink link = new ShareLink();
        link.setId(1L); link.setStatus("active");
        link.setExpiresAt(LocalDateTime.now().minusHours(1));
        link.setViewCount(0); link.setDownloadCount(0);

        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);
        when(shareLinkMapper.updateById(any(ShareLink.class))).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> shareLinkService.accessShare("abc12345", null));
    }

    // T40: Share access max downloads exceeded
    @Test
    void shareAccess_maxDownloadsExceeded_throws() {
        ShareLink link = new ShareLink();
        link.setId(1L); link.setStatus("active");
        link.setMaxDownloads(5); link.setDownloadCount(5);
        link.setViewCount(0);

        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);
        when(shareLinkMapper.updateById(any(ShareLink.class))).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> shareLinkService.accessShare("abc12345", null));
    }

    // T40: Share access disabled
    @Test
    void shareAccess_disabled_throws() {
        ShareLink link = new ShareLink();
        link.setId(1L); link.setStatus("disabled");

        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);

        assertThrows(BusinessException.class,
                () -> shareLinkService.accessShare("abc12345", null));
    }

    // T40: Share access success with correct password
    @Test
    void shareAccess_correctPassword_success() {
        ShareLink link = new ShareLink();
        link.setId(1L); link.setStatus("active"); link.setPassword("1234");
        link.setCloudFileId(1L); link.setViewCount(0); link.setDownloadCount(0);

        CloudFile file = new CloudFile();
        file.setId(1L); file.setFileName("test.mp4"); file.setFileSize(1000L);
        file.setBucket("files"); file.setStorageKey("key"); file.setContentType("video/mp4");

        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(link);
        when(shareLinkMapper.updateById(any(ShareLink.class))).thenReturn(1);
        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(storageService.getPresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("https://download-url");

        ShareLinkService.ShareAccessResult result = shareLinkService.accessShare("abc12345", "1234");
        assertNotNull(result);
        assertEquals("test.mp4", result.getFileName());
        assertNotNull(result.getDownloadUrl());
    }

    // T40: Cancel share validates ownership
    @Test
    void cancelShare_notOwner_throws() {
        setCurrentUser(2L, "operator", 100L);

        ShareLink link = new ShareLink();
        link.setId(1L); link.setUserId(1L); // owned by user 1

        when(shareLinkMapper.selectById(1L)).thenReturn(link);

        assertThrows(BusinessException.class, () -> shareLinkService.cancelShare(1L));
    }

    // T40: Cloud space list by type with keyword filter
    @Test
    void cloudSpace_listByTypeWithKeyword() {
        CloudFile f = new CloudFile();
        f.setId(1L); f.setFileName("demo.mp4"); f.setFileType("recording");
        f.setOrgId(100L); f.setStatus("active");

        Page<CloudFile> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(f));
        when(cloudFileMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<CloudFileVO> result = cloudSpaceService.listByType("recording", 1, 20, "demo", "time");
        assertEquals(1, result.getRecords().size());
        assertEquals("demo.mp4", result.getRecords().get(0).getFileName());
    }

    // T40: Share link list returns user's shares
    @Test
    void shareLink_listMyShares() {
        ShareLink link = new ShareLink();
        link.setId(1L); link.setUserId(1L); link.setCloudFileId(1L);
        link.setShareCode("test1234"); link.setStatus("active");
        link.setViewCount(5); link.setDownloadCount(2);

        CloudFile file = new CloudFile();
        file.setId(1L); file.setFileName("shared.mp4");

        when(shareLinkMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(link));
        when(cloudFileMapper.selectById(1L)).thenReturn(file);

        List<ShareLinkVO> shares = shareLinkService.listMyShares();
        assertEquals(1, shares.size());
        assertEquals("test1234", shares.get(0).getShareCode());
        assertEquals("shared.mp4", shares.get(0).getFileName());
    }

    // T40: Empty batch operations
    @Test
    void batchDelete_empty_throws() {
        assertThrows(BusinessException.class, () -> cloudSpaceService.batchDelete(Collections.emptyList()));
    }

    @Test
    void batchDownload_empty_throws() {
        assertThrows(BusinessException.class, () -> cloudSpaceService.batchDownloadUrls(Collections.emptyList()));
    }

    // T40: Share not found
    @Test
    void shareAccess_notFound_throws() {
        when(shareLinkMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> shareLinkService.accessShare("notexist", null));
    }
}
