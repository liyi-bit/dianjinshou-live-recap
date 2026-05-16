package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotBlank;
import java.util.List;

public class SubmitClipAsrRequest {

    /** 可选：若提供，service 走"更新现有 draft 任务"模式，不再新建 task。 */
    private Long taskId;

    @NotNull(message = "录制ID不能为空")
    private Long recordingId;

    @NotNull(message = "切片开始时间不能为空")
    private Integer clipStart;

    @NotNull(message = "切片结束时间不能为空")
    private Integer clipEnd;

    @NotBlank(message = "切片分类不能为空")
    private String clipCategory;

    private String clipFilename;
    private String clipFilePath;
    private String clipRemark;
    private String aiModel;

    @NotEmpty(message = "ASR结果不能为空")
    private List<SubmitAsrResultRequest.AsrSegment> segments;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getRecordingId() { return recordingId; }
    public void setRecordingId(Long recordingId) { this.recordingId = recordingId; }
    public Integer getClipStart() { return clipStart; }
    public void setClipStart(Integer clipStart) { this.clipStart = clipStart; }
    public Integer getClipEnd() { return clipEnd; }
    public void setClipEnd(Integer clipEnd) { this.clipEnd = clipEnd; }
    public String getClipCategory() { return clipCategory; }
    public void setClipCategory(String clipCategory) { this.clipCategory = clipCategory; }
    public String getClipFilename() { return clipFilename; }
    public void setClipFilename(String clipFilename) { this.clipFilename = clipFilename; }
    public String getClipFilePath() { return clipFilePath; }
    public void setClipFilePath(String clipFilePath) { this.clipFilePath = clipFilePath; }
    public String getClipRemark() { return clipRemark; }
    public void setClipRemark(String clipRemark) { this.clipRemark = clipRemark; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public List<SubmitAsrResultRequest.AsrSegment> getSegments() { return segments; }
    public void setSegments(List<SubmitAsrResultRequest.AsrSegment> segments) { this.segments = segments; }
}
