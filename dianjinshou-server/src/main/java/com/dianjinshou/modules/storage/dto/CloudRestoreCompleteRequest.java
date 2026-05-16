package com.dianjinshou.modules.storage.dto;

import javax.validation.constraints.NotBlank;

public class CloudRestoreCompleteRequest {

    @NotBlank(message = "本地文件路径不能为空")
    private String localFilePath;

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }
}
