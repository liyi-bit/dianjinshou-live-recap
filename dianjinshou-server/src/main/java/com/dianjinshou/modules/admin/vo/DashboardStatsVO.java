package com.dianjinshou.modules.admin.vo;

public class DashboardStatsVO {

    private long totalUsers;
    private long todayNewUsers;
    private long monthNewUsers;
    private long todayActive;

    private long totalRecordings;
    private long todayRecordings;
    private long totalDuration;

    private long totalTasks;
    private long consumedChars;
    private long avgProcessTime;

    private long paidUsers;
    private long monthRevenue;
    private long pendingRenew;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTodayNewUsers() { return todayNewUsers; }
    public void setTodayNewUsers(long todayNewUsers) { this.todayNewUsers = todayNewUsers; }

    public long getMonthNewUsers() { return monthNewUsers; }
    public void setMonthNewUsers(long monthNewUsers) { this.monthNewUsers = monthNewUsers; }

    public long getTodayActive() { return todayActive; }
    public void setTodayActive(long todayActive) { this.todayActive = todayActive; }

    public long getTotalRecordings() { return totalRecordings; }
    public void setTotalRecordings(long totalRecordings) { this.totalRecordings = totalRecordings; }

    public long getTodayRecordings() { return todayRecordings; }
    public void setTodayRecordings(long todayRecordings) { this.todayRecordings = todayRecordings; }

    public long getTotalDuration() { return totalDuration; }
    public void setTotalDuration(long totalDuration) { this.totalDuration = totalDuration; }

    public long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }

    public long getConsumedChars() { return consumedChars; }
    public void setConsumedChars(long consumedChars) { this.consumedChars = consumedChars; }

    public long getAvgProcessTime() { return avgProcessTime; }
    public void setAvgProcessTime(long avgProcessTime) { this.avgProcessTime = avgProcessTime; }

    public long getPaidUsers() { return paidUsers; }
    public void setPaidUsers(long paidUsers) { this.paidUsers = paidUsers; }

    public long getMonthRevenue() { return monthRevenue; }
    public void setMonthRevenue(long monthRevenue) { this.monthRevenue = monthRevenue; }

    public long getPendingRenew() { return pendingRenew; }
    public void setPendingRenew(long pendingRenew) { this.pendingRenew = pendingRenew; }
}
