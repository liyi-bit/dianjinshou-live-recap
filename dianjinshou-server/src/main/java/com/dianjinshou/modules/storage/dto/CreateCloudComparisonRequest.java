package com.dianjinshou.modules.storage.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class CreateCloudComparisonRequest {

    @NotBlank(message = "对比模式不能为空")
    private String mode;

    @NotEmpty(message = "请选择两条数据")
    private List<Long> fileIds;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }
}
