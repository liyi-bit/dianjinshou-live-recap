package com.dianjinshou.modules.fileanalysis.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dianjinshou.common.entity.BaseEntity;

@TableName("file_analysis_tasks")
public class FileAnalysisTask extends BaseEntity {

    private Long userId;
    private Long orgId;
    private String fileName;
    private String storageKey;
    private Long fileSize;
    private Integer duration;
    private Long industryId;
    private String aiModel;
    private String asrText;
    private String status;
    private String errorMsg;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Long getIndustryId() { return industryId; }
    public void setIndustryId(Long industryId) { this.industryId = industryId; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public String getAsrText() { return asrText; }
    public void setAsrText(String asrText) { this.asrText = asrText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}
