package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.storage.dto.CloudUploadCompleteRequest;
import com.dianjinshou.modules.storage.dto.CloudUploadFailRequest;
import com.dianjinshou.modules.storage.dto.CloudUploadInitRequest;
import com.dianjinshou.modules.storage.dto.CloudUploadProgressRequest;
import com.dianjinshou.modules.storage.dto.InitUploadRequest;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.entity.UploadTask;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.UploadTaskMapper;
import com.dianjinshou.modules.storage.vo.CloudUploadInitVO;
import com.dianjinshou.modules.storage.vo.UploadInitVO;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChunkedUploadService {

    private static final Logger log = LoggerFactory.getLogger(ChunkedUploadService.class);

    private static final long PART_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
    private static final int EXPIRE_HOURS = 24;

    private static final Set<String> ALLOWED_CONTENT_TYPES;
    static {
        Set<String> types = new HashSet<String>();
        types.add("video/mp4");
        types.add("video/x-flv");
        types.add("video/x-msvideo");
        types.add("video/avi");
        types.add("audio/mpeg");
        types.add("audio/mp3");
        types.add("text/plain");
        types.add("application/octet-stream");
        ALLOWED_CONTENT_TYPES = Collections.unmodifiableSet(types);
    }

    private final UploadTaskMapper uploadTaskMapper;
    private final CloudFileMapper cloudFileMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final CloudFileService cloudFileService;
    private final CosCredentialService cosCredentialService;
    private final StreamerMapper streamerMapper;
    private final UserMapper userMapper;

    public ChunkedUploadService(UploadTaskMapper uploadTaskMapper,
                                 CloudFileMapper cloudFileMapper,
                                 StorageService storageService,
                                 StorageProperties storageProperties,
                                 CloudFileService cloudFileService,
                                 CosCredentialService cosCredentialService,
                                 StreamerMapper streamerMapper,
                                 UserMapper userMapper) {
        this.uploadTaskMapper = uploadTaskMapper;
        this.cloudFileMapper = cloudFileMapper;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.cloudFileService = cloudFileService;
        this.cosCredentialService = cosCredentialService;
        this.streamerMapper = streamerMapper;
        this.userMapper = userMapper;
    }

    /** 上传账号兜底：客户端没显式传时，用当前登录用户的 phone 或 username 作为标识。 */
    private String resolveUploadAccount(CloudUploadInitRequest request, Long userId) {
        if (request.getUploadAccount() != null && !request.getUploadAccount().trim().isEmpty()) {
            return request.getUploadAccount().trim();
        }
        if (userId == null) return null;
        try {
            User u = userMapper.selectById(userId);
            if (u != null) {
                if (u.getPhone() != null && !u.getPhone().isEmpty()) return u.getPhone();
                if (u.getUsername() != null && !u.getUsername().isEmpty()) return u.getUsername();
            }
        } catch (Exception ignore) { /* 兜底失败不阻塞上传 */ }
        return null;
    }

    public CloudUploadInitVO initCloudUpload(CloudUploadInitRequest request) {
        Long userId = requireCurrentUserId();
        Long orgId = requireCurrentOrgId();
        validateCloudBusinessRequest(request, userId);

        // 单文件大小限制前置（避免无意义的 placeholder）
        if (request.getFileSize() != null && request.getFileSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE,
                    "单文件不能超过 " + (MAX_FILE_SIZE / 1024 / 1024 / 1024) + "GB");
        }

        CloudFile duplicate = findDuplicateBusinessCloudFile(userId, request);
        UploadTask existing = findExistingClientTask(userId, request);
        if (duplicate != null) {
            if (existing != null && samePendingUpload(existing, duplicate)) {
                return buildCloudInitVO(existing, duplicate, cosCredentialService.createSignedUploadUrl(existing.getStorageKey()));
            }
            rejectDuplicateUpload(duplicate);
        }

        if (existing != null) {
            CloudFile file = findCloudFileForTask(existing);
            return buildCloudInitVO(existing, file, cosCredentialService.createSignedUploadUrl(existing.getStorageKey()));
        }

        // 配额检查前置（getUsage 已包含 queued/uploading，减少并发 race 但不能完全消除）
        long usedBytes = cloudFileService.getUsage().getUsedBytes();
        long totalQuota = cloudFileService.getUsage().getTotalQuotaBytes();
        long requestSize = request.getFileSize() != null ? request.getFileSize() : 0;
        if (usedBytes + requestSize > totalQuota) {
            log.info("Cloud upload quota exceeded (pre-check): userId={}, used={}, quota={}, request={}",
                    userId, usedBytes, totalQuota, requestSize);
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "云空间容量不足");
        }

        String bucket = cosCredentialService.getBucket() != null && !cosCredentialService.getBucket().trim().isEmpty()
                ? cosCredentialService.getBucket()
                : storageProperties.getBucketFiles();
        String storageKey = cosCredentialService.buildObjectKey(
                userId,
                request.getBusinessType(),
                request.getBusinessId(),
                request.getFileName(),
                request.getRecordedAt());

        CloudFile file = createCloudPlaceholder(userId, orgId, request, bucket, storageKey, "queued", 0);
        UploadTask task = createUploadTask(userId, orgId, request, bucket, storageKey, "queued", 0);
        String uploadUrl = cosCredentialService.createSignedUploadUrl(storageKey);
        return buildCloudInitVO(task, file, uploadUrl);
    }

    public void updateCloudProgress(Long uploadId, CloudUploadProgressRequest request) {
        UploadTask task = getOwnedUploadTask(uploadId);
        int progress = request.getProgress() != null ? request.getProgress() : 0;
        progress = Math.max(0, Math.min(100, progress));

        task.setProgress(progress);
        task.setStatus(progress >= 100 ? "uploaded" : "uploading");
        uploadTaskMapper.updateById(task);

        CloudFile file = findCloudFileForTask(task);
        if (file != null) {
            file.setUploadProgress(progress);
            file.setStatus("uploading");
            cloudFileMapper.updateById(file);
        }
    }

    public void completeCloudUpload(Long uploadId, CloudUploadCompleteRequest request) {
        UploadTask task = getOwnedUploadTask(uploadId);
        task.setStatus("completed");
        task.setProgress(100);
        uploadTaskMapper.updateById(task);

        CloudFile file = findCloudFileForTask(task);
        if (file != null) {
            if (request != null && request.getFileSize() != null && request.getFileSize() > 0) {
                file.setFileSize(request.getFileSize());
            }
            if (request != null && request.getChecksum() != null) {
                file.setChecksum(request.getChecksum());
            }
            file.setStatus("active");
            file.setUploadProgress(100);
            cloudFileMapper.updateById(file);
        }
    }

    public void failCloudUpload(Long uploadId, CloudUploadFailRequest request) {
        UploadTask task = getOwnedUploadTask(uploadId);
        int retryCount = task.getRetryCount() != null ? task.getRetryCount() : 0;
        retryCount += 1;
        task.setRetryCount(retryCount);
        task.setLastError(request != null ? request.getErrorMessage() : null);
        task.setNextRetryAt(retryCount <= 3 ? LocalDateTime.now().plusMinutes(retryDelayMinutes(retryCount)) : null);
        task.setStatus(retryCount <= 3 ? "retry_scheduled" : "failed");
        uploadTaskMapper.updateById(task);

        CloudFile file = findCloudFileForTask(task);
        if (file != null && retryCount > 3) {
            file.setStatus("failed");
            cloudFileMapper.updateById(file);
            // 终态失败时尝试清理已上传的 partN 文件（best-effort，不阻塞业务）
            if (task.getStorageKey() != null && task.getTotalParts() != null && task.getBucket() != null) {
                for (int i = 1; i <= task.getTotalParts(); i++) {
                    String partKey = task.getStorageKey() + ".part" + i;
                    try {
                        storageService.delete(task.getBucket(), partKey);
                    } catch (Exception e) {
                        log.debug("Skip delete part: {}", partKey);
                    }
                }
            }
        }
    }

    public UploadInitVO initUpload(InitUploadRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getFileSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "文件大小不能超过2GB");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(request.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + request.getContentType());
        }

        String bucket = request.getBucket() != null ? request.getBucket() : storageProperties.getBucketFiles();
        String storageKey = buildStorageKey(orgId, userId, request.getFileName());
        int totalParts = (int) Math.ceil((double) request.getFileSize() / PART_SIZE);

        UploadTask task = new UploadTask();
        task.setUserId(userId);
        task.setOrgId(orgId);
        task.setFileName(request.getFileName());
        task.setFileSize(request.getFileSize());
        task.setContentType(request.getContentType());
        task.setBucket(bucket);
        task.setStorageKey(storageKey);
        task.setTotalParts(totalParts);
        task.setUploadedParts(0);
        task.setStatus("init");
        task.setExpiresAt(LocalDateTime.now().plusHours(EXPIRE_HOURS));
        task.setCreatedAt(LocalDateTime.now());
        uploadTaskMapper.insert(task);

        UploadInitVO vo = new UploadInitVO();
        vo.setUploadId(task.getId());
        vo.setStorageKey(storageKey);
        vo.setTotalParts(totalParts);
        vo.setPartSize(PART_SIZE);
        vo.setPartUploadUrls(Collections.emptyList());
        return vo;
    }

    public void uploadPart(Long uploadId, int partNumber, InputStream inputStream, long partSize) {
        UploadTask task = getAndValidateTask(uploadId);

        if (partNumber < 1 || partNumber > task.getTotalParts()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分片编号无效: " + partNumber);
        }

        String partKey = task.getStorageKey() + ".part" + partNumber;
        storageService.upload(task.getBucket(), partKey, inputStream, partSize, task.getContentType());

        // DB 自增避免并发分片上传计数丢失
        uploadTaskMapper.incrementUploadedParts(uploadId);

        log.info("Uploaded part {}/{} for upload task {}", partNumber, task.getTotalParts(), uploadId);
    }

    public String completeUpload(Long uploadId) {
        UploadTask task = getAndValidateTask(uploadId);

        if (task.getUploadedParts() < task.getTotalParts()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "分片未全部上传: " + task.getUploadedParts() + "/" + task.getTotalParts());
        }

        // 服务端合并所有 partN 为最终对象
        java.util.List<String> partKeys = new ArrayList<>();
        for (int i = 1; i <= task.getTotalParts(); i++) {
            partKeys.add(task.getStorageKey() + ".part" + i);
        }
        try {
            storageService.composeObject(task.getBucket(), task.getStorageKey(), partKeys);
        } catch (Exception e) {
            log.error("Failed to compose parts for upload {}: {}", uploadId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.THIRD_PARTY_UNAVAILABLE,
                    "合并分片失败，请稍后重试: " + e.getMessage());
        }

        // 合并成功，删除中间 partN 文件
        for (String partKey : partKeys) {
            try {
                storageService.delete(task.getBucket(), partKey);
            } catch (Exception e) {
                log.warn("Failed to delete part after compose: bucket={}, key={}", task.getBucket(), partKey, e);
            }
        }

        LambdaUpdateWrapper<UploadTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UploadTask::getId, uploadId)
                .set(UploadTask::getStatus, "completed");
        uploadTaskMapper.update(null, wrapper);

        log.info("Upload task {} completed (composed {} parts), storageKey={}",
                uploadId, partKeys.size(), task.getStorageKey());
        return task.getStorageKey();
    }

    public void cancelUpload(Long uploadId) {
        UploadTask task = getAndValidateTask(uploadId);

        for (int i = 1; i <= task.getTotalParts(); i++) {
            String partKey = task.getStorageKey() + ".part" + i;
            try {
                storageService.delete(task.getBucket(), partKey);
            } catch (Exception e) {
                log.warn("Failed to delete part {} for upload {}", i, uploadId, e);
            }
        }

        LambdaUpdateWrapper<UploadTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UploadTask::getId, uploadId)
                .set(UploadTask::getStatus, "cancelled");
        uploadTaskMapper.update(null, wrapper);

        log.info("Upload task {} cancelled", uploadId);
    }

    /** 每小时清理过期/失败的上传任务和孤儿 partN 文件 */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3600000L, initialDelay = 60000L)
    public void cleanExpiredTasks() {
        LambdaQueryWrapper<UploadTask> query = new LambdaQueryWrapper<>();
        query.in(UploadTask::getStatus, "init", "uploading")
                .lt(UploadTask::getExpiresAt, LocalDateTime.now());
        List<UploadTask> expired = uploadTaskMapper.selectList(query);

        for (UploadTask task : expired) {
            try {
                cancelUpload(task.getId());
                log.info("Cleaned expired upload task {}", task.getId());
            } catch (Exception e) {
                log.warn("Failed to clean expired upload {}", task.getId(), e);
            }
        }
    }

    /** 每天凌晨 4 点清理 failed 状态遗留的 partN 文件（避免无限增长） */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 4 * * ?")
    public void cleanFailedPartFiles() {
        LambdaQueryWrapper<UploadTask> query = new LambdaQueryWrapper<>();
        query.eq(UploadTask::getStatus, "failed");
        List<UploadTask> failedTasks = uploadTaskMapper.selectList(query);

        for (UploadTask task : failedTasks) {
            if (task.getTotalParts() == null || task.getStorageKey() == null) continue;
            for (int i = 1; i <= task.getTotalParts(); i++) {
                String partKey = task.getStorageKey() + ".part" + i;
                try {
                    storageService.delete(task.getBucket(), partKey);
                } catch (Exception e) {
                    log.debug("Skip delete part (not exist or already gone): {}", partKey);
                }
            }
        }
        log.info("Cleaned partN files for {} failed upload tasks", failedTasks.size());
    }

    private UploadTask getAndValidateTask(Long uploadId) {
        UploadTask task = uploadTaskMapper.selectById(uploadId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "上传任务不存在");
        }

        Long userId = SecurityContextHelper.currentUserId();
        if (userId != null && !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此上传任务");
        }

        if ("completed".equals(task.getStatus()) || "cancelled".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "上传任务已" +
                    ("completed".equals(task.getStatus()) ? "完成" : "取消"));
        }

        if (task.getExpiresAt() != null && task.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "上传任务已过期");
        }

        return task;
    }

    private UploadTask getOwnedUploadTask(Long uploadId) {
        UploadTask task = uploadTaskMapper.selectById(uploadId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "上传任务不存在");
        }
        Long userId = requireCurrentUserId();
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_RESOURCE_OWNER);
        }
        return task;
    }

    private void validateCloudBusinessRequest(CloudUploadInitRequest request, Long userId) {
        if (!cosCredentialService.isConfigured()) {
            throw new BusinessException(ErrorCode.THIRD_PARTY_NOT_CONFIGURED, "腾讯云 COS 未配置");
        }
        if (request.getFileSize() != null && request.getFileSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "文件大小不能超过2GB");
        }
        if (request.getStreamerId() != null) {
            Streamer streamer = streamerMapper.selectById(request.getStreamerId());
            if (streamer == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "主播不存在");
            }
            if (!userId.equals(streamer.getUserId())) {
                throw new BusinessException(ErrorCode.NOT_RESOURCE_OWNER, "无权操作该主播");
            }
            if (!Boolean.TRUE.equals(request.getManualUpload()) && !Boolean.TRUE.equals(streamer.getCloudSyncEnabled())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "主播未开启云空间同步");
            }
        }
    }

    private CloudFile createCloudPlaceholder(Long userId, Long orgId, CloudUploadInitRequest request,
                                             String bucket, String storageKey, String status, int progress) {
        CloudFile file = new CloudFile();
        file.setUserId(userId);
        file.setOrgId(orgId);
        file.setFileName(request.getFileName());
        file.setDisplayName(request.getFileName());
        file.setStorageKey(storageKey);
        file.setBucket(bucket);
        file.setFileSize(request.getFileSize());
        file.setContentType(request.getContentType() != null ? request.getContentType() : "application/octet-stream");
        file.setFileType(resolveFileType(request.getBusinessType()));
        file.setBusinessType(request.getBusinessType());
        file.setBusinessId(request.getBusinessId());
        file.setRecordingId(request.getRecordingId());
        file.setClipId(request.getClipId());
        file.setComparisonId(request.getComparisonId());
        file.setStreamerId(request.getStreamerId());
        file.setAnchorName(request.getAnchorName());
        file.setIndustryId(request.getIndustryId());
        file.setAccountType(request.getAccountType());
        file.setUploadAccount(resolveUploadAccount(request, userId));
        file.setRecordedAt(request.getRecordedAt());
        file.setDurationSeconds(request.getDurationSeconds());
        file.setSourceId(request.getBusinessId());
        file.setDownloadCount(0);
        file.setShareCount(0);
        file.setStatus(status);
        file.setLocalExists(true);
        file.setReadonlyRestored(false);
        file.setUploadProgress(progress);
        cloudFileMapper.insert(file);
        return file;
    }

    private UploadTask createUploadTask(Long userId, Long orgId, CloudUploadInitRequest request,
                                        String bucket, String storageKey, String status, int progress) {
        UploadTask task = new UploadTask();
        task.setUserId(userId);
        task.setOrgId(orgId);
        task.setFileName(request.getFileName());
        task.setFileSize(request.getFileSize());
        task.setContentType(request.getContentType() != null ? request.getContentType() : "application/octet-stream");
        task.setBucket(bucket);
        task.setStorageKey(storageKey);
        task.setBusinessType(request.getBusinessType());
        task.setBusinessId(request.getBusinessId());
        task.setLocalFilePath(request.getLocalFilePath());
        task.setTotalParts(1);
        task.setUploadedParts(0);
        task.setStatus(status);
        task.setRetryCount(0);
        task.setProgress(progress);
        task.setClientTaskId(request.getClientTaskId());
        task.setExpiresAt(LocalDateTime.now().plusSeconds(cosCredentialService.getUploadCredentialExpireSeconds()));
        task.setCreatedAt(LocalDateTime.now());
        uploadTaskMapper.insert(task);
        return task;
    }

    private CloudUploadInitVO buildCloudInitVO(UploadTask task, CloudFile file, String uploadUrl) {
        CloudUploadInitVO vo = new CloudUploadInitVO();
        vo.setUploadId(task.getId());
        vo.setFileId(file != null ? file.getId() : null);
        vo.setBucket(task.getBucket());
        vo.setStorageKey(task.getStorageKey());
        vo.setUploadUrl(uploadUrl);
        vo.setUploadMethod("PUT");
        vo.setExpiresAt(task.getExpiresAt());
        vo.setMaxRetry(3);
        vo.setUsedBytes(cloudFileService.getUsage().getUsedBytes());
        vo.setQuotaBytes(cloudFileService.getUsage().getTotalQuotaBytes());
        return vo;
    }

    private CloudFile findCloudFileByStorageKey(String storageKey) {
        if (storageKey == null) {
            return null;
        }
        return cloudFileMapper.selectOne(
                new LambdaQueryWrapper<CloudFile>()
                        .eq(CloudFile::getStorageKey, storageKey)
                        .orderByDesc(CloudFile::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private UploadTask findExistingClientTask(Long userId, CloudUploadInitRequest request) {
        if (request.getClientTaskId() == null || request.getClientTaskId().trim().isEmpty()) {
            return null;
        }
        UploadTask existing = uploadTaskMapper.selectOne(
                new LambdaQueryWrapper<UploadTask>()
                        .eq(UploadTask::getUserId, userId)
                        .eq(UploadTask::getClientTaskId, request.getClientTaskId().trim())
                        .orderByDesc(UploadTask::getCreatedAt)
                        .last("LIMIT 1"));
        if (existing == null || "completed".equals(existing.getStatus())) {
            return null;
        }
        return existing;
    }

    private boolean samePendingUpload(UploadTask task, CloudFile file) {
        if (task == null || file == null || "active".equals(file.getStatus())) {
            return false;
        }
        return Objects.equals(task.getStorageKey(), file.getStorageKey())
                && Objects.equals(task.getBusinessType(), file.getBusinessType())
                && Objects.equals(task.getBusinessId(), file.getBusinessId());
    }

    private CloudFile findCloudFileForTask(UploadTask task) {
        if (task == null || task.getStorageKey() == null) {
            return null;
        }
        CloudFile pending = cloudFileMapper.selectOne(
                new LambdaQueryWrapper<CloudFile>()
                        .eq(CloudFile::getUserId, task.getUserId())
                        .eq(CloudFile::getStorageKey, task.getStorageKey())
                        .eq(task.getBusinessType() != null, CloudFile::getBusinessType, task.getBusinessType())
                        .eq(task.getBusinessId() != null, CloudFile::getBusinessId, task.getBusinessId())
                        .in(CloudFile::getStatus, "queued", "uploading")
                        .orderByDesc(CloudFile::getCreatedAt)
                        .last("LIMIT 1"));
        if (pending != null) {
            return pending;
        }
        return findCloudFileByStorageKey(task.getStorageKey());
    }

    private CloudFile findDuplicateBusinessCloudFile(Long userId, CloudUploadInitRequest request) {
        String businessType = request.getBusinessType();
        if ("full_recap".equals(businessType)) {
            Long recordingId = firstId(request.getRecordingId(), request.getBusinessId());
            CloudFile byRecording = findDuplicateByField(userId, businessType, CloudFile::getRecordingId, recordingId);
            if (byRecording != null) {
                return byRecording;
            }
            return findDuplicateByField(userId, businessType, CloudFile::getBusinessId, request.getBusinessId());
        }
        if ("clip_recap".equals(businessType)) {
            Long clipId = firstId(request.getClipId(), request.getBusinessId());
            CloudFile byClip = findDuplicateByField(userId, businessType, CloudFile::getClipId, clipId);
            if (byClip != null) {
                return byClip;
            }
            return findDuplicateByField(userId, businessType, CloudFile::getBusinessId, request.getBusinessId());
        }
        if ("full_comparison".equals(businessType) || "clip_comparison".equals(businessType)) {
            Long comparisonId = firstId(request.getComparisonId(), request.getBusinessId());
            CloudFile byComparison = findDuplicateByField(userId, businessType, CloudFile::getComparisonId, comparisonId);
            if (byComparison != null) {
                return byComparison;
            }
            return findDuplicateByField(userId, businessType, CloudFile::getBusinessId, request.getBusinessId());
        }
        return null;
    }

    private CloudFile findDuplicateByField(Long userId,
                                           String businessType,
                                           com.baomidou.mybatisplus.core.toolkit.support.SFunction<CloudFile, ?> field,
                                           Long value) {
        if (value == null) {
            return null;
        }
        CloudFile active = cloudFileMapper.selectOne(
                new LambdaQueryWrapper<CloudFile>()
                        .eq(CloudFile::getUserId, userId)
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
                        .eq(CloudFile::getUserId, userId)
                        .eq(CloudFile::getBusinessType, businessType)
                        .eq(field, value)
                        .in(CloudFile::getStatus, "queued", "uploading")
                        .orderByDesc(CloudFile::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private void rejectDuplicateUpload(CloudFile file) {
        String subject = duplicateSubject(file.getBusinessType());
        if ("active".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, subject + "已在云空间，无需重复上传");
        }
        throw new BusinessException(ErrorCode.CONFLICT, subject + "已在上传队列，无需重复添加");
    }

    private String duplicateSubject(String businessType) {
        if ("clip_recap".equals(businessType)) {
            return "该切片复盘";
        }
        if ("full_comparison".equals(businessType) || "clip_comparison".equals(businessType)) {
            return "该对比复盘";
        }
        return "该全场复盘";
    }

    private Long firstId(Long first, Long second) {
        return first != null ? first : second;
    }

    private String resolveFileType(String businessType) {
        if (businessType == null) {
            return "recording";
        }
        if (businessType.contains("comparison")) {
            return "comparison";
        }
        if (businessType.contains("clip")) {
            return "clip";
        }
        return "recording";
    }

    private int retryDelayMinutes(int retryCount) {
        if (retryCount <= 1) {
            return 1;
        }
        if (retryCount == 2) {
            return 5;
        }
        return 30;
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private Long requireCurrentOrgId() {
        Long orgId = SecurityContextHelper.currentOrgId();
        if (orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return orgId;
    }

    private String buildStorageKey(Long orgId, Long userId, String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "org" + orgId + "/user" + userId + "/" + timestamp + "_" + uuid + "/" + fileName;
    }
}
