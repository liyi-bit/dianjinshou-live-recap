package com.dianjinshou.modules.settings.dto;

public class BasicSettingsRequest {

    private String storagePath;
    private String resolution;
    private Integer segmentDuration;
    private Boolean autoStart;
    private Boolean autoUpdate;
    private String theme;

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Integer getSegmentDuration() { return segmentDuration; }
    public void setSegmentDuration(Integer segmentDuration) { this.segmentDuration = segmentDuration; }

    public Boolean getAutoStart() { return autoStart; }
    public void setAutoStart(Boolean autoStart) { this.autoStart = autoStart; }

    public Boolean getAutoUpdate() { return autoUpdate; }
    public void setAutoUpdate(Boolean autoUpdate) { this.autoUpdate = autoUpdate; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}
