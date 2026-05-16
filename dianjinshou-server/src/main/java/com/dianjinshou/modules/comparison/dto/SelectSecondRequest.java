package com.dianjinshou.modules.comparison.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SelectSecondRequest {

    @NotNull(message = "第二方录制ID不能为空")
    private Long secondRecordingId;

    private Long secondTaskId;

    @NotBlank(message = "列表上下文不能为空")
    private String listContext;

    public Long getSecondTaskId() {
        return secondTaskId;
    }

    public void setSecondTaskId(Long secondTaskId) {
        this.secondTaskId = secondTaskId;
    }

    public Long getSecondRecordingId() {
        return secondRecordingId;
    }

    public void setSecondRecordingId(Long secondRecordingId) {
        this.secondRecordingId = secondRecordingId;
    }

    public String getListContext() {
        return listContext;
    }

    public void setListContext(String listContext) {
        this.listContext = listContext;
    }
}
