package com.dianjinshou.modules.comparison.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.comparison.dto.CreateDraftRequest;
import com.dianjinshou.modules.comparison.dto.SelectSecondRequest;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.entity.ComparisonDraft;
import com.dianjinshou.modules.comparison.mapper.ComparisonDraftMapper;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.vo.ComparisonDraftVO;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ComparisonDraftService {

    private final ComparisonDraftMapper draftMapper;
    private final ComparisonMapper comparisonMapper;
    private final RecordingMapper recordingMapper;
    private final AnalysisTaskMapper analysisTaskMapper;

    public ComparisonDraftService(ComparisonDraftMapper draftMapper,
                                  ComparisonMapper comparisonMapper,
                                  RecordingMapper recordingMapper,
                                  AnalysisTaskMapper analysisTaskMapper) {
        this.draftMapper = draftMapper;
        this.comparisonMapper = comparisonMapper;
        this.recordingMapper = recordingMapper;
        this.analysisTaskMapper = analysisTaskMapper;
    }

    @Transactional
    public ComparisonDraftVO createDraft(CreateDraftRequest request) {
        Long userId = SecurityContextHelper.currentUserId();

        // Validate recording exists and belongs to user's org
        Recording recording = recordingMapper.selectById(request.getFirstRecordingId());
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkRecordingOrgAccess(recording);

        // Delete any existing draft for this user (unique constraint)
        draftMapper.delete(new LambdaQueryWrapper<ComparisonDraft>()
                .eq(ComparisonDraft::getUserId, userId));

        ComparisonDraft draft = new ComparisonDraft();
        draft.setUserId(userId);
        draft.setFirstRecordingId(request.getFirstRecordingId());
        draft.setFirstTaskId(request.getFirstTaskId());
        draft.setListContext(request.getListContext());
        draft.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        draftMapper.insert(draft);

        return ComparisonDraftVO.fromEntity(draft);
    }

    public ComparisonDraftVO getCurrent() {
        Long userId = SecurityContextHelper.currentUserId();
        ComparisonDraft draft = getActiveDraft(userId);
        if (draft == null) {
            return null;
        }
        return ComparisonDraftVO.fromEntity(draft);
    }

    @Transactional
    public ComparisonVO selectSecondAndConfirm(Long draftId, SelectSecondRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();

        ComparisonDraft draft = draftMapper.selectById(draftId);
        if (draft == null || !draft.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "draft不存在");
        }

        // Check expiration
        if (draft.getExpiresAt().isBefore(LocalDateTime.now())) {
            draftMapper.deleteById(draftId);
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "draft已过期，请重新操作");
        }

        // Cross-list check
        if (!draft.getListContext().equals(request.getListContext())) {
            throw new BusinessException(ErrorCode.CROSS_LIST_COMPARISON, "不能跨列表加入对比");
        }

        // Validate second recording
        Recording secondRecording = recordingMapper.selectById(request.getSecondRecordingId());
        if (secondRecording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "第二方录制记录不存在");
        }
        checkRecordingOrgAccess(secondRecording);

        Recording firstRecording = recordingMapper.selectById(draft.getFirstRecordingId());
        if (firstRecording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "第一方录制记录不存在");
        }

        // Cross-org check between two recordings
        if (firstRecording.getOrgId() != null && secondRecording.getOrgId() != null
                && !firstRecording.getOrgId().equals(secondRecording.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "不能跨组织对比");
        }

        // Auto-determine optimize vs reference based on coreData
        Long optimizeId = firstRecording.getId();
        Long referenceId = secondRecording.getId();
        if (isHigherPerformance(secondRecording, firstRecording)) {
            referenceId = secondRecording.getId();
            optimizeId = firstRecording.getId();
        } else {
            referenceId = firstRecording.getId();
            optimizeId = secondRecording.getId();
        }

        // Determine type from listContext
        String type = "AI_FULL_RECAP".equals(draft.getListContext()) ? "full" : "clip";

        // For clip comparisons, use the explicit taskIds from the frontend
        // (user selected specific clips, not just recordings)
        Long firstTaskId = draft.getFirstTaskId();
        Long secondTaskId = request.getSecondTaskId();

        // Map taskIds to optimize/reference based on recording swap
        Long taskIdOpt;
        Long taskIdRef;
        if (firstTaskId != null && secondTaskId != null) {
            // Use explicit taskIds - map them correctly based on which recording became optimize/reference
            if (optimizeId.equals(draft.getFirstRecordingId())) {
                taskIdOpt = firstTaskId;
                taskIdRef = secondTaskId;
            } else {
                taskIdOpt = secondTaskId;
                taskIdRef = firstTaskId;
            }
        } else {
            // Fallback: lookup latest completed analysis task for each recording
            taskIdOpt = findLatestTaskId(optimizeId, type);
            taskIdRef = findLatestTaskId(referenceId, type);
        }

        // Create comparison
        Comparison comparison = new Comparison();
        comparison.setUserId(userId);
        comparison.setOrgId(orgId);
        comparison.setType(type);
        comparison.setRecordingIdOptimize(optimizeId);
        comparison.setRecordingIdReference(referenceId);
        comparison.setTaskIdOptimize(taskIdOpt);
        comparison.setTaskIdReference(taskIdRef);
        comparison.setStatus("pending");
        comparison.setAiModel("deepseek_r1");
        comparisonMapper.insert(comparison);

        // Delete draft
        draftMapper.deleteById(draftId);

        return ComparisonVO.fromEntity(comparison);
    }

    @Transactional
    public void cancelCurrent() {
        Long userId = SecurityContextHelper.currentUserId();
        draftMapper.delete(new LambdaQueryWrapper<ComparisonDraft>()
                .eq(ComparisonDraft::getUserId, userId));
    }

    private ComparisonDraft getActiveDraft(Long userId) {
        ComparisonDraft draft = draftMapper.selectOne(
                new LambdaQueryWrapper<ComparisonDraft>()
                        .eq(ComparisonDraft::getUserId, userId));
        if (draft == null) {
            return null;
        }
        // Check expiration
        if (draft.getExpiresAt().isBefore(LocalDateTime.now())) {
            draftMapper.deleteById(draft.getId());
            return null;
        }
        return draft;
    }

    private boolean isHigherPerformance(Recording a, Recording b) {
        // Compare by coreData JSON (sales amount / viewer count)
        // For now, compare by duration as a simple heuristic
        // The one with more data is the "reference" (higher performance)
        int durationA = a.getDuration() != null ? a.getDuration() : 0;
        int durationB = b.getDuration() != null ? b.getDuration() : 0;
        return durationA > durationB;
    }

    private Long findLatestTaskId(Long recordingId, String type) {
        // v1.1.0：放宽到只要有 ASR 就能参与对比（completed / transcribed / ai_processing / failed）
        //   旧逻辑只找 completed 导致「未分析」状态的场次对比时 taskId=null，进详情看不到逐字稿。
        AnalysisTask task = analysisTaskMapper.selectOne(
                new LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getRecordingId, recordingId)
                        .eq(AnalysisTask::getType, type)
                        .in(AnalysisTask::getStatus, "completed", "transcribed", "ai_processing", "failed")
                        .orderByDesc(AnalysisTask::getCreatedAt)
                        .last("LIMIT 1"));
        return task != null ? task.getId() : null;
    }

    private void checkRecordingOrgAccess(Recording recording) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long currentOrgId = SecurityContextHelper.currentOrgId();
        if (currentOrgId == null || !currentOrgId.equals(recording.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "你没有这条录制的访问权限");
        }
    }
}
