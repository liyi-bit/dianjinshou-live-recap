package com.dianjinshou.modules.storage.vo;

import com.dianjinshou.modules.storage.entity.ShareLink;

import java.time.LocalDateTime;

public class ShareLinkVO {

    private Long id;
    private Long cloudFileId;
    private String fileName;
    private String shareCode;
    private String shareUrl;
    private boolean hasPassword;
    private LocalDateTime expiresAt;
    private Integer maxDownloads;
    private Integer downloadCount;
    private Integer viewCount;
    private String status;
    private LocalDateTime createdAt;

    public static ShareLinkVO fromEntity(ShareLink link, String fileName) {
        ShareLinkVO vo = new ShareLinkVO();
        vo.setId(link.getId());
        vo.setCloudFileId(link.getCloudFileId());
        vo.setFileName(fileName);
        vo.setShareCode(link.getShareCode());
        vo.setShareUrl("/s/" + link.getShareCode());
        vo.setHasPassword(link.getPassword() != null && !link.getPassword().isEmpty());
        vo.setExpiresAt(link.getExpiresAt());
        vo.setMaxDownloads(link.getMaxDownloads());
        vo.setDownloadCount(link.getDownloadCount());
        vo.setViewCount(link.getViewCount());
        vo.setStatus(link.getStatus());
        vo.setCreatedAt(link.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCloudFileId() { return cloudFileId; }
    public void setCloudFileId(Long cloudFileId) { this.cloudFileId = cloudFileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }

    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }

    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
