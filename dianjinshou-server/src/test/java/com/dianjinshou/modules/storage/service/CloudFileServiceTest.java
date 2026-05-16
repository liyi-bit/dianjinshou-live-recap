package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.storage.CosProperties;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudUsageVO;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudFileServiceTest {

    @Mock
    private CloudFileMapper cloudFileMapper;
    @Mock
    private StorageService storageService;
    @Mock
    private CosCredentialService cosCredentialService;
    @Mock
    private AnalysisTaskMapper analysisTaskMapper;

    private StorageProperties storageProperties;
    private CosProperties cosProperties;
    private CloudFileService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("com.dianjinshou.modules.storage.mapper.CloudFileMapper");
        TableInfoHelper.initTableInfo(assistant, CloudFile.class);
    }

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setPresignedUrlExpireSeconds(3600);
        cosProperties = new CosProperties();
        cosProperties.setQuotaBytes(1L * 1024 * 1024 * 1024);
        service = new CloudFileService(cloudFileMapper, storageService, storageProperties, cosCredentialService, cosProperties, analysisTaskMapper);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listFiles_withFilters() {
        Page<CloudFile> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(cloudFileMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<CloudFileVO> result = service.listFiles(1, 20, "recording", "test");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        verify(cloudFileMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void deleteFile_success() {
        CloudFile file = buildFile(1L, 100L);
        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(cloudFileMapper.updateById(any(CloudFile.class))).thenReturn(1);

        service.deleteFile(1L);

        verify(storageService).delete("files", "org100/user1/test.mp4");
        verify(cloudFileMapper).updateById(any(CloudFile.class));
    }

    @Test
    void deleteFile_notFound() {
        when(cloudFileMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.deleteFile(999L));
    }

    @Test
    void deleteFile_crossUser_rejected() {
        CloudFile file = buildFile(1L, 200L); // different org
        file.setUserId(2L);
        when(cloudFileMapper.selectById(1L)).thenReturn(file);

        assertThrows(BusinessException.class, () -> service.deleteFile(1L));
    }

    @Test
    void getDownloadUrl_success() {
        CloudFile file = buildFile(1L, 100L);
        when(cloudFileMapper.selectById(1L)).thenReturn(file);
        when(storageService.getPresignedUrl(anyString(), anyString(), anyInt()))
                .thenReturn("http://minio/files/key?token=abc");

        String url = service.getDownloadUrl(1L);

        assertNotNull(url);
        assertTrue(url.contains("token"));
    }

    @Test
    void getUsage_success() {
        CloudFile f1 = buildFile(1L, 100L);
        f1.setFileSize(500L * 1024 * 1024); // 500MB
        CloudFile f2 = buildFile(2L, 100L);
        f2.setFileSize(300L * 1024 * 1024); // 300MB

        when(cloudFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(f1, f2));

        CloudUsageVO usage = service.getUsage(0); // free tier = 1GB

        assertNotNull(usage);
        assertEquals(800L * 1024 * 1024, usage.getUsedBytes());
        assertEquals(1L * 1024 * 1024 * 1024, usage.getTotalQuotaBytes());
        assertEquals(2, usage.getFileCount());
        assertTrue(usage.getUsagePercent() > 70); // ~78%
    }

    @Test
    void checkQuota_withinLimit() {
        when(cloudFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.checkQuota(100L, 0, 500L * 1024 * 1024));
    }

    @Test
    void checkQuota_exceedsLimit() {
        CloudFile f = buildFile(1L, 100L);
        f.setFileSize(900L * 1024 * 1024); // 900MB used, free tier=1GB
        when(cloudFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(f));

        assertThrows(BusinessException.class,
                () -> service.checkQuota(100L, 0, 200L * 1024 * 1024)); // 200MB more → over
    }

    @Test
    void createCloudFile_success() {
        when(cloudFileMapper.insert(any(CloudFile.class))).thenAnswer(inv -> {
            CloudFile cf = inv.getArgument(0);
            cf.setId(1L);
            return 1;
        });

        CloudFile result = service.createCloudFile(1L, 100L, "test.mp4",
                "org100/user1/test.mp4", "files", 1024L, "video/mp4", "recording", 10L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("active", result.getStatus());
        verify(cloudFileMapper).insert(any(CloudFile.class));
    }

    private CloudFile buildFile(Long id, Long orgId) {
        CloudFile file = new CloudFile();
        file.setId(id);
        file.setUserId(1L);
        file.setOrgId(orgId);
        file.setFileName("test.mp4");
        file.setStorageKey("org100/user1/test.mp4");
        file.setBucket("files");
        file.setFileSize(1024L);
        file.setContentType("video/mp4");
        file.setFileType("recording");
        file.setDownloadCount(0);
        file.setShareCount(0);
        file.setStatus("active");
        return file;
    }
}
