package com.dianjinshou.modules.fileanalysis.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class CreateFileClipRequest {

    @NotNull(message = "切片起始时间不能为空")
    @Positive(message = "起始时间必须大于0")
    private Integer clipStart;

    @NotNull(message = "切片结束时间不能为空")
    @Positive(message = "结束时间必须大于0")
    private Integer clipEnd;

    @NotBlank(message = "切片类型不能为空")
    private String clipCategory;

    private String clipFilename;
    private String clipRemark;

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
}
