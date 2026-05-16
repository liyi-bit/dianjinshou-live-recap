package com.dianjinshou.modules.recording.dto;

import javax.validation.constraints.NotBlank;

public class RenameRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
