package com.dianjinshou.modules.ai.vo;

public class PresetQuestionVO {

    private int id;
    private String title;
    private String desc;
    private String color;

    public PresetQuestionVO(int id, String title, String desc, String color) {
        this.id = id;
        this.title = title;
        this.desc = desc;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
