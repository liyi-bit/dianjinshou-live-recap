package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.comparison.dto.CreateComparisonRequest;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.service.ComparisonService;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.storage.dto.CreateCloudComparisonRequest;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.vo.CloudComparisonSourceStatusVO;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudUploadStatusVO;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CloudSpaceService {

    private static final Logger log = LoggerFactory.getLogger(CloudSpaceService.class);
    private static final int MAX_BATCH_SIZE = 50;

    private final CloudFileService cloudFileService;
    private final ComparisonService comparisonService;
    private final ComparisonMapper comparisonMapper;
    private final RecordingMapper recordingMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final StreamerMapper streamerMapper;
    private final CloudFileMapper cloudFileMapper;

    public CloudSpaceService(CloudFileService cloudFileService,
                             ComparisonService comparisonService,
                             ComparisonMapper comparisonMapper,
                             RecordingMapper recordingMapper,
                             AnalysisTaskMapper analysisTaskMapper,
                             StreamerMapper streamerMapper,
                             CloudFileMapper cloudFileMapper) {
        this.cloudFileService = cloudFileService;
        this.comparisonService = comparisonService;
        this.comparisonMapper = comparisonMapper;
        this.recordingMapper = recordingMapper;
        this.analysisTaskMapper = analysisTaskMapper;
        this.streamerMapper = streamerMapper;
        this.cloudFileMapper = cloudFileMapper;
    }

    public Page<CloudFileVO> listByType(String fileType, int page, int size,
                                         String keyword, String sortBy) {
        String businessType;
        if ("recording".equals(fileType)) {
            businessType = "full_recap";
        } else if ("clip".equals(fileType)) {
            businessType = "clip_recap";
        } else {
            businessType = "full_comparison";
        }
        return listBusiness(businessType, page, size, keyword, null, null, null, null, null, null);
    }

    public Page<CloudFileVO> listBusiness(String businessType, int page, int size,
                                           String keyword, Long industryId, String anchorName,
                                           String uploadAccount, String accountType,
                                           String startTime, String endTime) {
        return cloudFileService.listBusinessFiles(
                businessType,
                page,
                size,
                keyword,
                industryId,
                anchorName,
                uploadAccount,
                accountType,
                startTime,
                endTime);
    }

    public Page<CloudFileVO> listFullRecaps(int page, int size, String keyword, Long industryId,
                                             String anchorName, String uploadAccount, String accountType,
                                             String startTime, String endTime) {
        return listBusiness("full_recap", page, size, keyword, industryId, anchorName, uploadAccount, accountType, startTime, endTime);
    }

    public Page<CloudFileVO> listClipRecaps(int page, int size, String keyword, Long industryId,
                                             String anchorName, String uploadAccount, String accountType,
                                             String startTime, String endTime) {
        return listBusiness("clip_recap", page, size, keyword, industryId, anchorName, uploadAccount, accountType, startTime, endTime);
    }

    public Page<CloudFileVO> listComparisons(String mode, int page, int size, String keyword,
                                              String uploadAccount, String startTime, String endTime) {
        String businessType = "clip".equals(mode) ? "clip_comparison" : "full_comparison";
        return listBusiness(businessType, page, size, keyword, null, null, uploadAccount, null, startTime, endTime);
    }

    public Page<CloudFileVO> listComparisonCandidates(String mode, int page, int size, String keyword,
                                                       Long industryId, String anchorName, String uploadAccount,
                                                       String accountType, String startTime, String endTime) {
        String businessType = "clip".equals(mode) ? "clip_recap" : "full_recap";
        return listBusiness(businessType, page, size, keyword, industryId, anchorName, uploadAccount, accountType, startTime, endTime);
    }

    public CloudComparisonSourceStatusVO comparisonSourceStatus(Long comparisonId) {
        Comparison comparison = comparisonMapper.selectById(comparisonId);
        if (comparison == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Comparison not found");
        }
        checkComparisonAccess(comparison);

        String mode = "clip".equals(comparison.getType()) ? "clip" : "full";
        CloudComparisonSourceStatusVO vo = new CloudComparisonSourceStatusVO();
        vo.setComparisonId(comparisonId);
        vo.setMode(mode);
        vo.setOptimize(buildSource("optimize", mode, comparison.getRecordingIdOptimize(), comparison.getTaskIdOptimize()));
        vo.setReference(buildSource("reference", mode, comparison.getRecordingIdReference(), comparison.getTaskIdReference()));
        return vo;
    }

    public CloudUploadStatusVO uploadStatus(String businessType,
                                             Long businessId,
                                             Long recordingId,
                                             Long clipId,
                                             Long comparisonId) {
        CloudFile file = findUploadStatusFile(businessType, businessId, recordingId, clipId, comparisonId);
        CloudUploadStatusVO vo = new CloudUploadStatusVO();
        vo.setBusinessType(businessType);
        vo.setExists(file != null);
        if (file != null) {
            vo.setFileId(file.getId());
            vo.setStatus(file.getStatus());
            vo.setMessage(duplicateMessage(file.getBusinessType(), file.getStatus()));
        }
        return vo;
    }

    private CloudComparisonSourceStatusVO.Source buildSource(String role, String mode, Long recordingId, Long taskId) {
        CloudComparisonSourceStatusVO.Source source = new CloudComparisonSourceStatusVO.Source();
        source.setRole(role);
        source.setRecordingId(recordingId);
        source.setTaskId(taskId);

        if ("clip".equals(mode)) {
            fillClipSource(source, recordingId, taskId);
        } else {
            fillFullSource(source, recordingId);
        }

        CloudFile uploaded = findSourceCloudFile(source);
        if (uploaded != null) {
            source.setCloudFileId(uploaded.getId());
            source.setCloudStatus(uploaded.getStatus());
            source.setUploaded("active".equals(uploaded.getStatus()));
            source.setUploading("queued".equals(uploaded.getStatus()) || "uploading".equals(uploaded.getStatus()));
        } else {
            source.setUploaded(false);
            source.setUploading(false);
        }
        return source;
    }

    private void fillFullSource(CloudComparisonSourceStatusVO.Source source, Long recordingId) {
        source.setBusinessType("full_recap");
        source.setBusinessId(recordingId);
        if (recordingId == null) {
            return;
        }

        Recording recording = recordingMapper.selectById(recordingId);
        if (recording == null) {
            return;
        }
        checkRecordingAccess(recording);
        source.setRecordingId(recording.getId());
        source.setLocalFilePath(recording.getLocalFilePath());
        source.setFileName(firstText(recording.getLocalFileName(), fileNameOf(recording.getLocalFilePath()), "full_recap_" + recording.getId() + ".mp4"));
        source.setRecordedAt(recording.getStartTime());
        source.setDurationSeconds(recording.getDuration());
        fillStreamerMeta(source, recording.getStreamerId());
    }

    private void fillClipSource(CloudComparisonSourceStatusVO.Source source, Long recordingId, Long taskId) {
        source.setBusinessType("clip_recap");
        source.setBusinessId(taskId);
        source.setClipId(taskId);
        if (taskId == null) {
            return;
        }

        AnalysisTask task = analysisTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        checkTaskAccess(task);
        Long resolvedRecordingId = task.getRecordingId() != null ? task.getRecordingId() : recordingId;
        source.setRecordingId(resolvedRecordingId);
        source.setLocalFilePath(task.getClipFilePath());
        source.setFileName(firstText(task.getClipFilename(), fileNameOf(task.getClipFilePath()), "clip_recap_" + task.getId() + ".mp4"));
        if (task.getClipStart() != null && task.getClipEnd() != null) {
            source.setDurationSeconds(Math.max(0, task.getClipEnd() - task.getClipStart()));
        }

        Recording recording = resolvedRecordingId != null ? recordingMapper.selectById(resolvedRecordingId) : null;
        if (recording != null) {
            checkRecordingAccess(recording);
            source.setRecordedAt(recording.getStartTime());
            fillStreamerMeta(source, recording.getStreamerId());
        } else {
            source.setRecordedAt(task.getCreatedAt());
        }
    }

    private void fillStreamerMeta(CloudComparisonSourceStatusVO.Source source, Long streamerId) {
        if (streamerId == null) {
            return;
        }
        Streamer streamer = streamerMapper.selectById(streamerId);
        if (streamer == null) {
            return;
        }
        source.setStreamerId(streamer.getId());
        source.setAnchorName(streamer.getAnchorName());
        source.setIndustryId(streamer.getIndustryId());
        source.setAccountType(streamer.getAccountType() != null ? streamer.getAccountType().name() : null);
    }

    private CloudFile findSourceCloudFile(CloudComparisonSourceStatusVO.Source source) {
        if (source.getBusinessType() == null) {
            return null;
        }
        if ("clip_recap".equals(source.getBusinessType())) {
            CloudFile byClip = selectSourceCloudFile(source.getBusinessType(), CloudFile::getClipId, source.getClipId());
            if (byClip != null) {
                return byClip;
            }
            CloudFile byBusiness = selectSourceCloudFile(source.getBusinessType(), CloudFile::getBusinessId, source.getBusinessId());
            if (byBusiness != null) {
                return byBusiness;
            }
            return selectSourceCloudFile(source.getBusinessType(), CloudFile::getRecordingId, source.getRecordingId());
        }
        CloudFile byRecording = selectSourceCloudFile(source.getBusinessType(), CloudFile::getRecordingId, source.getRecordingId());
        if (byRecording != null) {
            return byRecording;
        }
        return selectSourceCloudFile(source.getBusinessType(), CloudFile::getBusinessId, source.getBusinessId());
    }

    private CloudFile selectSourceCloudFile(String businessType, SFunction<CloudFile, ?> field, Long value) {
        if (value == null) {
            return null;
        }
        CloudFile active = cloudFileMapper.selectOne(
                new LambdaQueryWrapper<CloudFile>()
                        .eq(CloudFile::getUserId, requireCurrentUserId())
                        .eq(CloudFile::getBusinessType, businessType)
                        .eq(field, value)
                        .eq(CloudFile::getStatus, "active")
                        .orderByDesc(CloudFile::getCreatedAt)
                        .last("LIMIT 1"));
        if (active != null) {
            return active;
        }
        return cloudFileMapper.selectOne(
                new LambdaQueryWrapper<CloudFile>()
                        .eq(CloudFile::getUserId, requireCurrentUserId())
                        .eq(CloudFile::getBusinessType, businessType)
                        .eq(field, value)
                        .in(CloudFile::getStatus, "queued", "uploading")
                        .orderByDesc(CloudFile::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private CloudFile findUploadStatusFile(String businessType,
                                           Long businessId,
                                           Long recordingId,
                                           Long clipId,
                                           Long comparisonId) {
        if ("full_recap".equals(businessType)) {
            CloudFile byRecording = selectSourceCloudFile(businessType, CloudFile::getRecordingId, firstId(recordingId, businessId));
            if (byRecording != null) {
                return byRecording;
            }
            return selectSourceCloudFile(businessType, CloudFile::getBusinessId, businessId);
        }
        if ("clip_recap".equals(businessType)) {
            CloudFile byClip = selectSourceCloudFile(businessType, CloudFile::getClipId, firstId(clipId, businessId));
            if (byClip != null) {
                return byClip;
            }
            return selectSourceCloudFile(businessType, CloudFile::getBusinessId, businessId);
        }
        if ("full_comparison".equals(businessType) || "clip_comparison".equals(businessType)) {
            CloudFile byComparison = selectSourceCloudFile(businessType, CloudFile::getComparisonId, firstId(comparisonId, businessId));
            if (byComparison != null) {
                return byComparison;
            }
            return selectSourceCloudFile(businessType, CloudFile::getBusinessId, businessId);
        }
        return null;
    }

    private String duplicateMessage(String businessType, String status) {
        String subject = "clip_recap".equals(businessType)
                ? "该切片复盘"
                : ("full_comparison".equals(businessType) || "clip_comparison".equals(businessType)
                ? "该对比复盘"
                : "该全场复盘");
        if ("active".equals(status)) {
            return subject + "已在云空间，无需重复上传";
        }
        return subject + "已在上传队列，无需重复添加";
    }

    private Long firstId(Long first, Long second) {
        return first != null ? first : second;
    }

    private void checkComparisonAccess(Comparison comparison) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long orgId = SecurityContextHelper.currentOrgId();
        if (orgId == null || !orgId.equals(comparison.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "No access to this comparison");
        }
    }

    private void checkRecordingAccess(Recording recording) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long orgId = SecurityContextHelper.currentOrgId();
        if (orgId == null || !orgId.equals(recording.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "No access to this recording");
        }
    }

    private void checkTaskAccess(AnalysisTask task) {
        String role = SecurityContextHelper.currentRole();
        if ("super_admin".equals(role)) {
            return;
        }
        Long orgId = SecurityContextHelper.currentOrgId();
        if (orgId == null || !orgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "No access to this analysis task");
        }
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String firstText(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return fallback;
    }

    private String fileNameOf(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        int index = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return index >= 0 ? path.substring(index + 1) : path;
    }

    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要删除的文件");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次最多删除" + MAX_BATCH_SIZE + "个文件");
        }

        for (Long id : ids) {
            cloudFileService.deleteFile(id);
        }
        log.info("Batch deleted {} cloud files", ids.size());
    }

    public List<String> batchDownloadUrls(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要下载的文件");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次最多下载" + MAX_BATCH_SIZE + "个文件");
        }

        List<String> urls = new ArrayList<String>();
        for (Long id : ids) {
            urls.add(cloudFileService.getDownloadUrl(id));
        }
        return urls;
    }

    public ComparisonVO createComparison(CreateCloudComparisonRequest request) {
        if (request.getFileIds() == null || request.getFileIds().size() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择两条数据进行对比");
        }
        String expectedBusinessType = "clip".equals(request.getMode()) ? "clip_recap" : "full_recap";

        CloudFile first = cloudFileService.getOwnedFile(request.getFileIds().get(0));
        CloudFile second = cloudFileService.getOwnedFile(request.getFileIds().get(1));
        validateComparisonCandidate(first, expectedBusinessType);
        validateComparisonCandidate(second, expectedBusinessType);

        if (first.getRecordingId() == null || second.getRecordingId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "云端记录缺少本地录制ID，无法创建对比");
        }

        CreateComparisonRequest createRequest = new CreateComparisonRequest();
        createRequest.setRecordingIdOptimize(first.getRecordingId());
        createRequest.setRecordingIdReference(second.getRecordingId());
        createRequest.setType("clip".equals(request.getMode()) ? "clip" : "full");
        ComparisonVO comparison = comparisonService.create(createRequest);
        cloudFileService.createComparisonRecord(comparison.getId(), request.getMode(), first, second);
        return comparison;
    }

    private void validateComparisonCandidate(CloudFile file, String expectedBusinessType) {
        if (!expectedBusinessType.equals(file.getBusinessType())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "请选择同类型数据进行对比");
        }
        if (!"active".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "文件尚未上传完成，不能加入对比");
        }
    }
}
