package com.dianjinshou.modules.fileanalysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.fileanalysis.dto.CreateFileClipRequest;
import com.dianjinshou.modules.fileanalysis.entity.FileAnalysisTask;
import com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper;
import com.dianjinshou.modules.fileanalysis.vo.FileAnalysisVO;
import com.dianjinshou.modules.recap.task.AnalysisTaskMessage;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FileClipService {

    private static final Logger log = LoggerFactory.getLogger(FileClipService.class);
    private static final int MIN_CLIP_DURATION = 10; // seconds

    private final FileAnalysisTaskMapper fileAnalysisTaskMapper;
    private final AnalysisTaskProducer analysisTaskProducer;

    public FileClipService(FileAnalysisTaskMapper fileAnalysisTaskMapper,
                           AnalysisTaskProducer analysisTaskProducer) {
        this.fileAnalysisTaskMapper = fileAnalysisTaskMapper;
        this.analysisTaskProducer = analysisTaskProducer;
    }

    public FileAnalysisVO createClip(Long fileAnalysisId, CreateFileClipRequest request) {
        FileAnalysisTask parent = fileAnalysisTaskMapper.selectById(fileAnalysisId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件分析任务不存在");
        }
        validateOrgAccess(parent);

        int duration = request.getClipEnd() - request.getClipStart();
        if (duration < MIN_CLIP_DURATION) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片时长不能小于10秒");
        }
        if (parent.getDuration() != null && request.getClipEnd() > parent.getDuration()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片结束时间超出视频时长");
        }

        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();

        FileAnalysisTask clipTask = new FileAnalysisTask();
        clipTask.setUserId(userId);
        clipTask.setOrgId(orgId);
        clipTask.setFileName(request.getClipFilename() != null ? request.getClipFilename() :
                parent.getFileName() + "_clip_" + request.getClipStart() + "_" + request.getClipEnd());
        clipTask.setStorageKey(parent.getStorageKey());
        clipTask.setFileSize(0L);
        clipTask.setDuration(duration);
        clipTask.setIndustryId(parent.getIndustryId());
        clipTask.setAiModel(parent.getAiModel());
        clipTask.setStatus("pending");
        fileAnalysisTaskMapper.insert(clipTask);

        AnalysisTaskMessage message = new AnalysisTaskMessage();
        message.setFileAnalysisTaskId(clipTask.getId());
        message.setType("FILE_CLIP_ANALYSIS");
        message.setPriority(5);
        analysisTaskProducer.send(message);

        log.info("Created file clip analysis: id={}, parent={}, range={}-{}s",
                clipTask.getId(), fileAnalysisId, request.getClipStart(), request.getClipEnd());
        return FileAnalysisVO.fromEntity(clipTask);
    }

    public List<FileAnalysisVO> listClips(Long fileAnalysisId) {
        FileAnalysisTask parent = fileAnalysisTaskMapper.selectById(fileAnalysisId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件分析任务不存在");
        }
        validateOrgAccess(parent);

        LambdaQueryWrapper<FileAnalysisTask> query = new LambdaQueryWrapper<>();
        query.eq(FileAnalysisTask::getStorageKey, parent.getStorageKey())
                .ne(FileAnalysisTask::getId, fileAnalysisId)
                .orderByDesc(FileAnalysisTask::getCreatedAt);
        List<FileAnalysisTask> clips = fileAnalysisTaskMapper.selectList(query);

        List<FileAnalysisVO> result = new ArrayList<FileAnalysisVO>();
        for (FileAnalysisTask clip : clips) {
            result.add(FileAnalysisVO.fromEntity(clip));
        }
        return result;
    }

    private void validateOrgAccess(FileAnalysisTask task) {
        Long orgId = SecurityContextHelper.currentOrgId();
        String role = SecurityContextHelper.currentRole();
        if (!"super_admin".equals(role) && orgId != null && !orgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
