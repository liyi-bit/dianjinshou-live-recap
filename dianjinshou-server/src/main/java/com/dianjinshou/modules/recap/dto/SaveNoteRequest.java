package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotBlank;

public class SaveNoteRequest {

    @NotBlank(message = "标签类型不能为空")
    private String tabType;
    private String contentHtml;

    public String getTabType() {
        return tabType;
    }

    public void setTabType(String tabType) {
        this.tabType = tabType;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }
}
