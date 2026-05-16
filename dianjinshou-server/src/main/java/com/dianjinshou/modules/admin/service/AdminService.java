package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.dto.AdminUserUpdateRequest;
import com.dianjinshou.modules.admin.dto.CreateOrgRequest;
import com.dianjinshou.modules.admin.dto.VipPlanRequest;
import com.dianjinshou.modules.admin.entity.OperationLog;
import com.dianjinshou.modules.admin.mapper.OperationLogMapper;
import com.dianjinshou.modules.admin.vo.AdminOrgVO;
import com.dianjinshou.modules.admin.vo.AdminUserVO;
import com.dianjinshou.modules.admin.vo.DashboardStatsVO;
import com.dianjinshou.modules.admin.vo.OperationLogVO;
import com.dianjinshou.modules.auth.entity.User;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.organization.entity.Organization;
import com.dianjinshou.modules.organization.mapper.OrganizationMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import com.dianjinshou.modules.vip.entity.VipPlan;
import com.dianjinshou.modules.vip.mapper.VipPlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final VipPlanMapper vipPlanMapper;
    private final OperationLogMapper operationLogMapper;
    private final RecordingMapper recordingMapper;
    private final AnalysisTaskMapper analysisTaskMapper;
    private final StreamerMapper streamerMapper;

    public AdminService(UserMapper userMapper,
                        OrganizationMapper organizationMapper,
                        VipPlanMapper vipPlanMapper,
                        OperationLogMapper operationLogMapper,
                        RecordingMapper recordingMapper,
                        AnalysisTaskMapper analysisTaskMapper,
                        StreamerMapper streamerMapper) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.vipPlanMapper = vipPlanMapper;
        this.operationLogMapper = operationLogMapper;
        this.recordingMapper = recordingMapper;
        this.analysisTaskMapper = analysisTaskMapper;
        this.streamerMapper = streamerMapper;
    }

    // ========== Dashboard ==========

    public DashboardStatsVO getDashboardStats() {
        requireAdmin();
        DashboardStatsVO stats = new DashboardStatsVO();

        // 使用滚动的 24h / 30d 窗口避免跨 0 点清零和容器时区歧义
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime last30d = now.minusDays(30);

        stats.setTotalUsers(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        stats.setTodayNewUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, last24h)));
        stats.setMonthNewUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, last30d)));
        stats.setTodayActive(userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getLastLoginAt, last24h)));

        stats.setTotalRecordings(recordingMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setTotalTasks(analysisTaskMapper.selectCount(new LambdaQueryWrapper<>()));

        stats.setPaidUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().gt(User::getVipLevel, 0)));

        return stats;
    }

    // ========== User Management ==========

    public PageResult<AdminUserVO> listUsers(int page, int size, String keyword, String role, Integer status) {
        requireAdmin();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(wrapper, User::getOrgId);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getPhone, keyword));
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageObj = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUserVO> items = pageObj.getRecords().stream()
                .map(AdminUserVO::fromEntity).collect(Collectors.toList());

        // 批量聚合：主播数 / 录制数 / 今日新增
        Set<Long> userIds = items.stream().map(AdminUserVO::getId).collect(Collectors.toSet());
        if (!userIds.isEmpty()) {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            Map<Long, Long> streamerTotal = groupCountByUser(streamerMapper.selectMaps(
                    new QueryWrapper<Streamer>()
                            .select("user_id AS user_id, COUNT(*) AS cnt")
                            .in("user_id", userIds)
                            .eq("deleted", 0)
                            .groupBy("user_id")));
            Map<Long, Long> recordingTotal = groupCountByUser(recordingMapper.selectMaps(
                    new QueryWrapper<Recording>()
                            .select("user_id AS user_id, COUNT(*) AS cnt")
                            .in("user_id", userIds)
                            .eq("deleted", 0)
                            .groupBy("user_id")));
            Map<Long, Long> streamerToday = groupCountByUser(streamerMapper.selectMaps(
                    new QueryWrapper<Streamer>()
                            .select("user_id AS user_id, COUNT(*) AS cnt")
                            .in("user_id", userIds)
                            .eq("deleted", 0)
                            .ge("created_at", todayStart)
                            .groupBy("user_id")));
            Map<Long, Long> recordingToday = groupCountByUser(recordingMapper.selectMaps(
                    new QueryWrapper<Recording>()
                            .select("user_id AS user_id, COUNT(*) AS cnt")
                            .in("user_id", userIds)
                            .eq("deleted", 0)
                            .ge("created_at", todayStart)
                            .groupBy("user_id")));

            for (AdminUserVO vo : items) {
                vo.setStreamerCount(streamerTotal.getOrDefault(vo.getId(), 0L));
                vo.setRecordingCount(recordingTotal.getOrDefault(vo.getId(), 0L));
                vo.setTodayStreamerCount(streamerToday.getOrDefault(vo.getId(), 0L));
                vo.setTodayRecordingCount(recordingToday.getOrDefault(vo.getId(), 0L));
            }
        }
        return PageResult.of(items, pageObj.getTotal(), page, size);
    }

    private Map<Long, Long> groupCountByUser(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> m = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object uid = r.get("user_id");
            Object cnt = r.get("cnt");
            if (uid instanceof Number && cnt instanceof Number) {
                m.put(((Number) uid).longValue(), ((Number) cnt).longValue());
            }
        }
        return m;
    }

    public AdminUserVO updateUser(Long userId, AdminUserUpdateRequest request) {
        requireAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        checkOrgAccess(user.getOrgId());

        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<>();
        update.eq(User::getId, userId);
        if (request.getRole() != null) {
            update.set(User::getRole, request.getRole());
        }
        if (request.getStatus() != null) {
            update.set(User::getStatus, request.getStatus());
        }
        if (request.getVipLevel() != null) {
            update.set(User::getVipLevel, request.getVipLevel());
        }
        userMapper.update(null, update);

        return AdminUserVO.fromEntity(userMapper.selectById(userId));
    }

    public void deleteUser(Long userId) {
        requireAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        checkOrgAccess(user.getOrgId());
        userMapper.deleteById(userId);
    }

    // ========== Organization Management ==========

    public List<AdminOrgVO> listOrganizations() {
        requireAdmin();
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        OrgScopeHelper.applyOrgScope(wrapper, Organization::getId);
        wrapper.orderByDesc(Organization::getCreatedAt);

        List<Organization> orgs = organizationMapper.selectList(wrapper);
        return orgs.stream().map(org -> {
            long memberCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getOrgId, org.getId()));
            return AdminOrgVO.fromEntity(org, null, (int) memberCount);
        }).collect(Collectors.toList());
    }

    public AdminOrgVO createOrganization(CreateOrgRequest request) {
        requireAdmin();
        Organization org = new Organization();
        org.setName(request.getName());
        org.setVipLevel(0);
        org.setMaxMembers(20);
        organizationMapper.insert(org);
        return AdminOrgVO.fromEntity(org, SecurityContextHelper.currentUserId(), 0);
    }

    // ========== VIP Plan Management ==========

    public List<VipPlan> listVipPlans() {
        requireAdmin();
        return vipPlanMapper.selectList(
                new LambdaQueryWrapper<VipPlan>().orderByAsc(VipPlan::getSortOrder));
    }

    public VipPlan createVipPlan(VipPlanRequest request) {
        requireAdmin();
        VipPlan plan = new VipPlan();
        applyPlanFields(plan, request);
        plan.setIsActive(1);
        plan.setSortOrder(0);
        vipPlanMapper.insert(plan);
        return plan;
    }

    public VipPlan updateVipPlan(Long planId, VipPlanRequest request) {
        requireAdmin();
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        applyPlanFields(plan, request);
        vipPlanMapper.updateById(plan);
        return plan;
    }

    public void toggleVipPlan(Long planId) {
        requireAdmin();
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在");
        }
        plan.setIsActive(plan.getIsActive() == 1 ? 0 : 1);
        vipPlanMapper.updateById(plan);
    }

    private void applyPlanFields(VipPlan plan, VipPlanRequest request) {
        plan.setName(request.getName());
        plan.setLevel(request.getLevel());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        if (request.getAiQuota() != null) plan.setAiQuota(request.getAiQuota());
        if (request.getMaxRooms() != null) plan.setMaxRooms(request.getMaxRooms());
        if (request.getMaxMembers() != null) plan.setMaxMembers(request.getMaxMembers());
        if (request.getFeatures() != null) plan.setFeatures(request.getFeatures());
    }

    // ========== Operation Logs ==========

    public PageResult<OperationLogVO> listLogs(int page, int size, String action) {
        requireAdmin();
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(OperationLog::getAction, action);
        }

        // admin can only see logs from their own org users
        SecurityUser current = SecurityContextHelper.currentUser();
        boolean crossOrgAdmin = "super_admin".equals(current.getRole())
                || (current.getRole() != null && current.getRole().startsWith("admin_"));
        if (!crossOrgAdmin) {
            List<Long> orgUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>().eq(User::getOrgId, current.getOrgId())
                            .select(User::getId))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (orgUserIds.isEmpty()) {
                return PageResult.of(java.util.Collections.emptyList(), 0, page, size);
            }
            wrapper.in(OperationLog::getUserId, orgUserIds);
        }

        wrapper.orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> pageObj = operationLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<OperationLogVO> items = pageObj.getRecords().stream()
                .map(OperationLogVO::fromEntity).collect(Collectors.toList());
        return PageResult.of(items, pageObj.getTotal(), page, size);
    }

    // ========== Helpers ==========

    private void requireAdmin() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String role = user.getRole();
        if (role == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此操作仅限管理员，请联系管理员开通");
        }
        if (role.startsWith("admin_")) return;
        if (!"super_admin".equals(role) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此操作仅限管理员，请联系管理员开通");
        }
    }

    private void checkOrgAccess(Long targetOrgId) {
        SecurityUser user = SecurityContextHelper.currentUser();
        if ("super_admin".equals(user.getRole())) {
            return;
        }
        if (user.getRole() != null && user.getRole().startsWith("admin_")) {
            return;
        }
        if (targetOrgId == null || !targetOrgId.equals(user.getOrgId())) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS);
        }
    }
}
