package com.dianjinshou.modules.fileanalysis.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CopywritingReviewRequest {

    @NotBlank(message = "文案内容不能为空")
    @Size(max = 10000, message = "文案内容不能超过10000字")
    private String textContent;

    private Long industryId;
    private boolean checkSensitive = true;
    private boolean checkCompliance = true;

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public Long getIndustryId() { return industryId; }
    public void setIndustryId(Long industryId) { this.industryId = industryId; }

    public boolean isCheckSensitive() { return checkSensitive; }
    public void setCheckSensitive(boolean checkSensitive) { this.checkSensitive = checkSensitive; }

    public boolean isCheckCompliance() { return checkCompliance; }
    public void setCheckCompliance(boolean checkCompliance) { this.checkCompliance = checkCompliance; }
}
