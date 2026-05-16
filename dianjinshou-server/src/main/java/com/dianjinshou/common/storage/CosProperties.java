package com.dianjinshou.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tencent.cos")
public class CosProperties {

    private boolean enabled = false;
    private String secretId = "";
    private String secretKey = "";
    private String bucket = "";
    private String region = "ap-beijing";
    private String endpoint = "";
    private long quotaBytes = 20L * 1024 * 1024 * 1024;
    private int readUrlExpireHours = 24;
    private int uploadCredentialExpireSeconds = 1800;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public void setQuotaBytes(long quotaBytes) {
        this.quotaBytes = quotaBytes;
    }

    public int getReadUrlExpireHours() {
        return readUrlExpireHours;
    }

    public void setReadUrlExpireHours(int readUrlExpireHours) {
        this.readUrlExpireHours = readUrlExpireHours;
    }

    public int getUploadCredentialExpireSeconds() {
        return uploadCredentialExpireSeconds;
    }

    public void setUploadCredentialExpireSeconds(int uploadCredentialExpireSeconds) {
        this.uploadCredentialExpireSeconds = uploadCredentialExpireSeconds;
    }
}
