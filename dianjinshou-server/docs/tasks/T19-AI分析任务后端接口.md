# T19 · AI 分析任务后端接口

- **来源**: SPEC.md §5.6 / 实现计划 T19
- **优先级**: P2
- **状态**: done

## 需求摘要
实现 SPEC §5.6 全部 14 个 AI 分析接口：创建分析/获取结果/段落/关键词/诊断/优化动作/笔记/进度/再分析/取消等。

## Dev 区域
- **状态**: dev:fixed
- **变更摘要**: AnalysisController、AnalysisService、实体/Mapper/DTO/VO 已实现
- **涉及文件**: modules/recap/controller/, modules/recap/service/, modules/recap/entity/, modules/recap/mapper/, modules/recap/dto/, modules/recap/vo/
- **新增接口**: POST /analysis/full, POST /analysis/clip, GET /analysis/{id}, GET /analysis/{id}/paragraphs, GET /analysis/{id}/keywords, GET /analysis/{id}/diagnosis, POST /analysis/{id}/optimization 等
- **修复内容**: (1) AnalysisTaskHandlerTest 改为 Mock AnalysisTaskProcessor 适配 Handler 重构后的委托模式 (2) AnalysisServiceTest 添加 @BeforeAll 初始化 MyBatis-Plus TableInfoHelper 缓存解决 LambdaQueryWrapper 异常

## QA 区域
- **状态**: qa:passed
- **测试计划**: docs/test-plans/T19-T23-test-plan.md
- **单元测试**: src/test/java 下 AnalysisServiceTest.java, DiagnosisServiceTest.java
- **API测试**: test/api/AnalysisApiTest.java
- **测试报告**: docs/test-reports/QA-执行报告-2026-04-12.md

## 失败用例详情

### [ERROR] AnalysisTaskHandlerTest.handle_completesSuccessfully
- 层级: 单元测试
- 期望: 分析任务正常完成，状态依次变更为 ASR_PROCESSING → AI_PROCESSING → COMPLETED
- 实际: java.lang.NullPointerException at AnalysisTaskHandlerTest.java:60
- 根因: AnalysisTaskHandler 已重构为委托 AnalysisTaskProcessor.process(taskId)，但测试仍 Mock 旧的 AnalysisTaskMapper/RecordingMapper/AsrService/AiAnalysisService
- 关联文件: src/test/java/com/dianjinshou/modules/recap/task/AnalysisTaskHandlerTest.java

### [ERROR] AnalysisTaskHandlerTest.handle_taskNotFound
- 层级: 单元测试
- 期望: 任务不存在时不更新
- 实际: java.lang.NullPointerException at AnalysisTaskHandlerTest.java:75
- 根因: 同上
- 关联文件: src/test/java/com/dianjinshou/modules/recap/task/AnalysisTaskHandlerTest.java

### [ERROR] AnalysisTaskHandlerTest.handle_clipTask
- 层级: 单元测试
- 期望: 切片分析任务正常处理
- 实际: java.lang.NullPointerException at AnalysisTaskHandlerTest.java:94
- 根因: 同上
- 关联文件: src/test/java/com/dianjinshou/modules/recap/task/AnalysisTaskHandlerTest.java

### [ERROR] AnalysisServiceTest.listTasks_withStreamerId_noRecordings_returnsEmpty
- 层级: 单元测试
- 期望: streamerId 无录制时返回空列表
- 实际: com.baomidou.mybatisplus.core.exceptions.MybatisPlusException
- 根因: LambdaQueryWrapper<Recording> 在纯单元测试中缺少 MyBatis-Plus TableInfoHelper 缓存
- 关联文件: src/test/java/com/dianjinshou/modules/recap/service/AnalysisServiceTest.java
- 建议: 此用例应 Mock recordingMapper.selectList() 或移至集成测试

### 修复建议
1. AnalysisTaskHandlerTest: 改为 Mock AnalysisTaskProcessor 而非旧的 4 个依赖
```java
@Mock
private AnalysisTaskProcessor processor;
@InjectMocks
private AnalysisTaskHandler handler;
```
2. AnalysisServiceTest: Mock recordingMapper.selectList() 调用，避免 LambdaQueryWrapper 实例化

## 流转记录
| 时间 | 操作人 | 状态变更 | 备注 |
|---|---|---|---|
| 2026-04-12 | QA Agent | 创建任务文件 | 基于已有代码补建 |
| 2026-04-12 | QA Agent | qa:ready → qa:failed | 3 个 AnalysisTaskHandlerTest NPE + 1 个 MybatisPlus 环境问题 |
| 2026-04-12 | Dev Agent | dev:done → dev:fixed | AnalysisTaskHandlerTest 改为 Mock Processor + AnalysisServiceTest 初始化 TableInfoHelper，50 个测试全部通过 |
| 2026-04-12 | QA Agent | qa:failed → qa:passed | 回归测试通过：490 全量用例 0 失败 0 错误 |
