package com.dianjinshou.modules.recap.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.enums.AnalysisStatus;
import com.dianjinshou.common.enums.ClipCategory;
import com.dianjinshou.common.enums.RecapType;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.DataPermission;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.integration.ai.AiAnalysisClient;
import com.dianjinshou.modules.admin.service.DailyAiQuotaService;
import com.dianjinshou.modules.recap.dto.CreateClipAnalysisRequest;
import com.dianjinshou.modules.recap.dto.CreateClipDraftRequest;
import com.dianjinshou.modules.recap.dto.CreateFullAnalysisRequest;
import com.dianjinshou.modules.recap.dto.SaveNoteRequest;
import com.dianjinshou.modules.recap.dto.SubmitAsrResultRequest;
import com.dianjinshou.modules.recap.dto.SubmitClipAsrRequest;
import com.dianjinshou.modules.recap.dto.SaveOptimizationRequest;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.AsrParagraph;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.entity.OptimizationAction;
import com.dianjinshou.modules.recap.entity.RecapNote;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.AsrParagraphMapper;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import com.dianjinshou.modules.recap.mapper.OptimizationActionMapper;
import com.dianjinshou.modules.recap.mapper.RecapNoteMapper;
import com.dianjinshou.modules.recap.vo.AnalysisTaskCreateVO;
import com.dianjinshou.modules.recap.vo.AnalysisTaskVO;
import com.dianjinshou.modules.recap.vo.AsrParagraphVO;
import com.dianjinshou.modules.recap.vo.KeywordListVO;
import com.dianjinshou.modules.recap.vo.KeywordVO;
import com.dianjinshou.modules.recap.vo.NoteVO;
import com.dianjinshou.modules.recap.task.AnalysisTaskMessage;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisTaskMapper analysisTaskMapper;
    private final AsrParagraphMapper asrParagraphMapper;
    private final KeywordMapper keywordMapper;
    private final OptimizationActionMapper optimizationActionMapper;
    private final RecapNoteMapper recapNoteMapper;
    private final RecordingMapper recordingMapper;
    private final AnalysisTaskProducer analysisTaskProducer;
    private final AiAnalysisClient aiClient;
    private final DailyAiQuotaService dailyQuota;

    public AnalysisService(AnalysisTaskMapper analysisTaskMapper,
                           AsrParagraphMapper asrParagraphMapper,
                           KeywordMapper keywordMapper,
                           OptimizationActionMapper optimizationActionMapper,
                           RecapNoteMapper recapNoteMapper,
                           RecordingMapper recordingMapper,
                           AnalysisTaskProducer analysisTaskProducer,
                           AiAnalysisClient aiClient,
                           DailyAiQuotaService dailyQuota) {
        this.analysisTaskMapper = analysisTaskMapper;
        this.asrParagraphMapper = asrParagraphMapper;
        this.keywordMapper = keywordMapper;
        this.optimizationActionMapper = optimizationActionMapper;
        this.recapNoteMapper = recapNoteMapper;
        this.recordingMapper = recordingMapper;
        this.analysisTaskProducer = analysisTaskProducer;
        this.aiClient = aiClient;
        this.dailyQuota = dailyQuota;
    }

    /**
     * v1.1.0 新增：用户在逐字稿页手动触发 AI 复盘。
     * 仅接受处于 TRANSCRIBED / FAILED 状态的任务（避免重复触发正在跑的任务）。
     * 配额超限抛 DAILY_QUOTA_EXHAUSTED。
     */
    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO startAiAnalysis(Long taskId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        String status = task.getStatus();
        if (!AnalysisStatus.TRANSCRIBED.getCode().equals(status)
                && !AnalysisStatus.FAILED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "当前任务状态不支持触发 AI 复盘：" + status);
        }

        // 必须有 ASR 文本
        if (task.getAsrText() == null || task.getAsrText().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该任务无逐字稿，无法进行 AI 分析");
        }

        // 每日配额 check（超限直接抛 DAILY_QUOTA_EXHAUSTED）
        dailyQuota.checkBeforeAnalyze(task.getUserId());

        // 切到 AI_PROCESSING
        task.setStatus(AnalysisStatus.AI_PROCESSING.getCode());
        task.setErrorMsg(null);
        task.setStartedAt(LocalDateTime.now());
        task.setCompletedAt(null);
        analysisTaskMapper.updateById(task);

        // 只有全场复盘代表录制记录的主分析状态；切片任务不能覆盖全场状态。
        if (RecapType.FULL.getCode().equals(task.getType()) && task.getRecordingId() != null) {
            Recording recording = recordingMapper.selectById(task.getRecordingId());
            if (recording != null) {
                recording.setAnalysisStatus(AnalysisStatus.AI_PROCESSING.getCode());
                recordingMapper.updateById(recording);
            }
        }

        final Long finalTaskId = task.getId();
        final Long finalRecordingId = task.getRecordingId();
        final String finalType = task.getType();
        final int finalPriority = task.getPriority() != null ? task.getPriority() : 5;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        analysisTaskProducer.send(new AnalysisTaskMessage(
                                finalTaskId, finalRecordingId, finalType, finalPriority));
                    }
                });

        log.info("User triggered AI analysis: taskId={}, userId={}", task.getId(), task.getUserId());
        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO createFullAnalysis(CreateFullAnalysisRequest req) {
        Recording recording = recordingMapper.selectById(req.getRecordingId());
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkRecordingOwnership(recording);

        AnalysisTask task = new AnalysisTask();
        task.setRecordingId(req.getRecordingId());
        task.setUserId(SecurityContextHelper.currentUserId());
        task.setOrgId(SecurityContextHelper.currentOrgId());
        task.setType(RecapType.FULL.getCode());
        task.setStatus(AnalysisStatus.PENDING.getCode());
        task.setPriority(5);
        task.setAiModel(req.getAiModel() != null ? req.getAiModel() : "doubao");
        task.setIndustry(req.getIndustry());
        task.setAsrWordCount(0);
        task.setSensitiveCount(0);
        task.setConsumedChars(0L);

        analysisTaskMapper.insert(task);
        final Long fTaskId = task.getId();
        final Long fRecId = task.getRecordingId();
        final String fType = task.getType();
        final int fPriority = task.getPriority();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        analysisTaskProducer.send(new AnalysisTaskMessage(fTaskId, fRecId, fType, fPriority));
                    }
                });
        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO createClipAnalysis(CreateClipAnalysisRequest req) {
        Recording recording = recordingMapper.selectById(req.getRecordingId());
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkRecordingOwnership(recording);

        validateClipCategory(req.getClipCategory());

        if (req.getClipStart() >= req.getClipEnd()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片开始时间必须小于结束时间");
        }

        AnalysisTask task = new AnalysisTask();
        task.setRecordingId(req.getRecordingId());
        task.setUserId(SecurityContextHelper.currentUserId());
        task.setOrgId(SecurityContextHelper.currentOrgId());
        task.setType(RecapType.CLIP.getCode());
        task.setStatus(AnalysisStatus.PENDING.getCode());
        task.setPriority(5);
        task.setAiModel(req.getAiModel() != null ? req.getAiModel() : "doubao");
        task.setClipStart(req.getClipStart());
        task.setClipEnd(req.getClipEnd());
        task.setClipCategory(req.getClipCategory());
        task.setClipFilename(req.getClipFilename());
        task.setClipFilePath(req.getClipFilePath());
        task.setClipRemark(req.getClipRemark());
        task.setAsrWordCount(0);
        task.setSensitiveCount(0);
        task.setConsumedChars(0L);

        analysisTaskMapper.insert(task);
        final Long fTaskId = task.getId();
        final Long fRecId = task.getRecordingId();
        final String fType = task.getType();
        final int fPriority = task.getPriority();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        analysisTaskProducer.send(new AnalysisTaskMessage(fTaskId, fRecId, fType, fPriority));
                    }
                });
        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO submitAsrAndAnalyze(SubmitAsrResultRequest req) {
        Recording recording = recordingMapper.selectById(req.getRecordingId());
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkRecordingOwnership(recording);

        // v1.1.0：autoAnalyze=false → 不触发 AI，只到 transcribed 停住，等用户手动触发
        //         null / true → 老行为（老客户端兼容）
        final boolean autoAnalyze = req.getAutoAnalyze() == null || Boolean.TRUE.equals(req.getAutoAnalyze());

        // 1. 创建分析任务：新客户端 autoAnalyze=false → TRANSCRIBED；老客户端 → AI_PROCESSING
        AnalysisTask task = new AnalysisTask();
        task.setRecordingId(req.getRecordingId());
        task.setUserId(SecurityContextHelper.currentUserId());
        task.setOrgId(SecurityContextHelper.currentOrgId());
        task.setType(RecapType.FULL.getCode());
        task.setStatus(autoAnalyze
                ? AnalysisStatus.AI_PROCESSING.getCode()
                : AnalysisStatus.TRANSCRIBED.getCode());
        task.setStartedAt(LocalDateTime.now());
        task.setPriority(5);
        task.setAiModel(req.getAiModel() != null ? req.getAiModel() : "doubao");
        task.setIndustry(req.getIndustry());
        task.setSensitiveCount(0);
        task.setConsumedChars(0L);

        // 2. 保存 ASR 文本
        StringBuilder fullText = new StringBuilder();
        int totalWordCount = 0;
        for (SubmitAsrResultRequest.AsrSegment seg : req.getSegments()) {
            fullText.append(seg.getText()).append("\n");
            totalWordCount += seg.getText().length();
        }
        task.setAsrText(fullText.toString().trim());
        task.setAsrWordCount(totalWordCount);

        analysisTaskMapper.insert(task);

        // 3. 保存 ASR 段落到数据库
        int index = 0;
        for (SubmitAsrResultRequest.AsrSegment seg : req.getSegments()) {
            AsrParagraph paragraph = new AsrParagraph();
            paragraph.setTaskId(task.getId());
            paragraph.setParagraphIndex(index++);
            paragraph.setStartTime(seg.getStartTime());
            paragraph.setEndTime(seg.getEndTime());
            paragraph.setTextContent(seg.getText());
            int wordCount = seg.getText().length();
            paragraph.setWordCount(wordCount);
            paragraph.setWordsPerMin(wordCount);
            paragraph.setIsHighlighted(0);
            asrParagraphMapper.insert(paragraph);
        }

        // 4. 更新录制记录的分析状态
        recording.setAnalysisStatus(autoAnalyze
                ? AnalysisStatus.AI_PROCESSING.getCode()
                : AnalysisStatus.TRANSCRIBED.getCode());
        recordingMapper.updateById(recording);

        // 5. 仅当 autoAnalyze 为 true 时事务提交后发 MQ 触发 AI 分析；否则等用户手动触发
        if (autoAnalyze) {
            final Long finalTaskId = task.getId();
            final Long finalRecordingId = task.getRecordingId();
            final String finalType = task.getType();
            final int finalPriority = task.getPriority();
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            analysisTaskProducer.send(new AnalysisTaskMessage(
                                    finalTaskId, finalRecordingId, finalType, finalPriority));
                        }
                    });
        }

        log.info("ASR result submitted: taskId={}, autoAnalyze={}, segments={}, words={}",
                task.getId(), autoAnalyze, req.getSegments().size(), totalWordCount);

        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    /**
     * 创建切片占位 task（status=transcribing），让前端立即在切片复盘列表里看到"逐字稿生成中"。
     * 真正的 ffmpeg 截取 + 客户端 ASR 在前端异步进行，完成后回调 {@link #submitClipAsrAndAnalyze} 携带 taskId 完成。
     */
    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO createClipDraft(CreateClipDraftRequest req) {
        Recording recording = recordingMapper.selectById(req.getRecordingId());
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkRecordingOwnership(recording);
        validateClipCategory(req.getClipCategory());
        if (req.getClipStart() >= req.getClipEnd()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片开始时间必须小于结束时间");
        }

        AnalysisTask task = new AnalysisTask();
        task.setRecordingId(req.getRecordingId());
        task.setUserId(SecurityContextHelper.currentUserId());
        task.setOrgId(SecurityContextHelper.currentOrgId());
        task.setType(RecapType.CLIP.getCode());
        task.setStatus(AnalysisStatus.TRANSCRIBING.getCode());
        task.setPriority(5);
        task.setAiModel(req.getAiModel() != null ? req.getAiModel() : "doubao");
        task.setClipStart(req.getClipStart());
        task.setClipEnd(req.getClipEnd());
        task.setClipCategory(req.getClipCategory());
        task.setClipFilename(req.getClipFilename());
        task.setClipRemark(req.getClipRemark());
        task.setSensitiveCount(0);
        task.setConsumedChars(0L);
        analysisTaskMapper.insert(task);

        log.info("Clip draft created, taskId={}, category={}, range={}-{}s",
                task.getId(), req.getClipCategory(), req.getClipStart(), req.getClipEnd());
        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO submitClipAsrAndAnalyze(SubmitClipAsrRequest req) {
        Recording recording = recordingMapper.selectById(req.getRecordingId());
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkRecordingOwnership(recording);
        validateClipCategory(req.getClipCategory());

        if (req.getClipStart() >= req.getClipEnd()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片开始时间必须小于结束时间");
        }

        // v1.1.0：切片自动 AI 复盘也要计入每日 10 次限额，超限直接抛 DAILY_QUOTA_EXHAUSTED
        dailyQuota.checkBeforeAnalyze(SecurityContextHelper.currentUserId());

        // 1. 创建/复用切片分析任务，状态切到 AI_PROCESSING
        AnalysisTask task;
        boolean isUpdate = false;
        if (req.getTaskId() != null) {
            // 升级模式：客户端已通过 createClipDraft 拿到 taskId，这里把 ASR 段落补全 + 状态推进
            task = analysisTaskMapper.selectById(req.getTaskId());
            if (task == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "切片任务不存在或已被清理");
            }
            if (!RecapType.CLIP.getCode().equals(task.getType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "taskId 不是切片类型");
            }
            // 简单复核：所有者一致
            if (!task.getUserId().equals(SecurityContextHelper.currentUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该切片任务");
            }
            isUpdate = true;
        } else {
            task = new AnalysisTask();
            task.setRecordingId(req.getRecordingId());
            task.setUserId(SecurityContextHelper.currentUserId());
            task.setOrgId(SecurityContextHelper.currentOrgId());
            task.setType(RecapType.CLIP.getCode());
            task.setPriority(5);
            task.setSensitiveCount(0);
            task.setConsumedChars(0L);
        }
        task.setStatus(AnalysisStatus.AI_PROCESSING.getCode());
        task.setStartedAt(LocalDateTime.now());
        task.setAiModel(req.getAiModel() != null ? req.getAiModel() : (task.getAiModel() != null ? task.getAiModel() : "doubao"));
        task.setClipStart(req.getClipStart());
        task.setClipEnd(req.getClipEnd());
        task.setClipCategory(req.getClipCategory());
        if (req.getClipFilename() != null) task.setClipFilename(req.getClipFilename());
        if (req.getClipFilePath() != null) task.setClipFilePath(req.getClipFilePath());
        if (req.getClipRemark() != null) task.setClipRemark(req.getClipRemark());

        // 2. 保存 ASR 文本
        StringBuilder fullText = new StringBuilder();
        int totalWordCount = 0;
        for (SubmitAsrResultRequest.AsrSegment seg : req.getSegments()) {
            fullText.append(seg.getText()).append("\n");
            totalWordCount += seg.getText().length();
        }
        task.setAsrText(fullText.toString().trim());
        task.setAsrWordCount(totalWordCount);

        if (isUpdate) {
            analysisTaskMapper.updateById(task);
            // update 模式下，先清掉历史段落（draft 阶段尚无段落，但安全起见做防护）
            asrParagraphMapper.delete(new LambdaQueryWrapper<AsrParagraph>().eq(AsrParagraph::getTaskId, task.getId()));
        } else {
            analysisTaskMapper.insert(task);
        }

        // 3. 保存 ASR 段落到数据库
        int index = 0;
        for (SubmitAsrResultRequest.AsrSegment seg : req.getSegments()) {
            AsrParagraph paragraph = new AsrParagraph();
            paragraph.setTaskId(task.getId());
            paragraph.setParagraphIndex(index++);
            paragraph.setStartTime(seg.getStartTime());
            paragraph.setEndTime(seg.getEndTime());
            paragraph.setTextContent(seg.getText());
            int wordCount = seg.getText().length();
            paragraph.setWordCount(wordCount);
            paragraph.setWordsPerMin(wordCount);
            paragraph.setIsHighlighted(0);
            asrParagraphMapper.insert(paragraph);
        }

        // 4. 事务提交后发送到消息队列触发 AI 分析
        final Long finalTaskId = task.getId();
        final Long finalRecordingId = task.getRecordingId();
        final String finalType = task.getType();
        final int finalPriority = task.getPriority();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        analysisTaskProducer.send(new AnalysisTaskMessage(
                                finalTaskId, finalRecordingId, finalType, finalPriority));
                    }
                });

        log.info("Clip ASR result submitted from desktop, taskId={}, category={}, segments={}, words={}",
                task.getId(), req.getClipCategory(), req.getSegments().size(), totalWordCount);

        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    @DataPermission
    public PageResult<AnalysisTaskVO> listTasks(String type, String status, Long streamerId, String clipCategory, int page, int size) {
        Long orgId = SecurityContextHelper.currentOrgId();

        LambdaQueryWrapper<AnalysisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisTask::getOrgId, orgId);
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(AnalysisTask::getType, type.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(AnalysisTask::getStatus, status.trim());
        }
        if (clipCategory != null && !clipCategory.trim().isEmpty()) {
            wrapper.eq(AnalysisTask::getClipCategory, clipCategory.trim());
        }
        if (streamerId != null) {
            // Filter by streamerId via recording table
            LambdaQueryWrapper<Recording> recWrapper = new LambdaQueryWrapper<>();
            recWrapper.eq(Recording::getStreamerId, streamerId);
            recWrapper.select(Recording::getId);
            List<Recording> recordings = recordingMapper.selectList(recWrapper);
            if (recordings.isEmpty()) {
                return PageResult.of(new ArrayList<>(), 0L, page, size);
            }
            List<Long> recordingIds = new ArrayList<>();
            for (Recording r : recordings) {
                recordingIds.add(r.getId());
            }
            wrapper.in(AnalysisTask::getRecordingId, recordingIds);
        }
        wrapper.orderByDesc(AnalysisTask::getCreatedAt);

        Page<AnalysisTask> pageParam = new Page<>(page, size);
        Page<AnalysisTask> result = analysisTaskMapper.selectPage(pageParam, wrapper);

        List<AnalysisTaskVO> items = new ArrayList<>();
        for (AnalysisTask t : result.getRecords()) {
            items.add(AnalysisTaskVO.fromEntity(t));
        }
        return PageResult.of(items, result.getTotal(), page, size);
    }

    @DataPermission
    public AnalysisTaskVO detail(Long id) {
        AnalysisTask task = analysisTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);
        return AnalysisTaskVO.fromEntity(task);
    }

    @DataPermission
    public PageResult<AsrParagraphVO> getParagraphs(Long taskId, int page, int size) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        LambdaQueryWrapper<AsrParagraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AsrParagraph::getTaskId, taskId);
        wrapper.orderByAsc(AsrParagraph::getParagraphIndex);

        Page<AsrParagraph> pageParam = new Page<>(page, size);
        Page<AsrParagraph> result = asrParagraphMapper.selectPage(pageParam, wrapper);

        List<AsrParagraphVO> items = new ArrayList<>();
        for (AsrParagraph p : result.getRecords()) {
            items.add(AsrParagraphVO.fromEntity(p));
        }

        return PageResult.of(items, result.getTotal(), page, size);
    }

    @DataPermission
    public KeywordListVO getKeywords(Long taskId, String type, String category, int page, int size) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        LambdaQueryWrapper<Keyword> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Keyword::getTaskId, taskId);
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Keyword::getType, type.trim());
        }
        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(Keyword::getCategory, category.trim());
        }
        wrapper.orderByDesc(Keyword::getTotalCount);

        Page<Keyword> pageParam = new Page<>(page, size);
        Page<Keyword> result = keywordMapper.selectPage(pageParam, wrapper);

        List<KeywordVO> items = new ArrayList<>();
        for (Keyword k : result.getRecords()) {
            items.add(KeywordVO.fromEntity(k));
        }

        // Build stats
        LambdaQueryWrapper<Keyword> statsWrapper = new LambdaQueryWrapper<>();
        statsWrapper.eq(Keyword::getTaskId, taskId);
        List<Keyword> allKeywords = keywordMapper.selectList(statsWrapper);

        int totalOperational = 0;
        int totalSensitive = 0;
        for (Keyword k : allKeywords) {
            if ("operational".equals(k.getType())) {
                totalOperational++;
            } else if ("sensitive".equals(k.getType())) {
                totalSensitive++;
            }
        }

        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalOperational", totalOperational);
        stats.put("totalSensitive", totalSensitive);

        return KeywordListVO.of(items, stats, result.getTotal());
    }

    @DataPermission
    public AnalysisTaskVO getDiagnosis(Long id) {
        AnalysisTask task = analysisTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);
        return AnalysisTaskVO.fromEntity(task);
    }

    @Transactional
    @DataPermission
    public void saveOptimization(Long taskId, SaveOptimizationRequest req) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        Long userId = SecurityContextHelper.currentUserId();

        // Update task-level optimization fields
        task.setOptimizationAction(req.getAction());
        task.setOptimizationGoal(req.getGoal());
        analysisTaskMapper.updateById(task);

        // Also save to optimization_actions table
        OptimizationAction action = new OptimizationAction();
        action.setTaskId(taskId);
        action.setUserId(userId);
        action.setAction(req.getAction());
        action.setGoal(req.getGoal());
        optimizationActionMapper.insert(action);
    }

    @DataPermission
    public NoteVO getNotes(Long taskId, String tabType) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        LambdaQueryWrapper<RecapNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecapNote::getTaskId, taskId);
        wrapper.eq(RecapNote::getTabType, tabType);
        RecapNote note = recapNoteMapper.selectOne(wrapper);

        if (note == null) {
            NoteVO emptyNote = new NoteVO();
            emptyNote.setTabType(tabType);
            emptyNote.setContentHtml("");
            return emptyNote;
        }
        return NoteVO.fromEntity(note);
    }

    @Transactional
    @DataPermission
    public NoteVO saveNotes(Long taskId, SaveNoteRequest req) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        LambdaQueryWrapper<RecapNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecapNote::getTaskId, taskId);
        wrapper.eq(RecapNote::getTabType, req.getTabType());
        RecapNote existing = recapNoteMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setContentHtml(req.getContentHtml());
            recapNoteMapper.updateById(existing);
            return NoteVO.fromEntity(existing);
        } else {
            RecapNote note = new RecapNote();
            note.setTaskId(taskId);
            note.setUserId(SecurityContextHelper.currentUserId());
            note.setTabType(req.getTabType());
            note.setContentHtml(req.getContentHtml());
            recapNoteMapper.insert(note);
            return NoteVO.fromEntity(note);
        }
    }

    @Transactional
    @DataPermission
    public AnalysisTaskCreateVO reAnalyze(Long id) {
        AnalysisTask task = analysisTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        // 如果已有 ASR 文本，直接跳到 AI 分析阶段（无需重新 ASR）
        if (task.getAsrText() != null && !task.getAsrText().trim().isEmpty()) {
            task.setStatus(AnalysisStatus.AI_PROCESSING.getCode());
        } else {
            // 无 ASR 文本，需要桌面端重新提供 ASR
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该任务无ASR数据，请从桌面端重新创建分析任务");
        }
        task.setErrorMsg(null);
        task.setStartedAt(LocalDateTime.now());
        task.setCompletedAt(null);
        analysisTaskMapper.updateById(task);

        final Long finalTaskId = task.getId();
        final Long finalRecordingId = task.getRecordingId();
        final String finalType = task.getType();
        final int finalPriority = task.getPriority();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        analysisTaskProducer.send(new AnalysisTaskMessage(
                                finalTaskId, finalRecordingId, finalType, finalPriority));
                    }
                });

        return AnalysisTaskCreateVO.of(task.getId(), task.getStatus());
    }

    @Transactional
    @DataPermission
    public void cancel(Long id) {
        AnalysisTask task = analysisTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        String status = task.getStatus();
        if (AnalysisStatus.COMPLETED.getCode().equals(status) || AnalysisStatus.FAILED.getCode().equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务已结束，无法取消");
        }

        task.setStatus(AnalysisStatus.FAILED.getCode());
        task.setErrorMsg("用户手动取消");
        analysisTaskMapper.updateById(task);
    }

    /**
     * Generate AI-optimized text for a task. If already generated, return existing.
     */
    @Transactional
    @DataPermission
    public String generateOptimizedText(Long taskId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        // Return cached result if exists
        if (task.getOptimizedText() != null && !task.getOptimizedText().trim().isEmpty()) {
            return task.getOptimizedText();
        }

        // Need ASR text to optimize
        String asrText = task.getAsrText();
        if (asrText == null || asrText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "暂无转写文本，无法生成优化原文");
        }

        String optimized = aiClient.optimizeText(task.getUserId(), asrText);
        task.setOptimizedText(optimized);
        analysisTaskMapper.updateById(task);

        return optimized;
    }

    /**
     * Re-classify paragraphs for a completed task that has no scriptCategory set.
     */
    @Transactional
    @DataPermission
    public void classifyParagraphs(Long taskId) {
        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        checkTaskOwnership(task);

        List<AsrParagraph> paragraphs = asrParagraphMapper.selectList(
                new LambdaQueryWrapper<AsrParagraph>()
                        .eq(AsrParagraph::getTaskId, taskId)
                        .orderByAsc(AsrParagraph::getParagraphIndex));

        if (paragraphs.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该任务暂无段落数据");
        }

        List<String> texts = new ArrayList<>();
        for (AsrParagraph p : paragraphs) {
            texts.add(p.getTextContent());
        }

        List<String> categories = aiClient.classifyParagraphs(task.getUserId(), texts);
        if (categories == null || categories.size() != paragraphs.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "话术分类失败，请稍后重试");
        }

        for (int i = 0; i < paragraphs.size(); i++) {
            AsrParagraph p = paragraphs.get(i);
            p.setScriptCategory(categories.get(i));
            asrParagraphMapper.updateById(p);
        }
    }

    private void checkRecordingOwnership(Recording recording) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long currentOrgId = SecurityContextHelper.currentOrgId();
        if (currentOrgId == null || !currentOrgId.equals(recording.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "你没有这条录制的访问权限");
        }
    }

    @Transactional
    @DataPermission
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ids不能为空");
        }
        for (Long id : ids) {
            AnalysisTask task = analysisTaskMapper.selectById(id);
            if (task == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在: " + id);
            }
            checkTaskOwnership(task);
        }
        int count = 0;
        for (Long id : ids) {
            count += analysisTaskMapper.deleteById(id);
        }
        return count;
    }

    private void checkTaskOwnership(AnalysisTask task) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long currentOrgId = SecurityContextHelper.currentOrgId();
        if (currentOrgId == null || !currentOrgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "无权访问该分析任务");
        }
    }

    private void validateClipCategory(String category) {
        for (ClipCategory c : ClipCategory.values()) {
            if (c.getCode().equals(category)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的切片分类: " + category);
    }
}
