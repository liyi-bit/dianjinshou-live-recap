package com.dianjinshou.modules.admin.vo;

public class AdminRecordingStatsVO {

    private long total;
    private long todayCount;
    private long weekCount;
    private long completed;
    private long failed;
    private long recording;
    private long analyzedDone;
    private long analyzedPending;
    private long totalDurationSec;
    private long totalFileBytes;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getTodayCount() { return todayCount; }
    public void setTodayCount(long todayCount) { this.todayCount = todayCount; }
    public long getWeekCount() { return weekCount; }
    public void setWeekCount(long weekCount) { this.weekCount = weekCount; }
    public long getCompleted() { return completed; }
    public void setCompleted(long completed) { this.completed = completed; }
    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }
    public long getRecording() { return recording; }
    public void setRecording(long recording) { this.recording = recording; }
    public long getAnalyzedDone() { return analyzedDone; }
    public void setAnalyzedDone(long analyzedDone) { this.analyzedDone = analyzedDone; }
    public long getAnalyzedPending() { return analyzedPending; }
    public void setAnalyzedPending(long analyzedPending) { this.analyzedPending = analyzedPending; }
    public long getTotalDurationSec() { return totalDurationSec; }
    public void setTotalDurationSec(long totalDurationSec) { this.totalDurationSec = totalDurationSec; }
    public long getTotalFileBytes() { return totalFileBytes; }
    public void setTotalFileBytes(long totalFileBytes) { this.totalFileBytes = totalFileBytes; }
}
