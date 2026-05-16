package com.dianjinshou.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String type = "minio";
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketRecordings = "recordings";
    private String bucketFiles = "files";
    private String bucketClips = "clips";
    private int presignedUrlExpireSeconds = 3600;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketRecordings() {
        return bucketRecordings;
    }

    public void setBucketRecordings(String bucketRecordings) {
        this.bucketRecordings = bucketRecordings;
    }

    public String getBucketFiles() {
        return bucketFiles;
    }

    public void setBucketFiles(String bucketFiles) {
        this.bucketFiles = bucketFiles;
    }

    public String getBucketClips() {
        return bucketClips;
    }

    public void setBucketClips(String bucketClips) {
        this.bucketClips = bucketClips;
    }

    public int getPresignedUrlExpireSeconds() {
        return presignedUrlExpireSeconds;
    }

    public void setPresignedUrlExpireSeconds(int presignedUrlExpireSeconds) {
        this.presignedUrlExpireSeconds = presignedUrlExpireSeconds;
    }
}
