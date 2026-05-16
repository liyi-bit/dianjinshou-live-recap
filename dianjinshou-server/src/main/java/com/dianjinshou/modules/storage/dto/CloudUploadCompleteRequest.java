package com.dianjinshou.modules.storage.dto;

public class CloudUploadCompleteRequest {

    private String checksum;
    private Long fileSize;

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
