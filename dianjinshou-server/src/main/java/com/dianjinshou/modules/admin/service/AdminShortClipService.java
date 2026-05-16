package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.vo.ShortClipStatsVO;
import com.dianjinshou.modules.shortclip.entity.ShortClip;
import com.dianjinshou.modules.shortclip.mapper.ShortClipMapper;
import com.dianjinshou.modules.shortclip.vo.ShortClipVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminShortClipService {

    private final ShortClipMapper shortClipMapper;

    public AdminShortClipService(ShortClipMapper shortClipMapper) {
        this.shortClipMapper = shortClipMapper;
    }

    public PageResult<ShortClipVO> listAll(int page, int size, Long orgId, String status) {
        requireAdmin();

        LambdaQueryWrapper<ShortClip> query = new LambdaQueryWrapper<>();
        // Non-super_admin can only see their own org
        SecurityUser current = SecurityContextHelper.currentUser();
        if (!"super_admin".equals(current.getRole())) {
            query.eq(ShortClip::getOrgId, current.getOrgId());
        } else if (orgId != null) {
            query.eq(ShortClip::getOrgId, orgId);
        }
        if (status != null && !status.isEmpty()) {
            query.eq(ShortClip::getStatus, status);
        }
        query.orderByDesc(ShortClip::getCreatedAt);

        Page<ShortClip> pageObj = shortClipMapper.selectPage(new Page<>(page, size), query);
        List<ShortClipVO> items = new ArrayList<ShortClipVO>();
        for (ShortClip sc : pageObj.getRecords()) {
            items.add(ShortClipVO.fromEntity(sc));
        }
        return PageResult.of(items, pageObj.getTotal(), page, size);
    }

    public ShortClipStatsVO getStats() {
        requireAdmin();

        ShortClipStatsVO stats = new ShortClipStatsVO();
        stats.setTotalClips(shortClipMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setCompletedClips(shortClipMapper.selectCount(
                new LambdaQueryWrapper<ShortClip>().eq(ShortClip::getStatus, "completed")));
        stats.setFailedClips(shortClipMapper.selectCount(
                new LambdaQueryWrapper<ShortClip>().eq(ShortClip::getStatus, "failed")));

        // Sum file sizes
        List<ShortClip> allClips = shortClipMapper.selectList(
                new LambdaQueryWrapper<ShortClip>().select(ShortClip::getFileSize));
        long totalSize = 0;
        for (ShortClip sc : allClips) {
            if (sc.getFileSize() != null) {
                totalSize += sc.getFileSize();
            }
        }
        stats.setTotalFileSize(totalSize);

        return stats;
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
