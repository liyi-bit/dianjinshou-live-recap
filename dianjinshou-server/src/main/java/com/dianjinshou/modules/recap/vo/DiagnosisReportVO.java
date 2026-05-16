package com.dianjinshou.modules.recap.vo;

import java.util.List;

public class DiagnosisReportVO {

    private Long taskId;
    private int overallScore;
    private String overallComment;
    private List<DimensionScore> dimensions;
    private List<Integer> radarData;
    private List<String> radarLabels;
    private String status;

    public static class DimensionScore {
        private String name;
        private int score;
        private String suggestion;
        private Integer historicalAvg;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

        public Integer getHistoricalAvg() { return historicalAvg; }
        public void setHistoricalAvg(Integer historicalAvg) { this.historicalAvg = historicalAvg; }
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public String getOverallComment() { return overallComment; }
    public void setOverallComment(String overallComment) { this.overallComment = overallComment; }

    public List<DimensionScore> getDimensions() { return dimensions; }
    public void setDimensions(List<DimensionScore> dimensions) { this.dimensions = dimensions; }

    public List<Integer> getRadarData() { return radarData; }
    public void setRadarData(List<Integer> radarData) { this.radarData = radarData; }

    public List<String> getRadarLabels() { return radarLabels; }
    public void setRadarLabels(List<String> radarLabels) { this.radarLabels = radarLabels; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
