package com.dianjinshou.common.storage;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.ErrorResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private StorageProperties properties;
    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setBucketRecordings("recordings");
        properties.setBucketFiles("files");
        properties.setBucketClips("clips");
        properties.setPresignedUrlExpireSeconds(3600);
        storageService = new MinioStorageService(minioClient, properties);
    }

    @Test
    void upload_success() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        byte[] content = "test content".getBytes();
        InputStream is = new ByteArrayInputStream(content);

        String result = storageService.upload("files", "test/file.txt", is, "text/plain");

        assertEquals("test/file.txt", result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_withSize_success() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        byte[] content = "test content".getBytes();
        InputStream is = new ByteArrayInputStream(content);

        String result = storageService.upload("files", "test/file.txt", is, content.length, "text/plain");

        assertEquals("test/file.txt", result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_failure_throwsStorageException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("connection refused"));

        byte[] content = "test".getBytes();
        InputStream is = new ByteArrayInputStream(content);

        StorageException ex = assertThrows(StorageException.class,
                () -> storageService.upload("files", "key", is, "text/plain"));
        assertTrue(ex.getMessage().contains("Failed to upload"));
    }

    @Test
    void download_success() throws Exception {
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

        InputStream result = storageService.download("files", "test/file.txt");

        assertNotNull(result);
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void download_notFound_throwsStorageException() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse("NoSuchKey", "Not found", "bucket", "key", "req", "host", "res");
        ErrorResponseException notFoundEx = new ErrorResponseException(errorResponse, null, "");
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(notFoundEx);

        StorageException ex = assertThrows(StorageException.class,
                () -> storageService.download("files", "nonexistent"));
        assertTrue(ex.getMessage().contains("Object not found"));
    }

    @Test
    void delete_success() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        storageService.delete("files", "test/file.txt");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void getPresignedUrl_success() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/files/test/file.txt?token=abc123");

        String url = storageService.getPresignedUrl("files", "test/file.txt", 3600);

        assertNotNull(url);
        assertTrue(url.contains("file.txt"));
    }

    @Test
    void exists_returnsTrue_whenObjectExists() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);

        assertTrue(storageService.exists("files", "test/file.txt"));
    }

    @Test
    void exists_returnsFalse_whenObjectNotFound() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse("NoSuchKey", "Not found", "bucket", "key", "req", "host", "res");
        ErrorResponseException notFoundEx = new ErrorResponseException(errorResponse, null, "");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(notFoundEx);

        assertFalse(storageService.exists("files", "nonexistent"));
    }

    @Test
    void ensureBucketExists_createsBucket_whenNotExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        storageService.ensureBucketExists("new-bucket");

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void ensureBucketExists_skipsCreation_whenExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.ensureBucketExists("existing-bucket");

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }
}
