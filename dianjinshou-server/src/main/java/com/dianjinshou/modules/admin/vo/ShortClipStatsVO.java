package com.dianjinshou.modules.admin.vo;

public class ShortClipStatsVO {

    private long totalClips;
    private long completedClips;
    private long failedClips;
    private long totalFileSize;

    public long getTotalClips() { return totalClips; }
    public void setTotalClips(long totalClips) { this.totalClips = totalClips; }

    public long getCompletedClips() { return completedClips; }
    public void setCompletedClips(long completedClips) { this.completedClips = completedClips; }

    public long getFailedClips() { return failedClips; }
    public void setFailedClips(long failedClips) { this.failedClips = failedClips; }

    public long getTotalFileSize() { return totalFileSize; }
    public void setTotalFileSize(long totalFileSize) { this.totalFileSize = totalFileSize; }
}
