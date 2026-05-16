package com.dianjinshou.modules.comparison.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.comparison.dto.CreateComparisonRequest;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.Keyword;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.KeywordMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComparisonService {

    private final ComparisonMapper comparisonMapper;
    private final RecordingMapper recordingMapper;
    private final KeywordMapper keywordMapper;
    private final StreamerMapper streamerMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final CloudFileMapper cloudFileMapper;

    public ComparisonService(ComparisonMapper comparisonMapper,
                             RecordingMapper recordingMapper,
                             KeywordMapper keywordMapper,
                             StreamerMapper streamerMapper,
                             AnalysisTaskMapper analysisTaskMapper,
                             CloudFileMapper cloudFileMapper) {
        this.comparisonMapper = comparisonMapper;
        this.recordingMapper = recordingMapper;
        this.keywordMapper = keywordMapper;
        this.streamerMapper = streamerMapper;
        this.analysisTaskMapper = analysisTaskMapper;
        this.cloudFileMapper = cloudFileMapper;
    }

    @Transactional
    public ComparisonVO create(CreateComparisonRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();

        Recording optimize = recordingMapper.selectById(request.getRecordingIdOptimize());
        if (optimize == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优化场次录制不存在");
        }
        checkRecordingOrgAccess(optimize);

        Recording reference = recordingMapper.selectById(request.getRecordingIdReference());
        if (reference == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参考场次录制不存在");
        }
        checkRecordingOrgAccess(reference);

        // Auto-resolve taskIds from recordings
        Long taskIdOpt = findLatestTaskId(request.getRecordingIdOptimize(), request.getType());
        Long taskIdRef = findLatestTaskId(request.getRecordingIdReference(), request.getType());

        Comparison comparison = new Comparison();
        comparison.setUserId(userId);
        comparison.setOrgId(orgId);
        comparison.setType(request.getType());
        comparison.setRecordingIdOptimize(request.getRecordingIdOptimize());
        comparison.setRecordingIdReference(request.getRecordingIdReference());
        comparison.setTaskIdOptimize(taskIdOpt);
        comparison.setTaskIdReference(taskIdRef);
        comparison.setClipCategory(request.getClipCategory());
        comparison.setAiModel(request.getAiModel() != null ? request.getAiModel() : "deepseek_r1");
        comparison.setStatus("pending");
        comparisonMapper.insert(comparison);

        return ComparisonVO.fromEntity(comparison);
    }

    public PageResult<ComparisonVO> list(String type, String status, String startDate, String endDate, int page, int size) {
        Long orgId = SecurityContextHelper.currentOrgId();

        LambdaQueryWrapper<Comparison> wrapper = new LambdaQueryWrapper<Comparison>()
                .eq(orgId != null, Comparison::getOrgId, orgId)
                .eq(type != null, Comparison::getType, type);

        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(Comparison::getStatus, status.trim());
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (startDate != null && !startDate.trim().isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate.trim(), fmt).atStartOfDay();
            wrapper.ge(Comparison::getCreatedAt, start);
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate.trim(), fmt).atTime(23, 59, 59);
            wrapper.le(Comparison::getCreatedAt, end);
        }

        wrapper.orderByDesc(Comparison::getCreatedAt);

        Page<Comparison> pageResult = comparisonMapper.selectPage(new Page<>(page, size), wrapper);

        List<ComparisonVO> items = pageResult.getRecords().stream()
                .map(c -> {
                    ComparisonVO vo = ComparisonVO.fromEntity(c);
                    populateStreamerInfo(vo, c.getRecordingIdOptimize(), c.getRecordingIdReference());
                    populateClipInfo(vo, c.getTaskIdOptimize(), c.getTaskIdReference());
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(items, pageResult.getTotal(), page, size);
    }

    public ComparisonVO detail(Long id) {
        Comparison comparison = comparisonMapper.selectById(id);
        if (comparison == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对比记录不存在");
        }
        checkComparisonOrgAccess(comparison);
        ComparisonVO vo = ComparisonVO.fromEntity(comparison);
        // 与 list 接口保持一致：填充两侧主播头像/名称、切片信息，
        // 否则前端列表（如 AI 助手对比 tab）拿不到主播展示数据。
        populateStreamerInfo(vo, comparison.getRecordingIdOptimize(), comparison.getRecordingIdReference());
        populateClipInfo(vo, comparison.getTaskIdOptimize(), comparison.getTaskIdReference());
        return vo;
    }

    @Transactional
    public ComparisonVO swap(Long id) {
        Comparison comparison = comparisonMapper.selectById(id);
        if (comparison == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对比记录不存在");
        }
        checkComparisonOrgAccess(comparison);

        // Swap optimize and reference
        Long tempRecording = comparison.getRecordingIdOptimize();
        comparison.setRecordingIdOptimize(comparison.getRecordingIdReference());
        comparison.setRecordingIdReference(tempRecording);

        Long tempTask = comparison.getTaskIdOptimize();
        comparison.setTaskIdOptimize(comparison.getTaskIdReference());
        comparison.setTaskIdReference(tempTask);

        comparisonMapper.updateById(comparison);
        return ComparisonVO.fromEntity(comparison);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Long orgId = SecurityContextHelper.currentOrgId();
        for (Long id : ids) {
            Comparison comparison = comparisonMapper.selectById(id);
            if (comparison != null) {
                checkComparisonOrgAccess(comparison);
                comparisonMapper.deleteById(id);
            }
        }
    }

    public List<Keyword> getKeywords(Long comparisonId) {
        Comparison comparison = comparisonMapper.selectById(comparisonId);
        if (comparison == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对比记录不存在");
        }
        checkComparisonOrgAccess(comparison);

        return keywordMapper.selectList(
                new LambdaQueryWrapper<Keyword>()
                        .eq(Keyword::getComparisonId, comparisonId)
                        .orderByDesc(Keyword::getTotalCount));
    }

    private void populateStreamerInfo(ComparisonVO vo, Long recordingIdOptimize, Long recordingIdReference) {
        StreamerSnapshot opt = resolveStreamerSnapshot(recordingIdOptimize);
        if (opt != null) {
            vo.setAnchorNameOptimize(opt.anchorName);
            vo.setAnchorAvatarOptimize(opt.anchorAvatar);
            vo.setLocalFileNameOptimize(opt.localFileName);
        }
        StreamerSnapshot ref = resolveStreamerSnapshot(recordingIdReference);
        if (ref != null) {
            vo.setAnchorNameReference(ref.anchorName);
            vo.setAnchorAvatarReference(ref.anchorAvatar);
            vo.setLocalFileNameReference(ref.localFileName);
        }
    }

    /**
     * 主播快照来源优先级：
     *   1) recording（含已软删，因为对比记录可能引用一条已被软删的录像 — 仍需展示当时的主播信息）
     *   2) cloud_files 快照（recording 实体也彻底清理后仍能展示）
     */
    private StreamerSnapshot resolveStreamerSnapshot(Long recordingId) {
        if (recordingId == null) return null;
        Recording rec = recordingMapper.selectByIdIncludeDeleted(recordingId);
        if (rec != null) {
            StreamerSnapshot snap = new StreamerSnapshot();
            snap.localFileName = rec.getLocalFileName();
            if (rec.getStreamerId() != null) {
                Streamer s = streamerMapper.selectById(rec.getStreamerId());
                if (s != null) {
                    snap.anchorName = s.getAnchorName();
                    snap.anchorAvatar = s.getAnchorAvatar();
                }
            }
            return snap;
        }
        // recording 已删除：从 cloud_files 兜底（云空间保留了主播名/头像/文件名快照）
        try {
            CloudFile cf = cloudFileMapper.selectOne(
                    new LambdaQueryWrapper<CloudFile>()
                            .eq(CloudFile::getRecordingId, recordingId)
                            .last("LIMIT 1"));
            if (cf != null) {
                StreamerSnapshot snap = new StreamerSnapshot();
                snap.anchorName = cf.getAnchorName();
                snap.localFileName = cf.getDisplayName() != null ? cf.getDisplayName() : cf.getFileName();
                if (cf.getStreamerId() != null) {
                    Streamer s = streamerMapper.selectById(cf.getStreamerId());
                    if (s != null) snap.anchorAvatar = s.getAnchorAvatar();
                }
                return snap;
            }
        } catch (Exception ignore) { /* 兜底失败不阻塞主流程 */ }
        return null;
    }

    private static class StreamerSnapshot {
        String anchorName;
        String anchorAvatar;
        String localFileName;
    }

    private void populateClipInfo(ComparisonVO vo, Long taskIdOptimize, Long taskIdReference) {
        if (taskIdOptimize != null) {
            AnalysisTask task = analysisTaskMapper.selectById(taskIdOptimize);
            if (task != null) {
                vo.setClipFilenameOptimize(task.getClipFilename());
                vo.setClipRemarkOptimize(task.getClipRemark());
            }
        }
        if (taskIdReference != null) {
            AnalysisTask task = analysisTaskMapper.selectById(taskIdReference);
            if (task != null) {
                vo.setClipFilenameReference(task.getClipFilename());
                vo.setClipRemarkReference(task.getClipRemark());
            }
        }
    }

    private Long findLatestTaskId(Long recordingId, String type) {
        // v1.1.0：放宽到只要有 ASR 就能对比（completed / transcribed / ai_processing / failed）
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

    private void checkComparisonOrgAccess(Comparison comparison) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long currentOrgId = SecurityContextHelper.currentOrgId();
        if (currentOrgId == null || !currentOrgId.equals(comparison.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "无权访问该对比记录");
        }
    }
}
