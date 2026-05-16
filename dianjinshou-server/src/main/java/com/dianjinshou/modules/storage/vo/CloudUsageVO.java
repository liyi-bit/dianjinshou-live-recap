package com.dianjinshou.modules.storage.vo;

public class CloudUsageVO {

    private long usedBytes;
    private long totalQuotaBytes;
    private long remainingBytes;
    private int fileCount;
    private double usagePercent;

    public long getUsedBytes() { return usedBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

    public long getTotalQuotaBytes() { return totalQuotaBytes; }
    public void setTotalQuotaBytes(long totalQuotaBytes) { this.totalQuotaBytes = totalQuotaBytes; }

    public long getRemainingBytes() { return remainingBytes; }
    public void setRemainingBytes(long remainingBytes) { this.remainingBytes = remainingBytes; }

    public int getFileCount() { return fileCount; }
    public void setFileCount(int fileCount) { this.fileCount = fileCount; }

    public double getUsagePercent() { return usagePercent; }
    public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }
}
