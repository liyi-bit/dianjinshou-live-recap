package com.dianjinshou.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;

/**
 * 组织范围查询辅助 — 为 LambdaQueryWrapper 添加 org_id 过滤
 *
 * 用法：
 *   OrgScopeHelper.applyOrgScope(wrapper, Entity::getOrgId);
 *
 * super_admin 不追加过滤条件
 */
public final class OrgScopeHelper {

    private OrgScopeHelper() {}

    public static <T> void applyOrgScope(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> orgIdGetter) {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if ("super_admin".equals(user.getRole())) {
            return;
        }
        if (user.getRole() != null && user.getRole().startsWith("admin_")) {
            return;
        }

        Long orgId = user.getOrgId();
        if (orgId == null) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "用户未关联组织");
        }

        wrapper.eq(orgIdGetter, orgId);
    }
}
