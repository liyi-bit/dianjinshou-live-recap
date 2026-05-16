package com.dianjinshou.modules.storage.vo;

import java.util.List;

public class UploadInitVO {

    private Long uploadId;
    private String storageKey;
    private Integer totalParts;
    private Long partSize;
    private List<String> partUploadUrls;

    public Long getUploadId() { return uploadId; }
    public void setUploadId(Long uploadId) { this.uploadId = uploadId; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public Integer getTotalParts() { return totalParts; }
    public void setTotalParts(Integer totalParts) { this.totalParts = totalParts; }

    public Long getPartSize() { return partSize; }
    public void setPartSize(Long partSize) { this.partSize = partSize; }

    public List<String> getPartUploadUrls() { return partUploadUrls; }
    public void setPartUploadUrls(List<String> partUploadUrls) { this.partUploadUrls = partUploadUrls; }
}
