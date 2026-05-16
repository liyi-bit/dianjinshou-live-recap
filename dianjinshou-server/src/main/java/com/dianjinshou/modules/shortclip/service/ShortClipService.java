package com.dianjinshou.modules.shortclip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.shortclip.dto.CreateShortClipRequest;
import com.dianjinshou.modules.shortclip.entity.ShortClip;
import com.dianjinshou.modules.shortclip.mapper.ShortClipMapper;
import com.dianjinshou.modules.shortclip.vo.ShortClipVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ShortClipService {

    private static final Logger log = LoggerFactory.getLogger(ShortClipService.class);
    private static final int MIN_DURATION = 3;
    private static final int MAX_DURATION = 300;

    private final ShortClipMapper shortClipMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public ShortClipService(ShortClipMapper shortClipMapper,
                            StorageService storageService,
                            StorageProperties storageProperties) {
        this.shortClipMapper = shortClipMapper;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    public ShortClipVO createClip(CreateShortClipRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        int duration = request.getEndTime() - request.getStartTime();
        if (duration < MIN_DURATION) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片时长不能小于3秒");
        }
        if (duration > MAX_DURATION) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片时长不能超过5分钟");
        }

        // Validate clip name length (codepoint count ≤ 15)
        if (request.getClipName().codePointCount(0, request.getClipName().length()) > 15) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "切片名称不能超过15个字符");
        }

        ShortClip clip = new ShortClip();
        clip.setUserId(userId);
        clip.setOrgId(orgId);
        clip.setRecordingId(request.getRecordingId());
        clip.setSourceType("recording");
        clip.setSourceId(request.getRecordingId());
        clip.setClipName(request.getClipName());
        clip.setStartTime(request.getStartTime());
        clip.setEndTime(request.getEndTime());
        clip.setDuration(duration);
        clip.setResolution(request.getResolution() != null ? request.getResolution() : "original");
        clip.setWatermarkText(request.getWatermarkText());
        clip.setOutputFormat("mp4");
        clip.setFileSize(0L);
        clip.setStatus("pending");
        shortClipMapper.insert(clip);

        // In production: send to RabbitMQ for async FFmpeg processing
        log.info("Created short clip: id={}, recording={}, range={}-{}s", clip.getId(),
                request.getRecordingId(), request.getStartTime(), request.getEndTime());
        return ShortClipVO.fromEntity(clip);
    }

    public Page<ShortClipVO> listClips(int page, int size, Long recordingId, String status) {
        LambdaQueryWrapper<ShortClip> query = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(query, ShortClip::getOrgId);

        if (recordingId != null) {
            query.eq(ShortClip::getRecordingId, recordingId);
        }
        if (status != null && !status.isEmpty()) {
            query.eq(ShortClip::getStatus, status);
        }
        query.orderByDesc(ShortClip::getCreatedAt);

        Page<ShortClip> entityPage = shortClipMapper.selectPage(new Page<>(page, size), query);
        Page<ShortClipVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(new ArrayList<ShortClipVO>());
        for (ShortClip sc : entityPage.getRecords()) {
            voPage.getRecords().add(ShortClipVO.fromEntity(sc));
        }
        return voPage;
    }

    public ShortClipVO getClip(Long id) {
        ShortClip clip = shortClipMapper.selectById(id);
        if (clip == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "切片不存在");
        }
        validateOrgAccess(clip);

        ShortClipVO vo = ShortClipVO.fromEntity(clip);
        // Generate presigned download URL if stored in cloud
        if (clip.getStorageKey() != null && !clip.getStorageKey().isEmpty()) {
            // URL would be added to VO in extended version
        }
        return vo;
    }

    public void deleteClip(Long id) {
        ShortClip clip = shortClipMapper.selectById(id);
        if (clip == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "切片不存在");
        }
        validateOrgAccess(clip);
        shortClipMapper.deleteById(id);
        log.info("Short clip deleted: id={}", id);
    }

    private void validateOrgAccess(ShortClip clip) {
        Long orgId = SecurityContextHelper.currentOrgId();
        String role = SecurityContextHelper.currentRole();
        if (!"super_admin".equals(role) && orgId != null && !orgId.equals(clip.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
