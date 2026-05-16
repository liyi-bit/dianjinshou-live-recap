package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.vo.AnalysisTaskVO;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudReadonlyDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;

@Service
public class CloudRestoreService {

    private final CloudFileService cloudFileService;
    private final CloudFileMapper cloudFileMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final ComparisonMapper comparisonMapper;
    private final RecordingMapper recordingMapper;

    public CloudRestoreService(CloudFileService cloudFileService,
                               CloudFileMapper cloudFileMapper,
                               AnalysisTaskMapper analysisTaskMapper,
                               ComparisonMapper comparisonMapper,
                               RecordingMapper recordingMapper) {
        this.cloudFileService = cloudFileService;
        this.cloudFileMapper = cloudFileMapper;
        this.analysisTaskMapper = analysisTaskMapper;
        this.comparisonMapper = comparisonMapper;
        this.recordingMapper = recordingMapper;
    }

    public CloudReadonlyDetailVO readonlyDetail(Long fileId) {
        CloudFile file = cloudFileService.getOwnedFile(fileId);
        if (!"active".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "文件尚未上传完成");
        }

        CloudReadonlyDetailVO vo = new CloudReadonlyDetailVO();
        vo.setFile(CloudFileVO.fromEntity(file));
        if (hasText(file.getStorageKey())) {
            vo.setSignedUrl(cloudFileService.signedUrl(fileId));
        }
        vo.setReadonly(true);
        vo.setAllowDownload(hasText(file.getStorageKey()));
        vo.setAllowDownloadToLocal(hasText(file.getStorageKey()));

        if (isComparison(file.getBusinessType())) {
            Comparison comparison = resolveComparison(file);
            vo.setComparisonDetail(comparison != null ? ComparisonVO.fromEntity(comparison) : null);
        } else {
            AnalysisTask task = resolveAnalysisTask(file);
            vo.setRecapDetail(task != null ? AnalysisTaskVO.fromEntity(task) : null);
        }
        return vo;
    }

    @Transactional
    public CloudFileVO markDownloadToLocalComplete(Long fileId, String localFilePath) {
        CloudFile file = cloudFileService.getOwnedFile(fileId);
        file.setReadonlyRestored(true);
        file.setLocalExists(true);
        cloudFileMapper.updateById(file);

        AnalysisTask task = resolveAnalysisTask(file);
        if (task != null && "clip_recap".equals(file.getBusinessType())) {
            task.setClipFilePath(localFilePath);
            analysisTaskMapper.updateById(task);
        }

        if (file.getRecordingId() != null) {
            Recording recording = recordingMapper.selectById(file.getRecordingId());
            if (recording != null && owns(recording.getUserId())) {
                recording.setLocalFilePath(localFilePath);
                recording.setLocalFileName(fileNameOf(localFilePath));
                recordingMapper.updateById(recording);
            }
        }

        return CloudFileVO.fromEntity(file);
    }

    private AnalysisTask resolveAnalysisTask(CloudFile file) {
        String expectedType = "clip_recap".equals(file.getBusinessType()) ? "clip" : "full";

        Long recordingId = file.getRecordingId() != null ? file.getRecordingId() : file.getBusinessId();
        if (recordingId != null) {
            AnalysisTask byRecording = analysisTaskMapper.selectOne(
                    new LambdaQueryWrapper<AnalysisTask>()
                            .eq(AnalysisTask::getRecordingId, recordingId)
                            .eq(AnalysisTask::getType, expectedType)
                            .eq(AnalysisTask::getUserId, currentUserId())
                            .orderByDesc(AnalysisTask::getCreatedAt)
                            .last("LIMIT 1"));
            if (byRecording != null) {
                return byRecording;
            }
        }

        Long directId = "clip_recap".equals(file.getBusinessType()) && file.getClipId() != null
                ? file.getClipId()
                : file.getBusinessId();
        if (directId == null) {
            return null;
        }
        AnalysisTask direct = analysisTaskMapper.selectById(directId);
        if (direct != null && owns(direct.getUserId()) && expectedType.equals(direct.getType())) {
            return direct;
        }
        return null;
    }

    private Comparison resolveComparison(CloudFile file) {
        Long comparisonId = file.getComparisonId() != null ? file.getComparisonId() : file.getBusinessId();
        if (comparisonId == null) {
            return null;
        }
        Comparison comparison = comparisonMapper.selectById(comparisonId);
        if (comparison != null && owns(comparison.getUserId())) {
            return comparison;
        }
        return null;
    }

    private boolean isComparison(String businessType) {
        return "full_comparison".equals(businessType) || "clip_comparison".equals(businessType);
    }

    private boolean owns(Long userId) {
        return userId != null && userId.equals(currentUserId());
    }

    private Long currentUserId() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String fileNameOf(String localFilePath) {
        try {
            return Paths.get(localFilePath).getFileName().toString();
        } catch (Exception e) {
            return localFilePath;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
