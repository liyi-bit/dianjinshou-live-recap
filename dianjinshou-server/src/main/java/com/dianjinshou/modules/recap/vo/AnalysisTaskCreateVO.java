package com.dianjinshou.modules.recap.vo;

public class AnalysisTaskCreateVO {

    private Long taskId;
    private String status;

    public static AnalysisTaskCreateVO of(Long taskId, String status) {
        AnalysisTaskCreateVO vo = new AnalysisTaskCreateVO();
        vo.setTaskId(taskId);
        vo.setStatus(status);
        return vo;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
