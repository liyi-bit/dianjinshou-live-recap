package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.PageResult;
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
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import com.dianjinshou.modules.vip.entity.VipPlan;
import com.dianjinshou.modules.vip.mapper.VipPlanMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private VipPlanMapper vipPlanMapper;
    @Mock private OperationLogMapper operationLogMapper;
    @Mock private RecordingMapper recordingMapper;
    @Mock private AnalysisTaskMapper analysisTaskMapper;

    @InjectMocks
    private AdminService adminService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Organization.class);
        TableInfoHelper.initTableInfo(assistant, OperationLog.class);
        TableInfoHelper.initTableInfo(assistant, VipPlan.class);
    }

    @BeforeEach
    void setUp() {
        setSecurityContext(1L, "super_admin", 5L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== Dashboard ==========

    @Test
    void getDashboardStats_superAdmin() {
        when(userMapper.selectCount(any())).thenReturn(100L, 5L, 20L, 15L);
        when(recordingMapper.selectCount(any())).thenReturn(50L);
        when(analysisTaskMapper.selectCount(any())).thenReturn(30L);

        DashboardStatsVO stats = adminService.getDashboardStats();

        assertEquals(100, stats.getTotalUsers());
        assertEquals(50, stats.getTotalRecordings());
        assertEquals(30, stats.getTotalTasks());
    }

    @Test
    void getDashboardStats_operatorForbidden() {
        setSecurityContext(2L, "operator", 5L);
        assertThrows(BusinessException.class, () -> adminService.getDashboardStats());
    }

    // ========== User Management ==========

    @Test
    void listUsers_superAdmin() {
        User user = makeUser(10L, "test", "operator", 5L);
        Page<User> page = new Page<>(1, 20);
        page.setRecords(Collections.singletonList(user));
        page.setTotal(1);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<AdminUserVO> result = adminService.listUsers(1, 20, null, null, null);

        assertEquals(1, result.getTotal());
        assertEquals("test", result.getItems().get(0).getUsername());
    }

    @Test
    void listUsers_adminFiltered() {
        setSecurityContext(2L, "admin", 5L);
        Page<User> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<AdminUserVO> result = adminService.listUsers(1, 20, "test", "operator", 1);

        assertEquals(0, result.getTotal());
    }

    @Test
    void updateUser_success() {
        User user = makeUser(10L, "test", "operator", 5L);
        when(userMapper.selectById(10L)).thenReturn(user);
        when(userMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        User updated = makeUser(10L, "test", "admin", 5L);
        when(userMapper.selectById(10L)).thenReturn(user, updated);

        AdminUserUpdateRequest req = new AdminUserUpdateRequest();
        req.setRole("admin");
        AdminUserVO vo = adminService.updateUser(10L, req);

        assertEquals("admin", vo.getRole());
    }

    @Test
    void updateUser_notFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        AdminUserUpdateRequest req = new AdminUserUpdateRequest();
        assertThrows(BusinessException.class, () -> adminService.updateUser(999L, req));
    }

    @Test
    void updateUser_crossOrgForbidden() {
        setSecurityContext(2L, "admin", 5L);
        User user = makeUser(10L, "test", "operator", 99L);
        when(userMapper.selectById(10L)).thenReturn(user);

        AdminUserUpdateRequest req = new AdminUserUpdateRequest();
        req.setRole("admin");
        assertThrows(BusinessException.class, () -> adminService.updateUser(10L, req));
    }

    @Test
    void deleteUser_success() {
        User user = makeUser(10L, "test", "operator", 5L);
        when(userMapper.selectById(10L)).thenReturn(user);
        when(userMapper.deleteById(10L)).thenReturn(1);

        adminService.deleteUser(10L);

        verify(userMapper).deleteById(10L);
    }

    @Test
    void deleteUser_notFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> adminService.deleteUser(999L));
    }

    // ========== Organization ==========

    @Test
    void listOrganizations_superAdmin() {
        Organization org = makeOrg(1L, "TestOrg");
        when(organizationMapper.selectList(any())).thenReturn(Collections.singletonList(org));
        when(userMapper.selectCount(any())).thenReturn(5L);

        List<AdminOrgVO> list = adminService.listOrganizations();

        assertEquals(1, list.size());
        assertEquals("TestOrg", list.get(0).getName());
        assertEquals(5, list.get(0).getMemberCount());
    }

    @Test
    void createOrganization_success() {
        when(organizationMapper.insert(any())).thenReturn(1);

        CreateOrgRequest req = new CreateOrgRequest();
        req.setName("NewOrg");
        AdminOrgVO vo = adminService.createOrganization(req);

        assertEquals("NewOrg", vo.getName());
        verify(organizationMapper).insert(any());
    }

    // ========== VIP Plans ==========

    @Test
    void listVipPlans() {
        VipPlan plan = new VipPlan();
        plan.setName("Free");
        when(vipPlanMapper.selectList(any())).thenReturn(Collections.singletonList(plan));

        List<VipPlan> plans = adminService.listVipPlans();

        assertEquals(1, plans.size());
        assertEquals("Free", plans.get(0).getName());
    }

    @Test
    void createVipPlan_success() {
        when(vipPlanMapper.insert(any())).thenReturn(1);

        VipPlanRequest req = new VipPlanRequest();
        req.setName("Enterprise");
        req.setLevel(3);
        req.setPrice(BigDecimal.valueOf(99));
        req.setDurationDays(30);

        VipPlan plan = adminService.createVipPlan(req);

        assertEquals("Enterprise", plan.getName());
        assertEquals(3, plan.getLevel());
        verify(vipPlanMapper).insert(any());
    }

    @Test
    void updateVipPlan_success() {
        VipPlan plan = new VipPlan();
        plan.setId(1L);
        plan.setName("Old");
        when(vipPlanMapper.selectById(1L)).thenReturn(plan);
        when(vipPlanMapper.updateById(any())).thenReturn(1);

        VipPlanRequest req = new VipPlanRequest();
        req.setName("New");
        req.setLevel(2);
        req.setPrice(BigDecimal.valueOf(50));
        req.setDurationDays(90);

        VipPlan result = adminService.updateVipPlan(1L, req);

        assertEquals("New", result.getName());
    }

    @Test
    void updateVipPlan_notFound() {
        when(vipPlanMapper.selectById(999L)).thenReturn(null);
        VipPlanRequest req = new VipPlanRequest();
        req.setName("X");
        req.setLevel(1);
        req.setPrice(BigDecimal.ONE);
        req.setDurationDays(30);
        assertThrows(BusinessException.class, () -> adminService.updateVipPlan(999L, req));
    }

    @Test
    void toggleVipPlan_success() {
        VipPlan plan = new VipPlan();
        plan.setId(1L);
        plan.setIsActive(1);
        when(vipPlanMapper.selectById(1L)).thenReturn(plan);
        when(vipPlanMapper.updateById(any())).thenReturn(1);

        adminService.toggleVipPlan(1L);

        assertEquals(0, plan.getIsActive());
        verify(vipPlanMapper).updateById(plan);
    }

    // ========== Operation Logs ==========

    @Test
    void listLogs_superAdmin() {
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setUserId(10L);
        log.setAction("LOGIN");
        log.setCreatedAt(LocalDateTime.now());

        Page<OperationLog> page = new Page<>(1, 20);
        page.setRecords(Collections.singletonList(log));
        page.setTotal(1);
        when(operationLogMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<OperationLogVO> result = adminService.listLogs(1, 20, null);

        assertEquals(1, result.getTotal());
        assertEquals("LOGIN", result.getItems().get(0).getAction());
    }

    @Test
    void listLogs_adminFilteredByOrg() {
        setSecurityContext(2L, "admin", 5L);

        User orgUser = makeUser(10L, "member", "operator", 5L);
        when(userMapper.selectList(any())).thenReturn(Collections.singletonList(orgUser));

        Page<OperationLog> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(operationLogMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<OperationLogVO> result = adminService.listLogs(1, 20, "LOGIN");

        assertEquals(0, result.getTotal());
    }

    @Test
    void listLogs_adminEmptyOrg() {
        setSecurityContext(2L, "admin", 5L);
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResult<OperationLogVO> result = adminService.listLogs(1, 20, null);

        assertEquals(0, result.getTotal());
        assertTrue(result.getItems().isEmpty());
    }

    // ========== Helpers ==========

    private void setSecurityContext(Long userId, String role, Long orgId) {
        SecurityUser securityUser = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                securityUser, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User makeUser(Long id, String username, String role, Long orgId) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setOrgId(orgId);
        user.setStatus(1);
        user.setVipLevel(0);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private Organization makeOrg(Long id, String name) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        org.setVipLevel(0);
        org.setCreatedAt(LocalDateTime.now());
        return org;
    }
}
