package com.dianjinshou.modules.fileanalysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.fileanalysis.dto.CreateFileAnalysisRequest;
import com.dianjinshou.modules.fileanalysis.entity.FileAnalysisTask;
import com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper;
import com.dianjinshou.modules.fileanalysis.vo.FileAnalysisVO;
import com.dianjinshou.modules.recap.task.AnalysisTaskMessage;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class FileAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FileAnalysisService.class);

    private final FileAnalysisTaskMapper fileAnalysisTaskMapper;
    private final AnalysisTaskProducer analysisTaskProducer;

    public FileAnalysisService(FileAnalysisTaskMapper fileAnalysisTaskMapper,
                                AnalysisTaskProducer analysisTaskProducer) {
        this.fileAnalysisTaskMapper = fileAnalysisTaskMapper;
        this.analysisTaskProducer = analysisTaskProducer;
    }

    public FileAnalysisVO createAnalysis(CreateFileAnalysisRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        FileAnalysisTask task = new FileAnalysisTask();
        task.setUserId(userId);
        task.setOrgId(orgId);
        task.setFileName(request.getFileName());
        task.setStorageKey(request.getStorageKey());
        task.setFileSize(0L);
        task.setIndustryId(request.getIndustryId());
        task.setAiModel(request.getAiModel() != null ? request.getAiModel() : "doubao");
        task.setStatus("pending");
        fileAnalysisTaskMapper.insert(task);

        AnalysisTaskMessage message = new AnalysisTaskMessage();
        message.setFileAnalysisTaskId(task.getId());
        message.setType("FILE_ANALYSIS");
        message.setPriority(5);
        analysisTaskProducer.send(message);

        log.info("Created file analysis task: id={}, fileName={}", task.getId(), task.getFileName());
        return FileAnalysisVO.fromEntity(task);
    }

    public Page<FileAnalysisVO> listAnalyses(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<FileAnalysisTask> query = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(query, FileAnalysisTask::getOrgId);

        if (status != null && !status.isEmpty()) {
            query.eq(FileAnalysisTask::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(FileAnalysisTask::getFileName, keyword);
        }
        query.orderByDesc(FileAnalysisTask::getCreatedAt);

        Page<FileAnalysisTask> entityPage = fileAnalysisTaskMapper.selectPage(new Page<>(page, size), query);
        Page<FileAnalysisVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(new ArrayList<FileAnalysisVO>());
        for (FileAnalysisTask t : entityPage.getRecords()) {
            voPage.getRecords().add(FileAnalysisVO.fromEntity(t));
        }
        return voPage;
    }

    public FileAnalysisVO getAnalysis(Long id) {
        FileAnalysisTask task = fileAnalysisTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        validateOrgAccess(task);
        return FileAnalysisVO.fromEntity(task);
    }

    public void deleteAnalysis(Long id) {
        FileAnalysisTask task = fileAnalysisTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        validateOrgAccess(task);
        fileAnalysisTaskMapper.deleteById(id);
        log.info("File analysis task deleted: id={}", id);
    }

    private void validateOrgAccess(FileAnalysisTask task) {
        Long orgId = SecurityContextHelper.currentOrgId();
        String role = SecurityContextHelper.currentRole();
        if (!"super_admin".equals(role) && orgId != null && !orgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
