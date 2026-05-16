package com.dianjinshou.modules.recap.task;

import com.dianjinshou.common.enums.AnalysisStatus;
import com.dianjinshou.common.enums.RecapType;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.service.AiAnalysisService;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 分析任务核心处理器：AI → 完成
 * 被 AnalysisTaskHandler（RabbitMQ模式）和 AnalysisTaskProducer（同步回退模式）共用
 *
 * ASR 已改为桌面端本机模型生成，后端只消费已提交的逐字稿。
 */
@Component
public class AnalysisTaskProcessor {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskProcessor.class);

    private final AnalysisTaskMapper analysisTaskMapper;
    private final RecordingMapper recordingMapper;
    private final AiAnalysisService aiAnalysisService;
    private final UserMapper userMapper;

    public AnalysisTaskProcessor(AnalysisTaskMapper analysisTaskMapper,
                                 RecordingMapper recordingMapper,
                                 AiAnalysisService aiAnalysisService,
                                 UserMapper userMapper) {
        this.analysisTaskMapper = analysisTaskMapper;
        this.recordingMapper = recordingMapper;
        this.aiAnalysisService = aiAnalysisService;
        this.userMapper = userMapper;
    }

    public void process(Long taskId) {
        log.info("Processing analysis task: taskId={}", taskId);

        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("Analysis task not found: {}", taskId);
            return;
        }

        try {
            if (!AnalysisStatus.AI_PROCESSING.getCode().equals(task.getStatus())) {
                throw new IllegalStateException("当前版本不支持服务端 ASR，请先在桌面端用本机 ASR 生成逐字稿后再发起 AI 分析");
            } else {
                log.info("Skipping ASR for taskId={} (transcript already submitted by desktop)", taskId);
            }

            // Stage 2: AI Processing
            updateStatus(task, AnalysisStatus.AI_PROCESSING.getCode(), null, null);
            aiAnalysisService.processAiAnalysis(task);

            // Stage 3: Completed
            updateStatus(task, AnalysisStatus.COMPLETED.getCode(), null, LocalDateTime.now());
            syncRecordingStatusIfFull(task, AnalysisStatus.COMPLETED.getCode());
            // Stage 4: bill the user — increment ai_quota_used (chars) + duration_quota_used (seconds).
            chargeUserQuota(task);
            log.info("Analysis task completed: taskId={}", taskId);

        } catch (Exception e) {
            log.error("Analysis task failed: taskId={}", taskId, e);
            task.setStatus(AnalysisStatus.FAILED.getCode());
            task.setErrorMsg(e.getMessage() != null ? e.getMessage() : "未知错误");
            task.setCompletedAt(LocalDateTime.now());
            analysisTaskMapper.updateById(task);
            syncRecordingStatusIfFull(task, AnalysisStatus.FAILED.getCode());
        }
    }

    /**
     * Bill the task owner: increase ai_quota_used by the AI tokens consumed and
     * duration_quota_used by the recording's duration (seconds). Best-effort —
     * a failure here must not roll back the analysis result.
     */
    private void chargeUserQuota(AnalysisTask task) {
        try {
            User user = userMapper.selectById(task.getUserId());
            if (user == null) return;
            long aiCharge = task.getConsumedChars() != null ? task.getConsumedChars() : 0L;
            long durationCharge = 0L;
            Recording recording = recordingMapper.selectById(task.getRecordingId());
            if (recording != null && recording.getDuration() != null) {
                durationCharge = recording.getDuration().longValue();
            }
            long aiUsed = (user.getAiQuotaUsed() != null ? user.getAiQuotaUsed() : 0L) + aiCharge;
            long durUsed = (user.getDurationQuotaUsed() != null ? user.getDurationQuotaUsed() : 0L) + durationCharge;
            user.setAiQuotaUsed(aiUsed);
            user.setDurationQuotaUsed(durUsed);
            userMapper.updateById(user);
            log.info("Quota charged: userId={} +ai={} chars +duration={}s (total ai={}, dur={})",
                    user.getId(), aiCharge, durationCharge, aiUsed, durUsed);
        } catch (Exception e) {
            log.warn("Failed to charge quota for taskId={}", task.getId(), e);
        }
    }

    private void syncRecordingStatusIfFull(AnalysisTask task, String analysisStatus) {
        if (task == null || !RecapType.FULL.getCode().equals(task.getType())) {
            return;
        }
        syncRecordingStatus(task.getRecordingId(), analysisStatus);
    }

    private void syncRecordingStatus(Long recordingId, String analysisStatus) {
        try {
            Recording recording = recordingMapper.selectById(recordingId);
            if (recording != null) {
                recording.setAnalysisStatus(analysisStatus);
                recordingMapper.updateById(recording);
            }
        } catch (Exception e) {
            log.warn("Failed to sync recording analysisStatus: recordingId={}", recordingId, e);
        }
    }

    private void updateStatus(AnalysisTask task, String status,
                              LocalDateTime startedAt, LocalDateTime completedAt) {
        task.setStatus(status);
        if (startedAt != null) {
            task.setStartedAt(startedAt);
        }
        if (completedAt != null) {
            task.setCompletedAt(completedAt);
        }
        analysisTaskMapper.updateById(task);
    }
}
