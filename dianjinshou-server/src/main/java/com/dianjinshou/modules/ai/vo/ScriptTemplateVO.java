package com.dianjinshou.modules.ai.vo;

import com.dianjinshou.modules.ai.entity.ScriptTemplate;

public class ScriptTemplateVO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String icon;
    private String promptTemplate;
    private String inputFields;
    private Integer sortOrder;

    public static ScriptTemplateVO fromEntity(ScriptTemplate t) {
        ScriptTemplateVO vo = new ScriptTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setDescription(t.getDescription());
        vo.setCategory(t.getCategory());
        vo.setIcon(t.getIcon());
        vo.setPromptTemplate(t.getPromptTemplate());
        vo.setInputFields(t.getInputFields());
        vo.setSortOrder(t.getSortOrder());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getPromptTemplate() { return promptTemplate; }
    public void setPromptTemplate(String promptTemplate) { this.promptTemplate = promptTemplate; }

    public String getInputFields() { return inputFields; }
    public void setInputFields(String inputFields) { this.inputFields = inputFields; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
