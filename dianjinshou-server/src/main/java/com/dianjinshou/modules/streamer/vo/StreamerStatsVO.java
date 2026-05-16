package com.dianjinshou.modules.streamer.vo;

public class StreamerStatsVO {

    private long total;
    private long monitoring;
    private long recording;
    private long ownCount;
    private long competitorCount;
    private long industryCount;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(long monitoring) {
        this.monitoring = monitoring;
    }

    public long getRecording() {
        return recording;
    }

    public void setRecording(long recording) {
        this.recording = recording;
    }

    public long getOwnCount() {
        return ownCount;
    }

    public void setOwnCount(long ownCount) {
        this.ownCount = ownCount;
    }

    public long getCompetitorCount() {
        return competitorCount;
    }

    public void setCompetitorCount(long competitorCount) {
        this.competitorCount = competitorCount;
    }

    public long getIndustryCount() {
        return industryCount;
    }

    public void setIndustryCount(long industryCount) {
        this.industryCount = industryCount;
    }
}
