package com.dianjinshou.modules.recap.vo;

import com.dianjinshou.modules.recap.entity.Keyword;

public class KeywordVO {

    private Long id;
    private String type;
    private String category;
    private String subCategory;
    private String word;
    private Integer hitCountVideo1;
    private Integer hitCountVideo2;
    private Integer totalCount;
    private String source;
    private String sceneDesc;
    private Integer riskLevel;
    private String sentenceRefs;

    public static KeywordVO fromEntity(Keyword k) {
        KeywordVO vo = new KeywordVO();
        vo.setId(k.getId());
        vo.setType(k.getType());
        vo.setCategory(k.getCategory());
        vo.setSubCategory(k.getSubCategory());
        vo.setWord(k.getWord());
        vo.setHitCountVideo1(k.getHitCountVideo1());
        vo.setHitCountVideo2(k.getHitCountVideo2());
        vo.setTotalCount(k.getTotalCount());
        vo.setSource(k.getSource());
        vo.setSceneDesc(k.getSceneDesc());
        vo.setRiskLevel(k.getRiskLevel());
        vo.setSentenceRefs(k.getSentenceRefs());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
