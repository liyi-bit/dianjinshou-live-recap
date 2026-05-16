package com.dianjinshou.modules.recap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("asr_paragraphs")
public class AsrParagraph {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer paragraphIndex;
    private String startTime;
    private String endTime;
    private String naturalTime;
    private String textContent;
    private Integer wordCount;
    private Integer wordsPerMin;
    private Integer onlineCount;
    private Integer barrageCount;
    private Integer transactionCount;
    private BigDecimal interactionRate;
    private BigDecimal transactionRate;
    private BigDecimal salesAmount;
    private BigDecimal uvValue;
    private String speakerId;
    private String scriptCategory;
    private Integer isHighlighted;
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

    public Integer getParagraphIndex() {
        return paragraphIndex;
    }

    public void setParagraphIndex(Integer paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getNaturalTime() {
        return naturalTime;
    }

    public void setNaturalTime(String naturalTime) {
        this.naturalTime = naturalTime;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getWordsPerMin() {
        return wordsPerMin;
    }

    public void setWordsPerMin(Integer wordsPerMin) {
        this.wordsPerMin = wordsPerMin;
    }

    public Integer getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(Integer onlineCount) {
        this.onlineCount = onlineCount;
    }

    public Integer getBarrageCount() {
        return barrageCount;
    }

    public void setBarrageCount(Integer barrageCount) {
        this.barrageCount = barrageCount;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getInteractionRate() {
        return interactionRate;
    }

    public void setInteractionRate(BigDecimal interactionRate) {
        this.interactionRate = interactionRate;
    }

    public BigDecimal getTransactionRate() {
        return transactionRate;
    }

    public void setTransactionRate(BigDecimal transactionRate) {
        this.transactionRate = transactionRate;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public BigDecimal getUvValue() {
        return uvValue;
    }

    public void setUvValue(BigDecimal uvValue) {
        this.uvValue = uvValue;
    }

    public String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }

    public String getScriptCategory() {
        return scriptCategory;
    }

    public void setScriptCategory(String scriptCategory) {
        this.scriptCategory = scriptCategory;
    }

    public Integer getIsHighlighted() {
        return isHighlighted;
    }

    public void setIsHighlighted(Integer isHighlighted) {
        this.isHighlighted = isHighlighted;
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
