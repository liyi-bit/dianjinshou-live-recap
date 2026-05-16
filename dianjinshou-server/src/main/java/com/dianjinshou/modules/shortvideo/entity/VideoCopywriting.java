package com.dianjinshou.modules.shortvideo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dianjinshou.common.entity.BaseEntity;

@TableName("video_copywriting")
public class VideoCopywriting extends BaseEntity {

    private Long userId;
    private Long orgId;
    private String sourceType;
    private String sourceUrl;
    private String storageKey;
    private String title;
    private String extractedText;
    private String polishedText;
    private Integer wordCount;
    private String tags;
    private String status;
    private String errorMsg;
    private Integer copyCount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getPolishedText() { return polishedText; }
    public void setPolishedText(String polishedText) { this.polishedText = polishedText; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public Integer getCopyCount() { return copyCount; }
    public void setCopyCount(Integer copyCount) { this.copyCount = copyCount; }
}
