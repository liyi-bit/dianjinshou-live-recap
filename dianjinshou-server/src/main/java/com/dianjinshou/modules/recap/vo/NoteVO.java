package com.dianjinshou.modules.recap.vo;

import com.dianjinshou.modules.recap.entity.RecapNote;

public class NoteVO {

    private Long id;
    private String tabType;
    private String contentHtml;

    public static NoteVO fromEntity(RecapNote note) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setTabType(note.getTabType());
        vo.setContentHtml(note.getContentHtml());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
