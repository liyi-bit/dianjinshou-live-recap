# T05 · JWT 鉴权 + Spring Security 配置

- **来源**: SPEC.md §5.3 / 实现计划 T05
- **优先级**: P0
- **状态**: in_progress

## 需求摘要
实现 JWT 工具类（生成/解析/刷新）、JwtAuthFilter、Spring Security 配置、RBAC 4 级角色权限注解。

## Dev 区域
- **状态**: dev:done
- **变更摘要**: JwtUtil、JwtAuthFilter、SecurityConfig、RequiresRole 注解及 AOP 切面、RateLimit 等安全组件已实现
- **涉及文件**: common/security/JwtUtil.java, JwtAuthFilter.java, RequiresRole.java, RequiresRoleAspect.java, SecurityContextHelper.java, SecurityUser.java, RateLimit.java, RateLimitAspect.java, config/SecurityConfig.java, config/JwtProperties.java
- **新增接口**: 无（安全基础层）

## QA 区域
- **状态**: qa:passed
- **测试计划**: docs/test-plans/T05-T07-test-plan.md
- **测试报告**: docs/test-reports/QA-执行报告-2026-04-12.md
- **测试代码**: test/unit/server/T05_JwtUtilTest.java, T05_JwtAuthFilterTest.java, T05_RequiresRoleAspectTest.java, T05_RateLimitAspectTest.java, T05_SecurityContextHelperTest.java

## 流转记录
| 时间 | 操作人 | 状态变更 | 备注 |
|---|---|---|---|
| 2026-04-12 | QA Agent | 创建任务文件 | 基于已有代码补建 |
| 2026-04-12 | QA Agent | qa:ready → qa:passed | 全部用例通过 |
