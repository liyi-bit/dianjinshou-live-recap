package com.dianjinshou.common.storage;

import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final StorageProperties properties;
    private final ThirdPartySettings settings;

    private volatile MinioClient cachedClient;
    private volatile String cachedSignature;

    public MinioStorageService(StorageProperties properties, ThirdPartySettings settings) {
        this.properties = properties;
        this.settings = settings;
    }

    /** Build a MinioClient on demand. Re-uses the cached instance unless endpoint or credentials change. */
    private MinioClient minioClient() {
        String endpoint = settings.getStorageEndpoint();
        String accessKey = settings.getStorageAccessKey();
        String secretKey = settings.getStorageSecretKey();
        String sig = endpoint + "|" + accessKey + "|" + secretKey;
        MinioClient c = cachedClient;
        if (c != null && sig.equals(cachedSignature)) return c;
        synchronized (this) {
            if (cachedClient == null || !sig.equals(cachedSignature)) {
                cachedClient = MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
                cachedSignature = sig;
                log.info("MinioClient (re)built: endpoint={}", endpoint);
            }
            return cachedClient;
        }
    }

    @Override
    public String upload(String bucket, String key, InputStream inputStream, String contentType) {
        try {
            ensureBucketExists(bucket);
            minioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(inputStream, -1, 10485760) // 10MB part size
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded object: bucket={}, key={}", bucket, key);
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to upload object: " + key, e);
        }
    }

    @Override
    public String upload(String bucket, String key, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucketExists(bucket);
            minioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded object: bucket={}, key={}, size={}", bucket, key, size);
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to upload object: " + key, e);
        }
    }

    @Override
    public InputStream download(String bucket, String key) {
        try {
            return minioClient().getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new StorageException("Object not found: " + key, e);
            }
            throw new StorageException("Failed to download object: " + key, e);
        } catch (Exception e) {
            throw new StorageException("Failed to download object: " + key, e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            minioClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            log.info("Deleted object: bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            throw new StorageException("Failed to delete object: " + key, e);
        }
    }

    @Override
    public String getPresignedUrl(String bucket, String key, int expireSeconds) {
        try {
            return minioClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned URL for: " + key, e);
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            minioClient().statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new StorageException("Failed to check object existence: " + key, e);
        } catch (Exception e) {
            throw new StorageException("Failed to check object existence: " + key, e);
        }
    }

    @Override
    public void ensureBucketExists(String bucket) {
        try {
            boolean found = minioClient().bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!found) {
                minioClient().makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to ensure bucket exists: " + bucket, e);
        }
    }

    @Override
    public void composeObject(String bucket, String targetKey, java.util.List<String> sourceKeys) {
        if (sourceKeys == null || sourceKeys.isEmpty()) {
            throw new StorageException("composeObject: sourceKeys 不能为空", null);
        }
        java.util.List<ComposeSource> sources = new java.util.ArrayList<>();
        for (String src : sourceKeys) {
            sources.add(ComposeSource.builder().bucket(bucket).object(src).build());
        }
        try {
            minioClient().composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket)
                            .object(targetKey)
                            .sources(sources)
                            .build()
            );
            log.info("Composed {} parts into {}", sourceKeys.size(), targetKey);
        } catch (Exception e) {
            throw new StorageException("Failed to compose object: " + targetKey, e);
        }
    }
}
