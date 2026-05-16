package com.dianjinshou.modules.ai.vo;

import java.util.List;

public class ComplianceCheckResultVO {

    private List<HitWord> hitWords;
    private String aiAnalysis;
    private Integer riskScore;
    private String riskLevel;
    private List<String> suggestions;

    public List<HitWord> getHitWords() { return hitWords; }
    public void setHitWords(List<HitWord> hitWords) { this.hitWords = hitWords; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public static class HitWord {
        private String word;
        private int position;
        private String category;
        private int riskLevel;
        private String replacement;

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getRiskLevel() { return riskLevel; }
        public void setRiskLevel(int riskLevel) { this.riskLevel = riskLevel; }

        public String getReplacement() { return replacement; }
        public void setReplacement(String replacement) { this.replacement = replacement; }
    }
}
