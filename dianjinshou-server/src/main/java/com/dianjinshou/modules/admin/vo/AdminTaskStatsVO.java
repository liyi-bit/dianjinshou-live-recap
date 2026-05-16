package com.dianjinshou.modules.admin.vo;

import java.util.Map;

public class AdminTaskStatsVO {

    private Map<String, Long> byType;
    private Map<String, Long> byStatus;
    private long todayCount;
    private long failedCount;
    private long pendingCount;
    private long completedCount;

    public Map<String, Long> getByType() { return byType; }
    public void setByType(Map<String, Long> byType) { this.byType = byType; }
    public Map<String, Long> getByStatus() { return byStatus; }
    public void setByStatus(Map<String, Long> byStatus) { this.byStatus = byStatus; }
    public long getTodayCount() { return todayCount; }
    public void setTodayCount(long todayCount) { this.todayCount = todayCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public long getCompletedCount() { return completedCount; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
}
