package com.dianjinshou.modules.recap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 切片占位创建请求：客户端发起切片时立即创建一条 status=transcribing 的 AnalysisTask，
 * 返回 taskId 给前端用于后续 ASR 结果回填。
 *
 * 设计目的：把"切片视频提取 + 客户端 ASR"耗时操作（数十秒到数分钟）从同步阻塞改为异步，
 * 让用户立即看到列表里多出一行"逐字稿生成中"。
 */
public class CreateClipDraftRequest {

    @NotNull(message = "录制ID不能为空")
    private Long recordingId;

    @NotNull(message = "切片开始时间不能为空")
    private Integer clipStart;

    @NotNull(message = "切片结束时间不能为空")
    private Integer clipEnd;

    @NotBlank(message = "切片分类不能为空")
    private String clipCategory;

    private String clipFilename;
    private String clipRemark;
    private String aiModel;

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
    public String getClipRemark() { return clipRemark; }
    public void setClipRemark(String clipRemark) { this.clipRemark = clipRemark; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
}
