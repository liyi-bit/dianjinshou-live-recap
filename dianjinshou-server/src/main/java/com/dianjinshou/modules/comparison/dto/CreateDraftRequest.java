package com.dianjinshou.modules.comparison.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateDraftRequest {

    @NotNull(message = "录制ID不能为空")
    private Long firstRecordingId;

    private Long firstTaskId;

    @NotBlank(message = "列表上下文不能为空")
    private String listContext;

    public Long getFirstTaskId() {
        return firstTaskId;
    }

    public void setFirstTaskId(Long firstTaskId) {
        this.firstTaskId = firstTaskId;
    }

    public Long getFirstRecordingId() {
        return firstRecordingId;
    }

    public void setFirstRecordingId(Long firstRecordingId) {
        this.firstRecordingId = firstRecordingId;
    }

    public String getListContext() {
        return listContext;
    }

    public void setListContext(String listContext) {
        this.listContext = listContext;
    }
}
