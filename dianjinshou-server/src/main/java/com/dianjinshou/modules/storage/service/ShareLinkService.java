package com.dianjinshou.modules.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.storage.dto.CreateShareRequest;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.entity.ShareLink;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.ShareLinkMapper;
import com.dianjinshou.modules.storage.vo.ShareLinkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ShareLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShareLinkService.class);

    private final ShareLinkMapper shareLinkMapper;
    private final CloudFileMapper cloudFileMapper;
    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public ShareLinkService(ShareLinkMapper shareLinkMapper,
                             CloudFileMapper cloudFileMapper,
                             StorageService storageService,
                             StorageProperties storageProperties) {
        this.shareLinkMapper = shareLinkMapper;
        this.cloudFileMapper = cloudFileMapper;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    public ShareLinkVO createShare(Long fileId, CreateShareRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        CloudFile file = cloudFileMapper.selectById(fileId);
        if (file == null || "deleted".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        // 仅文件所有者可分享
        if (!userId.equals(file.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权分享他人文件");
        }
        // 仅可分享已上传完成的文件
        if (!"active".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "文件尚未上传完成，暂无法分享");
        }

        String shareCode = generateShareCode();

        ShareLink link = new ShareLink();
        link.setUserId(userId);
        link.setOrgId(orgId);
        link.setCloudFileId(fileId);
        link.setShareCode(shareCode);
        link.setPassword(request.getPassword());
        link.setMaxDownloads(request.getMaxDownloads());
        link.setDownloadCount(0);
        link.setViewCount(0);
        link.setStatus("active");

        if (request.getExpireHours() != null && request.getExpireHours() > 0) {
            link.setExpiresAt(LocalDateTime.now().plusHours(request.getExpireHours()));
        }

        shareLinkMapper.insert(link);
        log.info("Share link created: code={}, fileId={}", shareCode, fileId);

        return ShareLinkVO.fromEntity(link, file.getFileName());
    }

    public List<ShareLinkVO> listMyShares() {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        LambdaQueryWrapper<ShareLink> query = new LambdaQueryWrapper<>();
        query.eq(ShareLink::getUserId, userId);
        query.orderByDesc(ShareLink::getCreatedAt);
        List<ShareLink> links = shareLinkMapper.selectList(query);

        if (links.isEmpty()) return new ArrayList<>();

        // 批量查 CloudFile，避免 N+1
        java.util.Set<Long> fileIds = new java.util.HashSet<>();
        for (ShareLink l : links) fileIds.add(l.getCloudFileId());
        java.util.Map<Long, CloudFile> fileMap = new java.util.HashMap<>();
        for (CloudFile f : cloudFileMapper.selectBatchIds(fileIds)) {
            fileMap.put(f.getId(), f);
        }

        List<ShareLinkVO> vos = new ArrayList<ShareLinkVO>();
        for (ShareLink link : links) {
            CloudFile file = fileMap.get(link.getCloudFileId());
            String fileName = file != null ? file.getFileName() : "已删除";
            vos.add(ShareLinkVO.fromEntity(link, fileName));
        }
        return vos;
    }

    public void cancelShare(Long id) {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        ShareLink link = shareLinkMapper.selectById(id);
        if (link == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分享链接不存在");
        }
        if (!link.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无该操作权限");
        }

        link.setStatus("disabled");
        shareLinkMapper.updateById(link);
    }

    public ShareAccessResult accessShare(String shareCode, String password) {
        LambdaQueryWrapper<ShareLink> query = new LambdaQueryWrapper<>();
        query.eq(ShareLink::getShareCode, shareCode);
        ShareLink link = shareLinkMapper.selectOne(query);

        if (link == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分享链接不存在");
        }

        if (!"active".equals(link.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "分享链接已失效");
        }

        if (link.getExpiresAt() != null && LocalDateTime.now().isAfter(link.getExpiresAt())) {
            shareLinkMapper.markExpired(link.getId());
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "分享链接已过期");
        }

        if (link.getMaxDownloads() != null && link.getDownloadCount() >= link.getMaxDownloads()) {
            shareLinkMapper.markExpired(link.getId());
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "下载次数已达上限");
        }

        if (link.getPassword() != null && !link.getPassword().isEmpty()) {
            if (password == null || !link.getPassword().equals(password)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码错误");
            }
        }

        // 先确认文件可用，再做计数；避免文件已删时仍累计 viewCount
        CloudFile file = cloudFileMapper.selectById(link.getCloudFileId());
        if (file == null || "deleted".equals(file.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件已删除");
        }

        String downloadUrl = storageService.getPresignedUrl(
                file.getBucket(), file.getStorageKey(),
                storageProperties.getPresignedUrlExpireSeconds());

        // 一次原子自增 viewCount（访问 + 拿到 URL 视为一次"查看"）
        // downloadCount 由真正发起下载时在网关侧统计；预签名 URL 颁发不计 download
        shareLinkMapper.incrementViewCount(link.getId());

        ShareAccessResult result = new ShareAccessResult();
        result.setFileName(file.getFileName());
        result.setFileSize(file.getFileSize());
        result.setContentType(file.getContentType());
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static class ShareAccessResult {
        private String fileName;
        private Long fileSize;
        private String contentType;
        private String downloadUrl;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }
}
