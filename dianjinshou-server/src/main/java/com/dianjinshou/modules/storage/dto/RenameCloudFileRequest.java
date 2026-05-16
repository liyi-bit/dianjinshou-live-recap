package com.dianjinshou.modules.storage.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RenameCloudFileRequest {

    @NotBlank(message = "文件名不能为空")
    @Size(max = 256, message = "文件名不能超过256个字符")
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
