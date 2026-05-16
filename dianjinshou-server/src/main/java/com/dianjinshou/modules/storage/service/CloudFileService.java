package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.storage.CosProperties;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.storage.dto.RenameCloudFileRequest;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.CloudUsageVO;
import com.dianjinshou.modules.storage.vo.OpenTargetVO;
import com.dianjinshou.modules.storage.vo.SignedUrlVO;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CloudFileService {

    private static final Logger log = LoggerFactory.getLogger(CloudFileService.class);
    private static final long DEFAULT_QUOTA_BYTES = 20L * 1024 * 1024 * 1024;

    private final CloudFileMapper cloudFileMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final CosCredentialService cosCredentialService;
    private final CosProperties cosProperties;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final StreamerMapper streamerMapper;
    private final ComparisonMapper comparisonMapper;
    private final RecordingMapper recordingMapper;
    private final UserMapper userMapper;

    public CloudFileService(CloudFileMapper cloudFileMapper,
                            StorageService storageService,
                            StorageProperties storageProperties,
                            CosCredentialService cosCredentialService,
                            CosProperties cosProperties,
                            AnalysisTaskMapper analysisTaskMapper,
                            StreamerMapper streamerMapper,
                            ComparisonMapper comparisonMapper,
                            RecordingMapper recordingMapper,
                            UserMapper userMapper) {
        this.cloudFileMapper = cloudFileMapper;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.cosCredentialService = cosCredentialService;
        this.cosProperties = cosProperties;
        this.analysisTaskMapper = analysisTaskMapper;
        this.streamerMapper = streamerMapper;
        this.comparisonMapper = comparisonMapper;
        this.recordingMapper = recordingMapper;
        this.userMapper = userMapper;
    }

    public Page<CloudFileVO> listFiles(int page, int size, String fileType, String keyword) {
        Long userId = requireCurrentUserId();
        LambdaQueryWrapper<CloudFile> query = new LambdaQueryWrapper<CloudFile>()
                .eq(CloudFile::getUserId, userId)
                .eq(CloudFile::getStatus, "active");

        if (fileType != null && !fileType.trim().isEmpty()) {
            query.eq(CloudFile::getFileType, fileType.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(w -> w.like(CloudFile::getDisplayName, keyword.trim())
                    .or()
                    .like(CloudFile::getFileName, keyword.trim()));
        }
        query.orderByDesc(CloudFile::getCreatedAt);
        return toVOPage(cloudFileMapper.selectPage(new Page<CloudFile>(page, size), query));
    }

    public Page<CloudFileVO> listBusinessFiles(String businessType, int page, int size,
                                                String keyword, Long industryId, String anchorName,
                                                String uploadAccount, String accountType,
                                                String startTime, String endTime) {
        Long userId = requireCurrentUserId();
        LambdaQueryWrapper<CloudFile> query = new LambdaQueryWrapper<CloudFile>()
                .eq(CloudFile::getUserId, userId)
                .eq(CloudFile::getBusinessType, businessType)
                .in(CloudFile::getStatus, "queued", "uploading", "active");

        if (keyword != null && !keyword.trim().isEmpty()) {
            String value = keyword.trim();
            query.and(w -> w.like(CloudFile::getDisplayName, value)
                    .or()
                    .like(CloudFile::getFileName, value)
                    .or()
                    .like(CloudFile::getAnchorName, value));
        }
        if (industryId != null) {
            query.eq(CloudFile::getIndustryId, industryId);
        }
        if (anchorName != null && !anchorName.trim().isEmpty()) {
            query.like(CloudFile::getAnchorName, anchorName.trim());
        }
        if (uploadAccount != null && !uploadAccount.trim().isEmpty()) {
            query.like(CloudFile::getUploadAccount, uploadAccount.trim());
        }
        if (accountType != null && !accountType.trim().isEmpty()) {
            query.eq(CloudFile::getAccountType, accountType.trim());
        }
        if (startTime != null && !startTime.trim().isEmpty()) {
            query.ge(CloudFile::getRecordedAt, startTime.trim());
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            query.le(CloudFile::getRecordedAt, endTime.trim());
        }

        query.orderByDesc(CloudFile::getRecordedAt).orderByDesc(CloudFile::getCreatedAt);
        return toVOPage(cloudFileMapper.selectPage(new Page<CloudFile>(page, size), query));
    }

    public void deleteFile(Long fileId) {
        CloudFile file = getOwnedFile(fileId);
        // 先删 MinIO，失败抛业务异常；DB 状态在对象存储删除成功后才更新
        deleteStorageObject(file);
        file.setStatus("deleted");
        cloudFileMapper.updateById(file);
        log.info("Cloud file deleted: id={}, key={}", fileId, file.getStorageKey());
    }

    public CloudFileVO rename(Long fileId, RenameCloudFileRequest request) {
        CloudFile file = getOwnedFile(fileId);
        file.setDisplayName(request.getDisplayName().trim());
        cloudFileMapper.updateById(file);
        return CloudFileVO.fromEntity(file);
    }

    public String getDownloadUrl(Long fileId) {
        CloudFile file = getOwnedFile(fileId);
        if (cosCredentialService.isConfigured() && hasText(file.getStorageKey())) {
            return cosCredentialService.createSignedReadUrl(file.getStorageKey());
        }
        return storageService.getPresignedUrl(file.getBucket(), file.getStorageKey(), storageProperties.getPresignedUrlExpireSeconds());
    }

    public SignedUrlVO signedUrl(Long fileId) {
        CloudFile file = getOwnedFile(fileId);
        if (!"active".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "文件尚未上传完成");
        }
        return new SignedUrlVO(getDownloadUrl(fileId), "GET", LocalDateTime.now().plusHours(cosCredentialService.getReadUrlExpireHours()));
    }

    public OpenTargetVO openTarget(Long fileId) {
        CloudFile file = getOwnedFile(fileId);
        Long recapTaskId = resolveAnalysisTaskId(file);
        boolean comparison = isComparison(file.getBusinessType());
        boolean local = Boolean.TRUE.equals(file.getLocalExists()) && (comparison || recapTaskId != null);
        OpenTargetVO vo = new OpenTargetVO();
        vo.setTarget(local ? "local" : "cloud_readonly");
        vo.setReadonly(!local);
        vo.getParams().put("fileId", file.getId());
        vo.getParams().put("businessType", file.getBusinessType());
        vo.getParams().put("businessId", recapTaskId != null ? recapTaskId : file.getBusinessId());
        vo.getParams().put("recordingId", file.getRecordingId());
        vo.getParams().put("clipId", file.getClipId());
        vo.getParams().put("comparisonId", file.getComparisonId());
        if (comparison) {
            vo.setRouteName(local ? "ComparisonDetail" : "CloudReadonlyDetail");
        } else {
            vo.setRouteName(local ? "RecapDetail" : "CloudReadonlyDetail");
        }
        return vo;
    }

    public CloudUsageVO getUsage(int vipLevel) {
        return getUsage();
    }

    public CloudUsageVO getUsage() {
        Long userId = requireCurrentUserId();
        long usedBytes = cloudFileMapper.sumUsedBytesByUser(userId);
        long fileCount = cloudFileMapper.countActiveByUser(userId);

        long quotaBytes = cosProperties.getQuotaBytes() > 0 ? cosProperties.getQuotaBytes() : DEFAULT_QUOTA_BYTES;
        CloudUsageVO vo = new CloudUsageVO();
        vo.setUsedBytes(usedBytes);
        vo.setTotalQuotaBytes(quotaBytes);
        vo.setRemainingBytes(Math.max(0, quotaBytes - usedBytes));
        vo.setFileCount((int) fileCount);
        vo.setUsagePercent(quotaBytes > 0 ? (double) usedBytes / quotaBytes * 100 : 0);
        return vo;
    }

    public void checkQuota(Long orgId, int vipLevel, long additionalBytes) {
        checkCurrentUserQuota(additionalBytes);
    }

    public void checkCurrentUserQuota(long additionalBytes) {
        CloudUsageVO usage = getUsage();
        if (usage.getUsedBytes() + additionalBytes > usage.getTotalQuotaBytes()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "云空间容量不足");
        }
    }

    public long getCurrentUserUsedBytes() {
        return getUsage().getUsedBytes();
    }

    public CloudFile createCloudFile(Long userId, Long orgId, String fileName, String storageKey,
                                      String bucket, long fileSize, String contentType, String fileType, Long sourceId) {
        CloudFile file = new CloudFile();
        file.setUserId(userId);
        file.setOrgId(orgId);
        file.setFileName(fileName);
        file.setDisplayName(fileName);
        file.setStorageKey(storageKey);
        file.setBucket(bucket);
        file.setFileSize(fileSize);
        file.setContentType(contentType);
        file.setFileType(fileType);
        file.setSourceId(sourceId);
        file.setDownloadCount(0);
        file.setShareCount(0);
        file.setStatus("active");
        file.setLocalExists(true);
        file.setReadonlyRestored(false);
        file.setUploadProgress(100);
        cloudFileMapper.insert(file);
        return file;
    }

    public CloudFile createComparisonRecord(Long comparisonId, String mode, CloudFile first, CloudFile second) {
        CloudFile file = new CloudFile();
        file.setUserId(requireCurrentUserId());
        file.setOrgId(first.getOrgId());
        file.setFileName(displayName(first));
        file.setDisplayName(displayName(first));
        file.setStorageKey(null);
        file.setBucket(cosCredentialService.getBucket());
        file.setFileSize(0L);
        file.setContentType("application/json");
        file.setFileType("comparison");
        file.setBusinessType("clip".equals(mode) ? "clip_comparison" : "full_comparison");
        file.setBusinessId(comparisonId);
        file.setComparisonId(comparisonId);
        file.setRecordingId(first.getRecordingId());
        file.setStreamerId(first.getStreamerId());
        file.setAnchorName(displayName(second));
        file.setIndustryId(first.getIndustryId());
        file.setAccountType(first.getAccountType());
        file.setUploadAccount(first.getUploadAccount());
        file.setRecordedAt(LocalDateTime.now());
        file.setDurationSeconds(null);
        file.setLocalExists(Boolean.TRUE.equals(first.getLocalExists()) && Boolean.TRUE.equals(second.getLocalExists()));
        file.setReadonlyRestored(false);
        file.setUploadProgress(100);
        file.setSourceId(comparisonId);
        file.setDownloadCount(0);
        file.setShareCount(0);
        file.setStatus("active");
        cloudFileMapper.insert(file);
        return file;
    }

    public CloudFile getOwnedFile(Long fileId) {
        CloudFile file = cloudFileMapper.selectById(fileId);
        if (file == null || "deleted".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "云端文件不存在");
        }
        validateOwnership(file);
        return file;
    }

    private Page<CloudFileVO> toVOPage(Page<CloudFile> entityPage) {
        Page<CloudFileVO> voPage = new Page<CloudFileVO>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(new java.util.ArrayList<CloudFileVO>());
        Map<Long, String> avatarMap = loadStreamerAvatarMap(entityPage.getRecords());
        // 存量数据 upload_account 可能为空（旧上传未带），按 userId 批量兜底为该用户的 phone/username
        Map<Long, String> userAccountMap = loadUserAccountFallback(entityPage.getRecords());
        for (CloudFile file : entityPage.getRecords()) {
            CloudFileVO vo = CloudFileVO.fromEntity(file);
            if (file.getStreamerId() != null) {
                vo.setAnchorAvatar(avatarMap.get(file.getStreamerId()));
            }
            if ((vo.getUploadAccount() == null || vo.getUploadAccount().isEmpty()) && file.getUserId() != null) {
                vo.setUploadAccount(userAccountMap.get(file.getUserId()));
            }
            if (isComparison(file.getBusinessType())) {
                populateComparisonStreamerInfo(vo, file);
            }
            voPage.getRecords().add(vo);
        }
        return voPage;
    }

    private Map<Long, String> loadUserAccountFallback(List<CloudFile> files) {
        Map<Long, String> map = new HashMap<Long, String>();
        if (files == null || files.isEmpty()) return map;
        Set<Long> userIds = new HashSet<Long>();
        for (CloudFile f : files) {
            if (f.getUserId() != null
                && (f.getUploadAccount() == null || f.getUploadAccount().isEmpty())) {
                userIds.add(f.getUserId());
            }
        }
        if (userIds.isEmpty()) return map;
        try {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                if (u == null || u.getId() == null) continue;
                String account = (u.getPhone() != null && !u.getPhone().isEmpty()) ? u.getPhone()
                        : (u.getUsername() != null ? u.getUsername() : null);
                if (account != null) map.put(u.getId(), account);
            }
        } catch (Exception e) {
            log.warn("load uploadAccount fallback failed: {}", e.getMessage());
        }
        return map;
    }

    private void populateComparisonStreamerInfo(CloudFileVO vo, CloudFile file) {
        if (file.getComparisonId() == null) {
            return;
        }
        Comparison comparison = comparisonMapper.selectById(file.getComparisonId());
        if (comparison == null) {
            return;
        }
        fillComparisonSide(vo, comparison.getRecordingIdOptimize(), true);
        fillComparisonSide(vo, comparison.getRecordingIdReference(), false);
    }

    private void fillComparisonSide(CloudFileVO vo, Long recordingId, boolean optimize) {
        if (recordingId == null) {
            return;
        }
        Recording recording = recordingMapper.selectById(recordingId);
        if (recording == null || recording.getStreamerId() == null) {
            return;
        }
        Streamer streamer = streamerMapper.selectById(recording.getStreamerId());
        if (streamer == null) {
            return;
        }
        if (optimize) {
            vo.setAnchorNameOptimize(streamer.getAnchorName());
            vo.setAnchorAvatarOptimize(streamer.getAnchorAvatar());
        } else {
            vo.setAnchorNameReference(streamer.getAnchorName());
            vo.setAnchorAvatarReference(streamer.getAnchorAvatar());
        }
    }

    private Map<Long, String> loadStreamerAvatarMap(List<CloudFile> files) {
        Set<Long> streamerIds = new HashSet<Long>();
        for (CloudFile file : files) {
            if (file.getStreamerId() != null) {
                streamerIds.add(file.getStreamerId());
            }
        }
        Map<Long, String> avatarMap = new HashMap<Long, String>();
        if (streamerIds.isEmpty()) {
            return avatarMap;
        }
        List<Streamer> streamers = streamerMapper.selectBatchIds(streamerIds);
        for (Streamer streamer : streamers) {
            avatarMap.put(streamer.getId(), streamer.getAnchorAvatar());
        }
        return avatarMap;
    }

    private void validateOwnership(CloudFile file) {
        String role = SecurityContextHelper.currentRole();
        Long userId = requireCurrentUserId();
        if (!"super_admin".equals(role) && !userId.equals(file.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_RESOURCE_OWNER);
        }
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private void deleteStorageObject(CloudFile file) {
        if (!hasText(file.getStorageKey())) {
            return;
        }
        try {
            if (cosCredentialService.isConfigured()) {
                cosCredentialService.deleteObject(file.getStorageKey());
            } else {
                storageService.delete(file.getBucket(), file.getStorageKey());
            }
        } catch (Exception e) {
            log.error("Failed to delete cloud object id={}, key={}", file.getId(), file.getStorageKey(), e);
            throw new BusinessException(ErrorCode.THIRD_PARTY_UNAVAILABLE,
                    "对象存储删除失败，请稍后重试: " + e.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isComparison(String businessType) {
        return "full_comparison".equals(businessType) || "clip_comparison".equals(businessType);
    }

    private Long resolveAnalysisTaskId(CloudFile file) {
        if (isComparison(file.getBusinessType())) {
            return null;
        }
        String expectedType = "clip_recap".equals(file.getBusinessType()) ? "clip" : "full";
        Long recordingId = file.getRecordingId() != null ? file.getRecordingId() : file.getBusinessId();
        if (recordingId != null) {
            AnalysisTask byRecording = analysisTaskMapper.selectOne(
                    new LambdaQueryWrapper<AnalysisTask>()
                            .eq(AnalysisTask::getRecordingId, recordingId)
                            .eq(AnalysisTask::getType, expectedType)
                            .eq(AnalysisTask::getUserId, requireCurrentUserId())
                            .orderByDesc(AnalysisTask::getCreatedAt)
                            .last("LIMIT 1"));
            if (byRecording != null) {
                return byRecording.getId();
            }
        }

        Long directId = "clip_recap".equals(file.getBusinessType()) && file.getClipId() != null
                ? file.getClipId()
                : file.getBusinessId();
        if (directId == null) {
            return null;
        }
        AnalysisTask direct = analysisTaskMapper.selectById(directId);
        if (direct != null && requireCurrentUserId().equals(direct.getUserId()) && expectedType.equals(direct.getType())) {
            return direct.getId();
        }
        return null;
    }

    private String displayName(CloudFile file) {
        if (file == null) {
            return "";
        }
        if (hasText(file.getDisplayName())) {
            return file.getDisplayName();
        }
        if (hasText(file.getFileName())) {
            return file.getFileName();
        }
        return Objects.toString(file.getId(), "");
    }
}
