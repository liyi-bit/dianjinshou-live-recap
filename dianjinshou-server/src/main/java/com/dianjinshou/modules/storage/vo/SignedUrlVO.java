package com.dianjinshou.modules.storage.vo;

import java.time.LocalDateTime;

public class SignedUrlVO {

    private String url;
    private String method;
    private LocalDateTime expiresAt;

    public SignedUrlVO() {
    }

    public SignedUrlVO(String url, String method, LocalDateTime expiresAt) {
        this.url = url;
        this.method = method;
        this.expiresAt = expiresAt;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
