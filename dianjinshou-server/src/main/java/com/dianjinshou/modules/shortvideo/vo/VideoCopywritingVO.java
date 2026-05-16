package com.dianjinshou.modules.shortvideo.vo;

import com.dianjinshou.modules.shortvideo.entity.VideoCopywriting;

import java.time.LocalDateTime;

public class VideoCopywritingVO {

    private Long id;
    private String sourceType;
    private String sourceUrl;
    private String title;
    private String extractedText;
    private String polishedText;
    private Integer wordCount;
    private String tags;
    private String status;
    private String errorMsg;
    private Integer copyCount;
    private LocalDateTime createdAt;

    public static VideoCopywritingVO fromEntity(VideoCopywriting entity) {
        VideoCopywritingVO vo = new VideoCopywritingVO();
        vo.setId(entity.getId());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setTitle(entity.getTitle());
        vo.setExtractedText(entity.getExtractedText());
        vo.setPolishedText(entity.getPolishedText());
        vo.setWordCount(entity.getWordCount());
        vo.setTags(entity.getTags());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCopyCount(entity.getCopyCount());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
