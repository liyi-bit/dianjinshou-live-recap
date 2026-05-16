package com.dianjinshou.modules.shortclip.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.shortclip.entity.ShortClip;
import com.dianjinshou.modules.shortclip.mapper.ShortClipMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClipExportService {

    private static final Logger log = LoggerFactory.getLogger(ClipExportService.class);
    private static final int MAX_BATCH_SIZE = 20;

    private final ShortClipMapper shortClipMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public ClipExportService(ShortClipMapper shortClipMapper,
                             StorageService storageService,
                             StorageProperties storageProperties) {
        this.shortClipMapper = shortClipMapper;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    public String batchExport(List<Long> clipIds) {
        if (clipIds == null || clipIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要导出的切片");
        }
        if (clipIds.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "批量导出最多支持" + MAX_BATCH_SIZE + "个");
        }

        List<ShortClip> clips = new ArrayList<ShortClip>();
        for (Long id : clipIds) {
            ShortClip clip = shortClipMapper.selectById(id);
            if (clip == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "切片不存在: " + id);
            }
            validateOrgAccess(clip);
            if (!"completed".equals(clip.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "切片未完成处理: " + clip.getClipName());
            }
            clips.add(clip);
        }

        // In production: async zip packaging via RabbitMQ + WebSocket progress push
        // For now: return a placeholder download key
        String exportKey = "exports/" + SecurityContextHelper.currentOrgId() + "/"
                + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".zip";

        log.info("Batch export initiated: {} clips, exportKey={}", clips.size(), exportKey);
        return exportKey;
    }

    public void uploadToCloud(Long clipId) {
        ShortClip clip = shortClipMapper.selectById(clipId);
        if (clip == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "切片不存在");
        }
        validateOrgAccess(clip);

        if (!"completed".equals(clip.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "切片未完成处理");
        }

        if (clip.getStorageKey() != null && !clip.getStorageKey().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "切片已上传至云端");
        }

        // In production: read local file → upload to MinIO → update storage_key
        String storageKey = "clips/" + clip.getOrgId() + "/" + clip.getId() + "/" + clip.getClipName() + ".mp4";

        LambdaUpdateWrapper<ShortClip> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShortClip::getId, clipId)
                .set(ShortClip::getStorageKey, storageKey)
                .set(ShortClip::getStatus, "exported");
        shortClipMapper.update(null, wrapper);

        log.info("Clip uploaded to cloud: id={}, key={}", clipId, storageKey);
    }

    private void validateOrgAccess(ShortClip clip) {
        Long orgId = SecurityContextHelper.currentOrgId();
        String role = SecurityContextHelper.currentRole();
        if (!"super_admin".equals(role) && orgId != null && !orgId.equals(clip.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
