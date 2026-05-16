package com.dianjinshou.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sensitive_word_library")
public class SensitiveWordLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private String category;
    private Integer riskLevel;
    private String replacementSuggestion;
    private String platform;
    private String industry;
    private String source;
    private Integer isActive;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }

    public String getReplacementSuggestion() { return replacementSuggestion; }
    public void setReplacementSuggestion(String replacementSuggestion) { this.replacementSuggestion = replacementSuggestion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
