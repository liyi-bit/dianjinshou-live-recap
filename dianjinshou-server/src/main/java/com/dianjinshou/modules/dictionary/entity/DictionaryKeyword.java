package com.dianjinshou.modules.dictionary.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("dictionary_keywords")
public class DictionaryKeyword {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dictionaryId;
    private String category;
    private String subCategory;
    private String keyword;
    private String description;
    private String replacementSuggestion;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDictionaryId() { return dictionaryId; }
    public void setDictionaryId(Long dictionaryId) { this.dictionaryId = dictionaryId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReplacementSuggestion() { return replacementSuggestion; }
    public void setReplacementSuggestion(String replacementSuggestion) { this.replacementSuggestion = replacementSuggestion; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
