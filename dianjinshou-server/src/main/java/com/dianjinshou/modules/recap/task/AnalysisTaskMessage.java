package com.dianjinshou.modules.recap.task;

import java.io.Serializable;

public class AnalysisTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long recordingId;
    private Long fileAnalysisTaskId;
    private String type;
    private Integer priority;

    public AnalysisTaskMessage() {
    }

    public AnalysisTaskMessage(Long taskId, Long recordingId, String type, Integer priority) {
        this.taskId = taskId;
        this.recordingId = recordingId;
        this.type = type;
        this.priority = priority;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getRecordingId() {
        return recordingId;
    }

    public void setRecordingId(Long recordingId) {
        this.recordingId = recordingId;
    }

    public Long getFileAnalysisTaskId() {
        return fileAnalysisTaskId;
    }

    public void setFileAnalysisTaskId(Long fileAnalysisTaskId) {
        this.fileAnalysisTaskId = fileAnalysisTaskId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
