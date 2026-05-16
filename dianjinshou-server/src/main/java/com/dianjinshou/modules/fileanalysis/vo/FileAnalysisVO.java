package com.dianjinshou.modules.fileanalysis.vo;

import com.dianjinshou.modules.fileanalysis.entity.FileAnalysisTask;

import java.time.LocalDateTime;

public class FileAnalysisVO {

    private Long id;
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private String aiModel;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;

    public static FileAnalysisVO fromEntity(FileAnalysisTask entity) {
        FileAnalysisVO vo = new FileAnalysisVO();
        vo.setId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setDuration(entity.getDuration());
        vo.setAiModel(entity.getAiModel());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
