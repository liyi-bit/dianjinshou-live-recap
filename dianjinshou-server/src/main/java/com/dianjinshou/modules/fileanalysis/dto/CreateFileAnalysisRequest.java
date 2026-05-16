package com.dianjinshou.modules.fileanalysis.dto;

import javax.validation.constraints.NotBlank;

public class CreateFileAnalysisRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "存储键不能为空")
    private String storageKey;

    private Long industryId;
    private String aiModel;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public Long getIndustryId() { return industryId; }
    public void setIndustryId(Long industryId) { this.industryId = industryId; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
}
