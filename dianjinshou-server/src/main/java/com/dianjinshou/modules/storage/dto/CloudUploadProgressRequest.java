package com.dianjinshou.modules.storage.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class CloudUploadProgressRequest {

    @Min(0)
    @Max(100)
    private Integer progress;

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
