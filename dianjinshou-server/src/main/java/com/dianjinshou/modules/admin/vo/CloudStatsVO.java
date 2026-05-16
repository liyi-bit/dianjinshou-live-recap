package com.dianjinshou.modules.admin.vo;

public class CloudStatsVO {

    private long totalFiles;
    private long totalSize;
    private long recordingCount;
    private long clipCount;
    private long documentCount;
    private long activeShareCount;

    public long getTotalFiles() { return totalFiles; }
    public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

    public long getRecordingCount() { return recordingCount; }
    public void setRecordingCount(long recordingCount) { this.recordingCount = recordingCount; }

    public long getClipCount() { return clipCount; }
    public void setClipCount(long clipCount) { this.clipCount = clipCount; }

    public long getDocumentCount() { return documentCount; }
    public void setDocumentCount(long documentCount) { this.documentCount = documentCount; }

    public long getActiveShareCount() { return activeShareCount; }
    public void setActiveShareCount(long activeShareCount) { this.activeShareCount = activeShareCount; }
}
