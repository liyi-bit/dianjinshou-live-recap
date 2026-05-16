package com.dianjinshou.modules.streamer.vo;

import com.dianjinshou.modules.streamer.entity.Industry;

import java.util.ArrayList;
import java.util.List;

public class IndustryTreeVO {

    private Long id;
    private String name;
    private String code;
    private List<IndustryTreeVO> children;

    public static IndustryTreeVO fromEntity(Industry industry) {
        IndustryTreeVO vo = new IndustryTreeVO();
        vo.setId(industry.getId());
        vo.setName(industry.getName());
        vo.setCode(industry.getCode());
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<IndustryTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<IndustryTreeVO> children) {
        this.children = children;
    }
}
