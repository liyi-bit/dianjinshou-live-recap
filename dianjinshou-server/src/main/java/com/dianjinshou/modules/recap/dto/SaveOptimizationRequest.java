package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SaveOptimizationRequest {

    @NotBlank(message = "优化动作不能为空")
    @Size(max = 500, message = "优化动作不能超过500字")
    private String action;

    @Size(max = 500, message = "优化目标不能超过500字")
    private String goal;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
}
