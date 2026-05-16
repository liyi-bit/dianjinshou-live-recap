package com.dianjinshou.modules.admin.vo;

import com.dianjinshou.modules.recording.entity.Recording;

import java.time.LocalDateTime;

public class AdminRecordingVO {

    private Long id;
    private Long userId;
    private String username;
    private String userPhone;
    private Long streamerId;
    private String streamerName;
    private String streamerAvatar;
    private Long orgId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Long fileSize;
    private String status;
    private String analysisStatus;
    private String errorMsg;
    private LocalDateTime createdAt;

    public static AdminRecordingVO fromEntity(Recording r, String username) {
        AdminRecordingVO vo = new AdminRecordingVO();
        vo.id = r.getId();
        vo.userId = r.getUserId();
        vo.username = username;
        vo.streamerId = r.getStreamerId();
        vo.orgId = r.getOrgId();
        return vo;
    }

    public static AdminRecordingVO fromEntity(Recording r, String username, String userPhone,
                                              String streamerName, String streamerAvatar) {
        AdminRecordingVO vo = new AdminRecordingVO();
        vo.id = r.getId();
        vo.userId = r.getUserId();
        vo.username = username;
        vo.userPhone = userPhone;
        vo.streamerId = r.getStreamerId();
        vo.streamerName = streamerName;
        vo.streamerAvatar = streamerAvatar;
        vo.orgId = r.getOrgId();
        vo.startTime = r.getStartTime();
        vo.endTime = r.getEndTime();
        vo.duration = r.getDuration();
        vo.fileSize = r.getFileSize();
        vo.status = r.getStatus();
        vo.analysisStatus = r.getAnalysisStatus();
        vo.errorMsg = r.getErrorMsg();
        vo.createdAt = r.getCreatedAt();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public Long getStreamerId() { return streamerId; }
    public void setStreamerId(Long streamerId) { this.streamerId = streamerId; }
    public String getStreamerName() { return streamerName; }
    public void setStreamerName(String streamerName) { this.streamerName = streamerName; }
    public String getStreamerAvatar() { return streamerAvatar; }
    public void setStreamerAvatar(String streamerAvatar) { this.streamerAvatar = streamerAvatar; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
