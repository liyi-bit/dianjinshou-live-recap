package com.dianjinshou.modules.shortvideo.dto;

import javax.validation.constraints.NotBlank;

public class ExtractCopywritingRequest {

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    private String sourceUrl;

    private String storageKey;

    private String title;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
