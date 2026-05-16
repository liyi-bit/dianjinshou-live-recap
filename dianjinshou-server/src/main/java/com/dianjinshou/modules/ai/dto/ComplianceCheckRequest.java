package com.dianjinshou.modules.ai.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ComplianceCheckRequest {

    @NotBlank(message = "场景不能为空")
    private String scenario;

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本不能超过10000字")
    private String textContent;

    private String platform;
    private String industry;

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}
