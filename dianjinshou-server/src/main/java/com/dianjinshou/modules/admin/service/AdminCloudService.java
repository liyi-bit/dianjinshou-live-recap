package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.admin.vo.CloudStatsVO;
import com.dianjinshou.modules.storage.entity.CloudFile;
import com.dianjinshou.modules.storage.entity.ShareLink;
import com.dianjinshou.modules.storage.mapper.CloudFileMapper;
import com.dianjinshou.modules.storage.mapper.ShareLinkMapper;
import com.dianjinshou.modules.storage.vo.CloudFileVO;
import com.dianjinshou.modules.storage.vo.ShareLinkVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminCloudService {

    private final CloudFileMapper cloudFileMapper;
    private final ShareLinkMapper shareLinkMapper;

    public AdminCloudService(CloudFileMapper cloudFileMapper, ShareLinkMapper shareLinkMapper) {
        this.cloudFileMapper = cloudFileMapper;
        this.shareLinkMapper = shareLinkMapper;
    }

    public CloudStatsVO getStats() {
        requireAdmin();

        CloudStatsVO stats = new CloudStatsVO();
        stats.setTotalFiles(cloudFileMapper.selectCount(
                new LambdaQueryWrapper<CloudFile>().eq(CloudFile::getStatus, "active")));

        // Sum total size via list — acceptable for admin dashboard
        List<CloudFile> allActive = cloudFileMapper.selectList(
                new LambdaQueryWrapper<CloudFile>().eq(CloudFile::getStatus, "active").select(CloudFile::getFileSize, CloudFile::getFileType));
        long totalSize = 0;
        long recordings = 0;
        long clips = 0;
        long documents = 0;
        for (CloudFile f : allActive) {
            if (f.getFileSize() != null) {
                totalSize += f.getFileSize();
            }
            if ("recording".equals(f.getFileType())) {
                recordings++;
            } else if ("clip".equals(f.getFileType())) {
                clips++;
            } else {
                documents++;
            }
        }
        stats.setTotalSize(totalSize);
        stats.setRecordingCount(recordings);
        stats.setClipCount(clips);
        stats.setDocumentCount(documents);

        stats.setActiveShareCount(shareLinkMapper.selectCount(
                new LambdaQueryWrapper<ShareLink>().eq(ShareLink::getStatus, "active")));

        return stats;
    }

    public PageResult<CloudFileVO> listFiles(int page, int size, Long orgId, String fileType, String keyword) {
        requireAdmin();

        LambdaQueryWrapper<CloudFile> query = new LambdaQueryWrapper<>();
        query.eq(CloudFile::getStatus, "active");
        if (orgId != null) {
            query.eq(CloudFile::getOrgId, orgId);
        }
        if (fileType != null && !fileType.isEmpty()) {
            query.eq(CloudFile::getFileType, fileType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(CloudFile::getFileName, keyword);
        }
        query.orderByDesc(CloudFile::getCreatedAt);

        Page<CloudFile> entityPage = cloudFileMapper.selectPage(new Page<>(page, size), query);
        List<CloudFileVO> items = new ArrayList<CloudFileVO>();
        for (CloudFile cf : entityPage.getRecords()) {
            items.add(CloudFileVO.fromEntity(cf));
        }
        return PageResult.of(items, entityPage.getTotal(), page, size);
    }

    public PageResult<ShareLinkVO> listShares(int page, int size, String status) {
        requireAdmin();

        LambdaQueryWrapper<ShareLink> query = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            query.eq(ShareLink::getStatus, status);
        }
        query.orderByDesc(ShareLink::getCreatedAt);

        Page<ShareLink> entityPage = shareLinkMapper.selectPage(new Page<>(page, size), query);
        List<ShareLinkVO> items = new ArrayList<ShareLinkVO>();
        for (ShareLink link : entityPage.getRecords()) {
            CloudFile file = cloudFileMapper.selectById(link.getCloudFileId());
            String fileName = file != null ? file.getFileName() : "已删除";
            items.add(ShareLinkVO.fromEntity(link, fileName));
        }
        return PageResult.of(items, entityPage.getTotal(), page, size);
    }

    public void disableShare(Long id) {
        requireAdmin();

        ShareLink link = shareLinkMapper.selectById(id);
        if (link == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分享链接不存在");
        }
        link.setStatus("disabled");
        shareLinkMapper.updateById(link);
    }

    private void requireAdmin() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String role = user.getRole();
        if (!"super_admin".equals(role) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此操作仅限管理员，请联系管理员开通");
        }
    }
}
