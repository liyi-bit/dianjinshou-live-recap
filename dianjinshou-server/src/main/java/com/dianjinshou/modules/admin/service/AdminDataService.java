package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.vo.AdminRecordingDetailVO;
import com.dianjinshou.modules.admin.vo.AdminRecordingStatsVO;
import com.dianjinshou.modules.admin.vo.AdminRecordingVO;
import com.dianjinshou.modules.admin.vo.AdminTaskDetailVO;
import com.dianjinshou.modules.admin.vo.AdminTaskStatsVO;
import com.dianjinshou.modules.admin.vo.AdminTaskVO;
import com.dianjinshou.modules.admin.vo.AdminUserDetailVO;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.fileanalysis.entity.FileAnalysisTask;
import com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.storage.entity.UploadTask;
import com.dianjinshou.modules.storage.mapper.UploadTaskMapper;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDataService {

    private final RecordingMapper recordingMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final FileAnalysisTaskMapper fileAnalysisTaskMapper;
    private final UploadTaskMapper uploadTaskMapper;
    private final UserMapper userMapper;
    private final StreamerMapper streamerMapper;

    public AdminDataService(RecordingMapper recordingMapper,
                            AnalysisTaskMapper analysisTaskMapper,
                            FileAnalysisTaskMapper fileAnalysisTaskMapper,
                            UploadTaskMapper uploadTaskMapper,
                            UserMapper userMapper,
                            StreamerMapper streamerMapper) {
        this.recordingMapper = recordingMapper;
        this.analysisTaskMapper = analysisTaskMapper;
        this.fileAnalysisTaskMapper = fileAnalysisTaskMapper;
        this.uploadTaskMapper = uploadTaskMapper;
        this.userMapper = userMapper;
        this.streamerMapper = streamerMapper;
    }

    // ========== Users ==========

    public AdminUserDetailVO userDetail(Long id) {
        requireAdmin();
        User u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        return AdminUserDetailVO.fromEntity(u);
    }

    public Map<String, Long> userRelatedCounts(Long userId) {
        requireAdmin();
        Map<String, Long> m = new HashMap<>();
        m.put("recordingCount", recordingMapper.selectCount(
                new LambdaQueryWrapper<Recording>().eq(Recording::getUserId, userId)));
        m.put("analysisTaskCount", analysisTaskMapper.selectCount(
                new LambdaQueryWrapper<AnalysisTask>().eq(AnalysisTask::getUserId, userId)));
        m.put("fileAnalysisCount", fileAnalysisTaskMapper.selectCount(
                new LambdaQueryWrapper<FileAnalysisTask>().eq(FileAnalysisTask::getUserId, userId)));
        m.put("uploadCount", uploadTaskMapper.selectCount(
                new LambdaQueryWrapper<UploadTask>().eq(UploadTask::getUserId, userId)));
        return m;
    }

    // ========== Recordings ==========

    public PageResult<AdminRecordingVO> listRecordings(int page, int size,
                                                       Long userId,
                                                       String userPhone,
                                                       String analysisStatus,
                                                       LocalDateTime start,
                                                       LocalDateTime end) {
        requireAdmin();
        LambdaQueryWrapper<Recording> wrapper = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(wrapper, Recording::getOrgId);

        // 如提供 userPhone，先把 phone → userId 列表
        if (userPhone != null && !userPhone.isEmpty()) {
            List<Long> phoneIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>().like(User::getPhone, userPhone).select(User::getId))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (phoneIds.isEmpty()) return PageResult.of(Collections.emptyList(), 0, page, size);
            wrapper.in(Recording::getUserId, phoneIds);
        }

        if (userId != null) wrapper.eq(Recording::getUserId, userId);
        if (analysisStatus != null && !analysisStatus.isEmpty()) wrapper.eq(Recording::getAnalysisStatus, analysisStatus);
        if (start != null) wrapper.ge(Recording::getStartTime, start);
        if (end != null) wrapper.le(Recording::getStartTime, end);
        wrapper.orderByDesc(Recording::getCreatedAt);

        Page<Recording> pageObj = recordingMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量载入 User（username+phone）
        Set<Long> userIds = pageObj.getRecords().stream().map(Recording::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUsers(userIds);

        // 批量载入 Streamer
        Set<Long> streamerIds = pageObj.getRecords().stream()
                .map(Recording::getStreamerId).filter(x -> x != null).collect(Collectors.toSet());
        Map<Long, Streamer> streamerMap = loadStreamers(streamerIds);

        List<AdminRecordingVO> items = pageObj.getRecords().stream()
                .map(r -> {
                    User u = userMap.get(r.getUserId());
                    Streamer s = r.getStreamerId() != null ? streamerMap.get(r.getStreamerId()) : null;
                    return AdminRecordingVO.fromEntity(
                            r,
                            u != null ? u.getUsername() : null,
                            u != null ? u.getPhone() : null,
                            s != null ? s.getAnchorName() : null,
                            s != null ? s.getAnchorAvatar() : null
                    );
                })
                .collect(Collectors.toList());
        return PageResult.of(items, pageObj.getTotal(), page, size);
    }

    private Map<Long, User> loadUsers(Set<Long> ids) {
        Set<Long> clean = new HashSet<>();
        for (Long id : ids) if (id != null) clean.add(id);
        if (clean.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(clean).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, Streamer> loadStreamers(Set<Long> ids) {
        Set<Long> clean = new HashSet<>();
        for (Long id : ids) if (id != null) clean.add(id);
        if (clean.isEmpty()) return Collections.emptyMap();
        return streamerMapper.selectBatchIds(clean).stream()
                .collect(Collectors.toMap(Streamer::getId, s -> s, (a, b) -> a));
    }

    public AdminRecordingStatsVO recordingStats() {
        requireAdmin();
        AdminRecordingStatsVO vo = new AdminRecordingStatsVO();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();

        vo.setTotal(countRecording(null, null, null, null));
        vo.setTodayCount(countRecording(null, null, todayStart, null));
        vo.setWeekCount(countRecording(null, null, weekStart, null));
        vo.setCompleted(countRecording(null, "COMPLETED", null, null));
        vo.setFailed(countRecording(null, "FAILED", null, null));
        vo.setRecording(countRecording(null, "RECORDING", null, null));

        vo.setAnalyzedDone(analyzedCount("COMPLETED"));
        vo.setAnalyzedPending(analyzedCount("PENDING"));

        QueryWrapper<Recording> sumWrapper = new QueryWrapper<>();
        sumWrapper.select("COALESCE(SUM(duration),0) AS sumDur, COALESCE(SUM(file_size),0) AS sumSize");
        SecurityUser cur = SecurityContextHelper.currentUser();
        boolean crossOrg = "super_admin".equals(cur.getRole())
                || (cur.getRole() != null && cur.getRole().startsWith("admin_"));
        if (!crossOrg) {
            if (cur.getOrgId() == null) {
                throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "用户未关联组织");
            }
            sumWrapper.eq("org_id", cur.getOrgId());
        }
        List<Map<String, Object>> rows = recordingMapper.selectMaps(sumWrapper);
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            vo.setTotalDurationSec(toLong(r.get("sumDur")));
            vo.setTotalFileBytes(toLong(r.get("sumSize")));
        }
        return vo;
    }

    public AdminRecordingDetailVO recordingDetail(Long id) {
        requireAdmin();
        Recording r = recordingMapper.selectById(id);
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND, "录制不存在");
        checkOrgAccess(r.getOrgId());

        String username = resolveUsername(r.getUserId());

        LambdaQueryWrapper<AnalysisTask> tw = new LambdaQueryWrapper<>();
        tw.eq(AnalysisTask::getRecordingId, id).orderByDesc(AnalysisTask::getCreatedAt);
        List<AdminTaskVO> tasks = analysisTaskMapper.selectList(tw).stream()
                .map(this::mapAnalysisTask).collect(Collectors.toList());
        tasks.forEach(t -> t.setUsername(username));

        return AdminRecordingDetailVO.fromEntity(r, username, tasks);
    }

    // ========== Tasks ==========

    public PageResult<AdminTaskVO> listTasks(int page, int size,
                                             String taskType,
                                             Long userId,
                                             String userPhone,
                                             String status,
                                             LocalDateTime start,
                                             LocalDateTime end) {
        requireAdmin();

        // 将 userPhone 解析为 userId 列表（作为额外过滤）
        List<Long> phoneUserIds = null;
        if (userPhone != null && !userPhone.isEmpty()) {
            phoneUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>().like(User::getPhone, userPhone).select(User::getId))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (phoneUserIds.isEmpty()) return PageResult.of(Collections.emptyList(), 0, page, size);
        }

        List<AdminTaskVO> merged = new ArrayList<>();
        boolean wantAnalysis = taskType == null || taskType.isEmpty() || "analysis".equalsIgnoreCase(taskType);
        boolean wantFile = taskType == null || taskType.isEmpty() || "file_analysis".equalsIgnoreCase(taskType);
        boolean wantUpload = taskType == null || taskType.isEmpty() || "upload".equalsIgnoreCase(taskType);

        if (wantAnalysis) merged.addAll(queryAnalysisTasks(userId, phoneUserIds, status, start, end));
        if (wantFile) merged.addAll(queryFileAnalysisTasks(userId, phoneUserIds, status, start, end));
        if (wantUpload) merged.addAll(queryUploadTasks(userId, phoneUserIds, status, start, end));

        merged.sort(Comparator.comparing(AdminTaskVO::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long total = merged.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(merged.size(), from + size);
        List<AdminTaskVO> pageItems = from >= merged.size() ? Collections.emptyList() : merged.subList(from, to);

        // 批量回填 user 和 streamer 信息
        Set<Long> userIds = pageItems.stream().map(AdminTaskVO::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = loadUsers(userIds);

        // analysis 任务的 streamerId 通过 recordingId 查 recording 再查 streamer
        Set<Long> recordingIds = pageItems.stream()
                .filter(v -> "analysis".equals(v.getTaskType()) && v.getResource() != null && v.getResource().startsWith("recording#"))
                .map(v -> {
                    try { return Long.parseLong(v.getResource().substring("recording#".length())); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(x -> x != null).collect(Collectors.toSet());
        Map<Long, Recording> recordingMap = loadRecordings(recordingIds);
        Set<Long> streamerIds = recordingMap.values().stream()
                .map(Recording::getStreamerId).filter(x -> x != null).collect(Collectors.toSet());
        Map<Long, Streamer> streamerMap = loadStreamers(streamerIds);

        for (AdminTaskVO vo : pageItems) {
            User u = userMap.get(vo.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setUserPhone(u.getPhone());
            }
            if ("analysis".equals(vo.getTaskType()) && vo.getResource() != null
                    && vo.getResource().startsWith("recording#")) {
                try {
                    Long rid = Long.parseLong(vo.getResource().substring("recording#".length()));
                    Recording r = recordingMap.get(rid);
                    if (r != null && r.getStreamerId() != null) {
                        Streamer s = streamerMap.get(r.getStreamerId());
                        if (s != null) {
                            vo.setStreamerId(s.getId());
                            vo.setStreamerName(s.getAnchorName());
                            vo.setStreamerAvatar(s.getAnchorAvatar());
                        }
                    }
                } catch (NumberFormatException ignored) { }
            }
        }

        return PageResult.of(new ArrayList<>(pageItems), total, page, size);
    }

    private Map<Long, Recording> loadRecordings(Set<Long> ids) {
        Set<Long> clean = new HashSet<>();
        for (Long id : ids) if (id != null) clean.add(id);
        if (clean.isEmpty()) return Collections.emptyMap();
        return recordingMapper.selectBatchIds(clean).stream()
                .collect(Collectors.toMap(Recording::getId, r -> r, (a, b) -> a));
    }

    public AdminTaskStatsVO taskStats() {
        requireAdmin();
        AdminTaskStatsVO vo = new AdminTaskStatsVO();

        long analysisTotal = analysisTaskMapper.selectCount(scopedAnalysisWrapper());
        long fileTotal = fileAnalysisTaskMapper.selectCount(scopedFileAnalysisWrapper());
        long uploadTotal = uploadTaskMapper.selectCount(scopedUploadWrapper());

        Map<String, Long> byType = new HashMap<>();
        byType.put("analysis", analysisTotal);
        byType.put("file_analysis", fileTotal);
        byType.put("upload", uploadTotal);
        vo.setByType(byType);

        Map<String, Long> byStatus = new HashMap<>();
        statusAgg(byStatus, analysisTaskMapper.selectList(scopedAnalysisWrapper()),
                AnalysisTask::getStatus);
        statusAgg(byStatus, fileAnalysisTaskMapper.selectList(scopedFileAnalysisWrapper()),
                FileAnalysisTask::getStatus);
        statusAgg(byStatus, uploadTaskMapper.selectList(scopedUploadWrapper()),
                UploadTask::getStatus);
        vo.setByStatus(byStatus);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long analysisToday = analysisTaskMapper.selectCount(
                scopedAnalysisWrapper().ge(AnalysisTask::getCreatedAt, todayStart));
        long fileToday = fileAnalysisTaskMapper.selectCount(
                scopedFileAnalysisWrapper().ge(FileAnalysisTask::getCreatedAt, todayStart));
        long uploadToday = uploadTaskMapper.selectCount(
                scopedUploadWrapper().ge(UploadTask::getCreatedAt, todayStart));
        vo.setTodayCount(analysisToday + fileToday + uploadToday);

        vo.setFailedCount(sumByStatus(byStatus, "FAILED", "failed"));
        vo.setPendingCount(sumByStatus(byStatus, "PENDING", "pending"));
        vo.setCompletedCount(sumByStatus(byStatus, "COMPLETED", "completed"));
        return vo;
    }

    public AdminTaskDetailVO taskDetail(String taskType, Long id) {
        requireAdmin();
        if ("analysis".equalsIgnoreCase(taskType)) {
            AnalysisTask t = analysisTaskMapper.selectById(id);
            if (t == null) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
            checkOrgAccess(t.getOrgId());
            return buildAnalysisDetail(t);
        }
        if ("file_analysis".equalsIgnoreCase(taskType)) {
            FileAnalysisTask t = fileAnalysisTaskMapper.selectById(id);
            if (t == null) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
            checkOrgAccess(t.getOrgId());
            return buildFileAnalysisDetail(t);
        }
        if ("upload".equalsIgnoreCase(taskType)) {
            UploadTask t = uploadTaskMapper.selectById(id);
            if (t == null) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
            checkOrgAccess(t.getOrgId());
            return buildUploadDetail(t);
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "未知任务类型");
    }

    // ========== Task query helpers ==========

    private List<AdminTaskVO> queryAnalysisTasks(Long userId, List<Long> phoneUserIds, String status,
                                                 LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<AnalysisTask> w = scopedAnalysisWrapper();
        if (userId != null) w.eq(AnalysisTask::getUserId, userId);
        if (phoneUserIds != null) w.in(AnalysisTask::getUserId, phoneUserIds);
        if (status != null && !status.isEmpty()) w.eq(AnalysisTask::getStatus, status);
        if (start != null) w.ge(AnalysisTask::getCreatedAt, start);
        if (end != null) w.le(AnalysisTask::getCreatedAt, end);
        w.orderByDesc(AnalysisTask::getCreatedAt);
        w.last("limit 500");
        return analysisTaskMapper.selectList(w).stream().map(this::mapAnalysisTask).collect(Collectors.toList());
    }

    private List<AdminTaskVO> queryFileAnalysisTasks(Long userId, List<Long> phoneUserIds, String status,
                                                     LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<FileAnalysisTask> w = scopedFileAnalysisWrapper();
        if (userId != null) w.eq(FileAnalysisTask::getUserId, userId);
        if (phoneUserIds != null) w.in(FileAnalysisTask::getUserId, phoneUserIds);
        if (status != null && !status.isEmpty()) w.eq(FileAnalysisTask::getStatus, status);
        if (start != null) w.ge(FileAnalysisTask::getCreatedAt, start);
        if (end != null) w.le(FileAnalysisTask::getCreatedAt, end);
        w.orderByDesc(FileAnalysisTask::getCreatedAt);
        w.last("limit 500");
        return fileAnalysisTaskMapper.selectList(w).stream().map(this::mapFileAnalysisTask).collect(Collectors.toList());
    }

    private List<AdminTaskVO> queryUploadTasks(Long userId, List<Long> phoneUserIds, String status,
                                               LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<UploadTask> w = scopedUploadWrapper();
        if (userId != null) w.eq(UploadTask::getUserId, userId);
        if (phoneUserIds != null) w.in(UploadTask::getUserId, phoneUserIds);
        if (status != null && !status.isEmpty()) w.eq(UploadTask::getStatus, status);
        if (start != null) w.ge(UploadTask::getCreatedAt, start);
        if (end != null) w.le(UploadTask::getCreatedAt, end);
        w.orderByDesc(UploadTask::getCreatedAt);
        w.last("limit 500");
        return uploadTaskMapper.selectList(w).stream().map(this::mapUploadTask).collect(Collectors.toList());
    }

    private AdminTaskVO mapAnalysisTask(AnalysisTask t) {
        AdminTaskVO vo = new AdminTaskVO();
        vo.setId(t.getId());
        vo.setTaskType("analysis");
        vo.setUserId(t.getUserId());
        vo.setOrgId(t.getOrgId());
        vo.setSubType(t.getType());
        vo.setStatus(t.getStatus());
        vo.setAiModel(t.getAiModel());
        vo.setErrorMsg(t.getErrorMsg());
        vo.setResource(t.getRecordingId() != null ? "recording#" + t.getRecordingId() : null);
        vo.setStartedAt(t.getStartedAt());
        vo.setCompletedAt(t.getCompletedAt());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }

    private AdminTaskVO mapFileAnalysisTask(FileAnalysisTask t) {
        AdminTaskVO vo = new AdminTaskVO();
        vo.setId(t.getId());
        vo.setTaskType("file_analysis");
        vo.setUserId(t.getUserId());
        vo.setOrgId(t.getOrgId());
        vo.setStatus(t.getStatus());
        vo.setAiModel(t.getAiModel());
        vo.setErrorMsg(t.getErrorMsg());
        vo.setResource(t.getFileName());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }

    private AdminTaskVO mapUploadTask(UploadTask t) {
        AdminTaskVO vo = new AdminTaskVO();
        vo.setId(t.getId());
        vo.setTaskType("upload");
        vo.setUserId(t.getUserId());
        vo.setOrgId(t.getOrgId());
        vo.setStatus(t.getStatus());
        String progress = "";
        if (t.getTotalParts() != null && t.getUploadedParts() != null) {
            progress = " (" + t.getUploadedParts() + "/" + t.getTotalParts() + ")";
        }
        vo.setResource(t.getFileName() + progress);
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }

    // ========== Detail builders ==========

    private AdminTaskDetailVO buildAnalysisDetail(AnalysisTask t) {
        AdminTaskDetailVO vo = new AdminTaskDetailVO();
        vo.setId(t.getId());
        vo.setTaskType("analysis");
        vo.setUserId(t.getUserId());
        vo.setUsername(resolveUsername(t.getUserId()));
        vo.setOrgId(t.getOrgId());
        vo.setRecordingId(t.getRecordingId());
        if (t.getRecordingId() != null) {
            Recording r = recordingMapper.selectById(t.getRecordingId());
            if (r != null) vo.setRecordingName(r.getLocalFileName());
        }
        vo.setSubType(t.getType());
        vo.setStatus(t.getStatus());
        vo.setPriority(t.getPriority());
        vo.setAiModel(t.getAiModel());
        vo.setIndustry(t.getIndustry());
        vo.setAsrText(t.getAsrText());
        vo.setAsrWordCount(t.getAsrWordCount());
        vo.setAiResult(t.getAiResult());
        vo.setAiDiagnosis(t.getAiDiagnosis());
        vo.setKeywordSummary(t.getKeywordSummary());
        vo.setSensitiveWords(t.getSensitiveWords());
        vo.setSensitiveCount(t.getSensitiveCount());
        vo.setContentCompass(t.getContentCompass());
        vo.setOptimizedText(t.getOptimizedText());
        vo.setOptimizationAction(t.getOptimizationAction());
        vo.setOptimizationGoal(t.getOptimizationGoal());
        vo.setSummary(t.getSummary());
        vo.setConsumedChars(t.getConsumedChars());
        vo.setErrorMsg(t.getErrorMsg());
        vo.setStartedAt(t.getStartedAt());
        vo.setCompletedAt(t.getCompletedAt());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setUpdatedAt(t.getUpdatedAt());
        return vo;
    }

    private AdminTaskDetailVO buildFileAnalysisDetail(FileAnalysisTask t) {
        AdminTaskDetailVO vo = new AdminTaskDetailVO();
        vo.setId(t.getId());
        vo.setTaskType("file_analysis");
        vo.setUserId(t.getUserId());
        vo.setUsername(resolveUsername(t.getUserId()));
        vo.setOrgId(t.getOrgId());
        vo.setStatus(t.getStatus());
        vo.setAiModel(t.getAiModel());
        vo.setFileName(t.getFileName());
        vo.setFileSize(t.getFileSize());
        vo.setDuration(t.getDuration());
        vo.setStorageKey(t.getStorageKey());
        vo.setAsrText(t.getAsrText());
        vo.setErrorMsg(t.getErrorMsg());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setUpdatedAt(t.getUpdatedAt());
        return vo;
    }

    private AdminTaskDetailVO buildUploadDetail(UploadTask t) {
        AdminTaskDetailVO vo = new AdminTaskDetailVO();
        vo.setId(t.getId());
        vo.setTaskType("upload");
        vo.setUserId(t.getUserId());
        vo.setUsername(resolveUsername(t.getUserId()));
        vo.setOrgId(t.getOrgId());
        vo.setStatus(t.getStatus());
        vo.setFileName(t.getFileName());
        vo.setFileSize(t.getFileSize());
        vo.setStorageKey(t.getStorageKey());
        vo.setTotalParts(t.getTotalParts());
        vo.setUploadedParts(t.getUploadedParts());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }

    // ========== Small helpers ==========

    private LambdaQueryWrapper<AnalysisTask> scopedAnalysisWrapper() {
        LambdaQueryWrapper<AnalysisTask> w = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(w, AnalysisTask::getOrgId);
        return w;
    }

    private LambdaQueryWrapper<FileAnalysisTask> scopedFileAnalysisWrapper() {
        LambdaQueryWrapper<FileAnalysisTask> w = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(w, FileAnalysisTask::getOrgId);
        return w;
    }

    private LambdaQueryWrapper<UploadTask> scopedUploadWrapper() {
        LambdaQueryWrapper<UploadTask> w = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(w, UploadTask::getOrgId);
        return w;
    }

    private long countRecording(Long userId, String status, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Recording> w = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(w, Recording::getOrgId);
        if (userId != null) w.eq(Recording::getUserId, userId);
        if (status != null) w.eq(Recording::getStatus, status);
        if (start != null) w.ge(Recording::getCreatedAt, start);
        if (end != null) w.le(Recording::getCreatedAt, end);
        return recordingMapper.selectCount(w);
    }

    private long analyzedCount(String analysisStatus) {
        LambdaQueryWrapper<Recording> w = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(w, Recording::getOrgId);
        w.eq(Recording::getAnalysisStatus, analysisStatus);
        return recordingMapper.selectCount(w);
    }

    private <T> void statusAgg(Map<String, Long> acc, List<T> rows, java.util.function.Function<T, String> getter) {
        for (T row : rows) {
            String s = getter.apply(row);
            if (s == null) continue;
            acc.merge(s, 1L, Long::sum);
        }
    }

    private long sumByStatus(Map<String, Long> byStatus, String... keys) {
        long sum = 0;
        for (String k : keys) sum += byStatus.getOrDefault(k, 0L);
        return sum;
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    private Map<Long, String> loadUsernames(Set<Long> ids) {
        Set<Long> clean = new HashSet<>();
        for (Long id : ids) if (id != null) clean.add(id);
        if (clean.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(clean).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
    }

    private String resolveUsername(Long userId) {
        if (userId == null) return null;
        User u = userMapper.selectById(userId);
        return u != null ? u.getUsername() : null;
    }

    private void requireAdmin() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        String role = user.getRole();
        if (role == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此操作仅限管理员");
        }
        if (role.startsWith("admin_")) return;
        if (!"super_admin".equals(role) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此操作仅限管理员");
        }
    }

    private void checkOrgAccess(Long targetOrgId) {
        SecurityUser user = SecurityContextHelper.currentUser();
        if ("super_admin".equals(user.getRole())) return;
        if (user.getRole() != null && user.getRole().startsWith("admin_")) return;
        if (targetOrgId == null || !targetOrgId.equals(user.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
