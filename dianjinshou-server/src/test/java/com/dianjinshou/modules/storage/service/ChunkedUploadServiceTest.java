package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.storage.dto.InitUploadRequest;
import com.dianjinshou.modules.storage.entity.UploadTask;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.UploadTaskMapper;
import com.dianjinshou.modules.storage.vo.UploadInitVO;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkedUploadServiceTest {

    @Mock
    private UploadTaskMapper uploadTaskMapper;
    @Mock
    private StorageService storageService;
    @Mock
    private CloudFileMapper cloudFileMapper;
    @Mock
    private CloudFileService cloudFileService;
    @Mock
    private CosCredentialService cosCredentialService;
    @Mock
    private StreamerMapper streamerMapper;

    private StorageProperties storageProperties;
    private ChunkedUploadService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("com.dianjinshou.modules.storage.mapper.UploadTaskMapper");
        TableInfoHelper.initTableInfo(assistant, UploadTask.class);
    }

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setBucketFiles("files");
        service = new ChunkedUploadService(uploadTaskMapper, cloudFileMapper, storageService, storageProperties, cloudFileService, cosCredentialService, streamerMapper);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void initUpload_success() {
        when(uploadTaskMapper.insert(any(UploadTask.class))).thenAnswer(inv -> {
            UploadTask task = inv.getArgument(0);
            task.setId(1L);
            return 1;
        });

        InitUploadRequest req = new InitUploadRequest();
        req.setFileName("test.mp4");
        req.setFileSize(15L * 1024 * 1024); // 15MB → 3 parts
        req.setContentType("video/mp4");

        UploadInitVO result = service.initUpload(req);

        assertNotNull(result);
        assertEquals(1L, result.getUploadId());
        assertEquals(3, result.getTotalParts());
        assertEquals(5 * 1024 * 1024, result.getPartSize());
        verify(uploadTaskMapper).insert(any(UploadTask.class));
    }

    @Test
    void initUpload_fileTooLarge_rejected() {
        InitUploadRequest req = new InitUploadRequest();
        req.setFileName("big.mp4");
        req.setFileSize(3L * 1024 * 1024 * 1024); // 3GB
        req.setContentType("video/mp4");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.initUpload(req));
        assertTrue(ex.getMessage().contains("2GB"));
    }

    @Test
    void initUpload_invalidContentType_rejected() {
        InitUploadRequest req = new InitUploadRequest();
        req.setFileName("test.exe");
        req.setFileSize(1024L);
        req.setContentType("application/x-msdownload");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.initUpload(req));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void uploadPart_success() {
        UploadTask task = buildTask(1L, "init", 3, 0);
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);
        when(storageService.upload(anyString(), anyString(), any(), anyLong(), anyString())).thenReturn("key");
        when(uploadTaskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ByteArrayInputStream is = new ByteArrayInputStream("data".getBytes());
        service.uploadPart(1L, 1, is, 4);

        verify(storageService).upload(eq("files"), anyString(), any(), eq(4L), eq("video/mp4"));
        verify(uploadTaskMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void uploadPart_invalidPartNumber_rejected() {
        UploadTask task = buildTask(1L, "init", 3, 0);
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);

        ByteArrayInputStream is = new ByteArrayInputStream("data".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.uploadPart(1L, 5, is, 4));
        assertTrue(ex.getMessage().contains("分片编号无效"));
    }

    @Test
    void completeUpload_success() {
        UploadTask task = buildTask(1L, "uploading", 3, 3);
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);
        when(uploadTaskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        String result = service.completeUpload(1L);

        assertNotNull(result);
        assertEquals(task.getStorageKey(), result);
    }

    @Test
    void completeUpload_incompletePartsRejected() {
        UploadTask task = buildTask(1L, "uploading", 3, 2);
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.completeUpload(1L));
        assertTrue(ex.getMessage().contains("分片未全部上传"));
    }

    @Test
    void cancelUpload_success() {
        UploadTask task = buildTask(1L, "uploading", 3, 2);
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);
        when(uploadTaskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.cancelUpload(1L);

        verify(storageService, times(3)).delete(eq("files"), anyString());
        verify(uploadTaskMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void getAndValidateTask_expiredTask_rejected() {
        UploadTask task = buildTask(1L, "uploading", 3, 1);
        task.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);

        ByteArrayInputStream is = new ByteArrayInputStream("data".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.uploadPart(1L, 2, is, 4));
        assertTrue(ex.getMessage().contains("已过期"));
    }

    @Test
    void getAndValidateTask_completedTask_rejected() {
        UploadTask task = buildTask(1L, "completed", 3, 3);
        when(uploadTaskMapper.selectById(1L)).thenReturn(task);

        ByteArrayInputStream is = new ByteArrayInputStream("data".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.uploadPart(1L, 1, is, 4));
        assertTrue(ex.getMessage().contains("已完成"));
    }

    private UploadTask buildTask(Long id, String status, int totalParts, int uploadedParts) {
        UploadTask task = new UploadTask();
        task.setId(id);
        task.setUserId(1L);
        task.setOrgId(100L);
        task.setFileName("test.mp4");
        task.setFileSize(15L * 1024 * 1024);
        task.setContentType("video/mp4");
        task.setBucket("files");
        task.setStorageKey("org100/user1/12345_abcdef/test.mp4");
        task.setTotalParts(totalParts);
        task.setUploadedParts(uploadedParts);
        task.setStatus(status);
        task.setExpiresAt(LocalDateTime.now().plusHours(24));
        return task;
    }
}
