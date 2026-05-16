package com.dianjinshou.modules.shortvideo.dto;

import javax.validation.constraints.NotNull;

public class SubscribeCreatorRequest {

    @NotNull(message = "达人ID不能为空")
    private Long creatorId;

    private Boolean notifyOnNewVideo;

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public Boolean getNotifyOnNewVideo() { return notifyOnNewVideo; }
    public void setNotifyOnNewVideo(Boolean notifyOnNewVideo) { this.notifyOnNewVideo = notifyOnNewVideo; }
}
