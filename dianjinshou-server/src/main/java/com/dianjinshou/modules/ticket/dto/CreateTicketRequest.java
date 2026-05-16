package com.dianjinshou.modules.ticket.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CreateTicketRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不超过200字")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    private String contact;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}
