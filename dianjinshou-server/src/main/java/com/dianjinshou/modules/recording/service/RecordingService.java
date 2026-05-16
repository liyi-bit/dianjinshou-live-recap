package com.dianjinshou.modules.recording.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.DataPermission;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.recording.dto.CompleteRecordingRequest;
import com.dianjinshou.modules.recording.dto.CreateRecordingRequest;
import com.dianjinshou.modules.recording.dto.RecordingQueryRequest;
import com.dianjinshou.modules.recording.dto.RenameRequest;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.recording.vo.RecordingListVO;
import com.dianjinshou.modules.recording.vo.RecordingVO;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecordingService {

    private static final Logger log = LoggerFactory.getLogger(RecordingService.class);

    private final RecordingMapper recordingMapper;
    private final StreamerMapper streamerMapper;
    private final AnalysisTaskMapper analysisTaskMapper;

    public RecordingService(RecordingMapper recordingMapper, StreamerMapper streamerMapper,
                            AnalysisTaskMapper analysisTaskMapper) {
        this.recordingMapper = recordingMapper;
        this.streamerMapper = streamerMapper;
        this.analysisTaskMapper = analysisTaskMapper;
    }

    @Transactional
    @DataPermission
    public RecordingVO create(CreateRecordingRequest req) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();

        Recording recording = new Recording();
        recording.setStreamerId(req.getStreamerId());
        recording.setUserId(userId);
        recording.setOrgId(orgId);
        recording.setLocalFilePath(req.getLocalFilePath());
        recording.setLocalFileName(req.getLocalFileName());
        recording.setStreamUrl(req.getStreamUrl());
        recording.setResolution(req.getResolution());
        recording.setSessionId(req.getSessionId());
        recording.setStartTime(LocalDateTime.now());
        recording.setStatus("recording");
        // v1.1.0: 状态流 录制中 → 逐字稿生成中 → 未分析 → AI分析中 → 分析完成
        recording.setAnalysisStatus("recording");

        recordingMapper.insert(recording);

        // Increment streamer's totalSessions and todaySessions
        if (req.getStreamerId() != null) {
            Streamer streamer = streamerMapper.selectById(req.getStreamerId());
            if (streamer != null) {
                // totalSessions / todaySessions 都不在此写入：StreamerService.list/detail 返回时实时 count
                streamer.setLastLiveAt(LocalDateTime.now());
                streamerMapper.updateById(streamer);
            }
        }

        return RecordingVO.fromEntity(recording);
    }

    @Transactional
    @DataPermission
    public RecordingVO complete(Long id, CompleteRecordingRequest req) {
        Recording recording = recordingMapper.selectById(id);
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkOwnership(recording);

        if (req.getLocalFilePath() != null) {
            recording.setLocalFilePath(req.getLocalFilePath());
        }
        if (req.getLocalFileName() != null) {
            recording.setLocalFileName(req.getLocalFileName());
        }
        if (req.getDuration() != null) {
            recording.setDuration(req.getDuration());
        }
        if (req.getFileSize() != null) {
            recording.setFileSize(req.getFileSize());
        }
        recording.setEndTime(LocalDateTime.now());
        String newStatus = req.getStatus() != null ? req.getStatus() : "completed";
        recording.setStatus(newStatus);
        if (req.getErrorMsg() != null) {
            recording.setErrorMsg(req.getErrorMsg());
        }

        // v1.1.0: 录制完成后即进入"逐字稿生成中"（desktop 会异步跑 ASR）。
        //   录制失败的情况下保持原值，由前端按 recording.status 判断"录制失败"。
        //   后续 submitAsrResult 会把 analysisStatus 推到 transcribed/ai_processing。
        if ("completed".equals(newStatus) && "recording".equals(recording.getAnalysisStatus())) {
            recording.setAnalysisStatus("transcribing");
        }

        recordingMapper.updateById(recording);

        return RecordingVO.fromEntity(recording);
    }

    /**
     * v1.1.0：桌面端启动时调用，拿到当前用户 analysis_status='transcribing' 卡住的记录。
     * 桌面端按 localFilePath 是否还在决定：重跑 ASR or 标记 failed。
     */
    @DataPermission
    public List<RecordingVO> listPendingAsr() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) return new ArrayList<>();
        LambdaQueryWrapper<Recording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recording::getUserId, userId);
        wrapper.eq(Recording::getAnalysisStatus, "transcribing");
        wrapper.orderByDesc(Recording::getCreatedAt);
        wrapper.last("LIMIT 50");
        List<Recording> list = recordingMapper.selectList(wrapper);
        List<RecordingVO> vos = new ArrayList<>(list.size());
        for (Recording r : list) vos.add(RecordingVO.fromEntity(r));
        return vos;
    }

    /**
     * v1.1.0：桌面端在本地 MP4 文件已丢失、无法续跑 ASR 时调用，把卡住的记录标为 failed。
     */
    @Transactional
    @DataPermission
    public void markAsrFailed(Long id, String reason) {
        Recording recording = recordingMapper.selectById(id);
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkOwnership(recording);
        if (!"transcribing".equals(recording.getAnalysisStatus())
                && !"recording".equals(recording.getAnalysisStatus())) {
            // 已经不是中间态，不重复改
            return;
        }
        recording.setAnalysisStatus("failed");
        if (reason != null && !reason.isEmpty()) {
            recording.setErrorMsg(reason);
        }
        recordingMapper.updateById(recording);
    }

    @DataPermission
    public PageResult<RecordingListVO> list(RecordingQueryRequest req) {
        int page = req.getPageOrDefault();
        int size = req.getSizeOrDefault();

        LambdaQueryWrapper<Recording> wrapper = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(wrapper, Recording::getOrgId);

        if (req.getStreamerId() != null) {
            wrapper.eq(Recording::getStreamerId, req.getStreamerId());
        }
        if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
            wrapper.eq(Recording::getStatus, req.getStatus().trim());
        }
        if (req.getAnalysisStatus() != null && !req.getAnalysisStatus().trim().isEmpty()) {
            wrapper.eq(Recording::getAnalysisStatus, req.getAnalysisStatus().trim());
        }

        // Date range filter
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (req.getStartDate() != null && !req.getStartDate().trim().isEmpty()) {
            LocalDateTime start = LocalDate.parse(req.getStartDate().trim(), fmt).atStartOfDay();
            wrapper.ge(Recording::getStartTime, start);
        }
        if (req.getEndDate() != null && !req.getEndDate().trim().isEmpty()) {
            LocalDateTime end = LocalDate.parse(req.getEndDate().trim(), fmt).atTime(23, 59, 59);
            wrapper.le(Recording::getStartTime, end);
        }

        // Tab filter
        if (req.getTab() != null && !req.getTab().trim().isEmpty()) {
            String tab = req.getTab().trim();
            if ("COMPLETED".equals(tab)) {
                wrapper.eq(Recording::getAnalysisStatus, "completed");
            } else if ("DIAG_DONE".equals(tab)) {
                wrapper.eq(Recording::getAnalysisStatus, "completed");
            }
            // ALL = no extra filter
        }

        wrapper.orderByDesc(Recording::getCreatedAt);

        Page<Recording> pageParam = new Page<>(page, size);
        Page<Recording> result = recordingMapper.selectPage(pageParam, wrapper);

        // Collect IDs for batch queries
        List<Long> recordingIds = new ArrayList<>();
        java.util.Set<Long> streamerIds = new java.util.HashSet<>();
        for (Recording r : result.getRecords()) {
            recordingIds.add(r.getId());
            if (r.getStreamerId() != null) {
                streamerIds.add(r.getStreamerId());
            }
        }

        // Build a map: recordingId -> latestTaskId
        Map<Long, Long> latestTaskMap = new HashMap<>();
        if (!recordingIds.isEmpty()) {
            for (Long rid : recordingIds) {
                LambdaQueryWrapper<AnalysisTask> taskWrapper = new LambdaQueryWrapper<>();
                taskWrapper.eq(AnalysisTask::getRecordingId, rid);
                taskWrapper.eq(AnalysisTask::getType, "full");
                taskWrapper.orderByDesc(AnalysisTask::getId);
                taskWrapper.last("LIMIT 1");
                AnalysisTask latestTask = analysisTaskMapper.selectOne(taskWrapper);
                if (latestTask != null) {
                    latestTaskMap.put(rid, latestTask.getId());
                }
            }
        }

        // Build a map: streamerId -> Streamer for avatar/name
        Map<Long, Streamer> streamerMap = new HashMap<>();
        for (Long sid : streamerIds) {
            Streamer s = streamerMapper.selectById(sid);
            if (s != null) {
                streamerMap.put(sid, s);
            }
        }

        List<RecordingListVO> items = new ArrayList<>();
        for (Recording r : result.getRecords()) {
            RecordingListVO vo = RecordingListVO.fromEntity(r);
            vo.setLatestTaskId(latestTaskMap.get(r.getId()));
            Streamer s = r.getStreamerId() != null ? streamerMap.get(r.getStreamerId()) : null;
            if (s != null) {
                vo.setAnchorName(s.getAnchorName());
                vo.setAnchorAvatar(s.getAnchorAvatar());
            }
            items.add(vo);
        }

        return PageResult.of(items, result.getTotal(), page, size);
    }

    @DataPermission
    public RecordingVO detail(Long id) {
        Recording recording = recordingMapper.selectById(id);
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkOwnership(recording);

        RecordingVO vo = RecordingVO.fromEntity(recording);

        // Attach streamer info if available
        if (recording.getStreamerId() != null) {
            Streamer streamer = streamerMapper.selectById(recording.getStreamerId());
            if (streamer != null) {
                RecordingVO.StreamerInfo info = new RecordingVO.StreamerInfo();
                info.setId(streamer.getId());
                info.setAnchorName(streamer.getAnchorName());
                info.setAccountType(streamer.getAccountType() != null ? streamer.getAccountType().getCode() : null);
                info.setAnchorAvatar(streamer.getAnchorAvatar());
                vo.setStreamerInfo(info);
                // 顶级冗余：与列表接口保持字段一致，避免前端模板需要分两套字段路径
                vo.setAnchorName(streamer.getAnchorName());
                vo.setAnchorAvatar(streamer.getAnchorAvatar());
            }
        }

        // 与 list 接口保持一致：补 latestTaskId，前端单场详情跳 AI 助手时直接携带 taskId
        try {
            com.dianjinshou.modules.recap.entity.AnalysisTask latest = analysisTaskMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.dianjinshou.modules.recap.entity.AnalysisTask>()
                            .eq(com.dianjinshou.modules.recap.entity.AnalysisTask::getRecordingId, recording.getId())
                            .eq(com.dianjinshou.modules.recap.entity.AnalysisTask::getType, "full")
                            .orderByDesc(com.dianjinshou.modules.recap.entity.AnalysisTask::getId)
                            .last("LIMIT 1"));
            if (latest != null) {
                vo.setLatestTaskId(latest.getId());
            }
        } catch (Exception ignore) { /* 兜底失败不阻塞 detail 主流程 */ }

        return vo;
    }

    @Transactional
    @DataPermission
    public RecordingVO rename(Long id, RenameRequest req) {
        String name = req.getName();
        if (name.codePointCount(0, name.length()) > 15) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "名称不能超过15个字符");
        }

        Recording recording = recordingMapper.selectById(id);
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkOwnership(recording);

        recording.setLocalFileName(name);
        recordingMapper.updateById(recording);

        return RecordingVO.fromEntity(recording);
    }

    @DataPermission
    public Map<String, Object> export(Long id) {
        Recording recording = recordingMapper.selectById(id);
        if (recording == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录已不存在，可能已被清理");
        }
        checkOwnership(recording);

        Map<String, Object> result = new HashMap<>();
        result.put("url", null);
        result.put("message", "导出功能开发中");
        return result;
    }

    @Transactional
    @DataPermission
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ids不能为空");
        }

        for (Long id : ids) {
            Recording recording = recordingMapper.selectById(id);
            if (recording == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "录制记录不存在: " + id);
            }
            checkOwnership(recording);
        }

        int count = 0;
        for (Long id : ids) {
            count += recordingMapper.deleteById(id);
        }

        // totalSessions / todaySessions 在 StreamerService.list/detail 返回时实时 count，这里无需维护
        return count;
    }

    private void checkOwnership(Recording recording) {
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
