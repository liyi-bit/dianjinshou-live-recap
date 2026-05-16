package com.dianjinshou.modules.fileanalysis.vo;

import com.dianjinshou.modules.fileanalysis.entity.CopywritingReview;

import java.time.LocalDateTime;

public class CopywritingReviewVO {

    private Long id;
    private String textContent;
    private String result;
    private Integer riskScore;
    private String status;
    private LocalDateTime createdAt;

    public static CopywritingReviewVO fromEntity(CopywritingReview entity) {
        CopywritingReviewVO vo = new CopywritingReviewVO();
        vo.setId(entity.getId());
        vo.setTextContent(entity.getTextContent());
        vo.setResult(entity.getResult());
        vo.setRiskScore(entity.getRiskScore());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
