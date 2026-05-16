package com.dianjinshou.modules.storage.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.storage.CosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.region.Region;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class CosCredentialService {

    private static final DateTimeFormatter KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final CosProperties properties;

    public CosCredentialService(CosProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && hasText(properties.getSecretId())
                && hasText(properties.getSecretKey())
                && hasText(properties.getBucket())
                && hasText(properties.getRegion());
    }

    public long getQuotaBytes() {
        return properties.getQuotaBytes();
    }

    public String getBucket() {
        return properties.getBucket();
    }

    public int getReadUrlExpireHours() {
        return properties.getReadUrlExpireHours();
    }

    public int getUploadCredentialExpireSeconds() {
        return properties.getUploadCredentialExpireSeconds();
    }

    public String createSignedReadUrl(String storageKey) {
        int expireSeconds = Math.max(60, properties.getReadUrlExpireHours() * 3600);
        return createSignedUrl(storageKey, HttpMethodName.GET, expireSeconds);
    }

    public String createSignedUploadUrl(String storageKey) {
        int expireSeconds = Math.max(60, properties.getUploadCredentialExpireSeconds());
        return createSignedUrl(storageKey, HttpMethodName.PUT, expireSeconds);
    }

    public void deleteObject(String storageKey) {
        if (!isConfigured() || !hasText(storageKey)) {
            return;
        }
        COSClient client = buildClient();
        try {
            client.deleteObject(properties.getBucket(), storageKey);
        } finally {
            client.shutdown();
        }
    }

    public String buildObjectKey(Long userId, String businessType, Long businessId, String originalFileName, LocalDateTime recordedAt) {
        LocalDateTime keyTime = recordedAt != null ? recordedAt : LocalDateTime.now();
        String datePath = keyTime.format(KEY_DATE_FORMATTER);
        String safeBusinessType = sanitizePathPart(businessType, "file");
        String userPart = userId != null ? String.valueOf(userId) : "unknown";
        String idPart = businessId != null ? String.valueOf(businessId) : String.valueOf(System.currentTimeMillis());
        String extension = extractExtension(originalFileName);
        return "users/" + userPart + "/" + datePath + "/" + safeBusinessType + "/" + idPart + extension;
    }

    private String extractExtension(String fileName) {
        if (!hasText(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return sanitizeExtension(fileName.substring(dotIndex));
    }

    private String sanitizePathPart(String value, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        return value.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String sanitizeExtension(String extension) {
        return extension.trim().replaceAll("[^A-Za-z0-9.]", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String createSignedUrl(String storageKey, HttpMethodName method, int expireSeconds) {
        requireConfigured();
        COSClient client = buildClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            URL url = client.generatePresignedUrl(properties.getBucket(), storageKey, expiration, method);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    private COSClient buildClient() {
        COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        return new COSClient(credentials, clientConfig);
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.THIRD_PARTY_NOT_CONFIGURED, "腾讯云 COS 未配置");
        }
    }
}
