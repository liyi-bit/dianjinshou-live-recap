package com.dianjinshou.modules.recap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("keywords")
public class Keyword {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long comparisonId;
    private String type;
    private String category;
    private String subCategory;
    private String word;
    private Integer hitCountVideo1;
    private Integer hitCountVideo2;
    private Integer totalCount;
    private String source;
    private String sceneDesc;
    private String industry;
    private Integer riskLevel;
    private String sentenceRefs;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getComparisonId() {
        return comparisonId;
    }

    public void setComparisonId(Long comparisonId) {
        this.comparisonId = comparisonId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Integer getHitCountVideo1() {
        return hitCountVideo1;
    }

    public void setHitCountVideo1(Integer hitCountVideo1) {
        this.hitCountVideo1 = hitCountVideo1;
    }

    public Integer getHitCountVideo2() {
        return hitCountVideo2;
    }

    public void setHitCountVideo2(Integer hitCountVideo2) {
        this.hitCountVideo2 = hitCountVideo2;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSceneDesc() {
        return sceneDesc;
    }

    public void setSceneDesc(String sceneDesc) {
        this.sceneDesc = sceneDesc;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Integer riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSentenceRefs() {
        return sentenceRefs;
    }

    public void setSentenceRefs(String sentenceRefs) {
        this.sentenceRefs = sentenceRefs;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
