package com.dianjinshou.modules.admin.vo;

public class AiStatsVO {

    private long totalSessions;
    private long totalMessages;
    private long operationSessions;
    private long complianceSessions;
    private long scriptSessions;
    private long totalGenerations;
    private int sensitiveWordCount;

    public long getTotalSessions() { return totalSessions; }
    public void setTotalSessions(long totalSessions) { this.totalSessions = totalSessions; }

    public long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }

    public long getOperationSessions() { return operationSessions; }
    public void setOperationSessions(long operationSessions) { this.operationSessions = operationSessions; }

    public long getComplianceSessions() { return complianceSessions; }
    public void setComplianceSessions(long complianceSessions) { this.complianceSessions = complianceSessions; }

    public long getScriptSessions() { return scriptSessions; }
    public void setScriptSessions(long scriptSessions) { this.scriptSessions = scriptSessions; }

    public long getTotalGenerations() { return totalGenerations; }
    public void setTotalGenerations(long totalGenerations) { this.totalGenerations = totalGenerations; }

    public int getSensitiveWordCount() { return sensitiveWordCount; }
    public void setSensitiveWordCount(int sensitiveWordCount) { this.sensitiveWordCount = sensitiveWordCount; }
}
