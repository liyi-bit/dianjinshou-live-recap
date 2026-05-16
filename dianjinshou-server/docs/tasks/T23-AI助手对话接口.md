# T23 · AI 助手对话接口

- **来源**: SPEC.md §5.7 / 实现计划 T23
- **优先级**: P2
- **状态**: in_progress

## 需求摘要
实现 SPEC §5.7 全部 AI 助手接口：SSE 流式对话、预设问题、模型切换、对话历史。

## Dev 区域
- **状态**: dev:done
- **变更摘要**: AiAssistantController、AiChatController、AiSessionController 及 Service 层已实现
- **涉及文件**: modules/aiassistant/, modules/ai/controller/, modules/ai/service/, modules/ai/entity/, modules/ai/mapper/, modules/ai/dto/
- **新增接口**: POST /ai/chat, GET /ai/presets/operation, POST /ai/model/switch, GET /ai/history

## QA 区域
- **状态**: qa:passed
- **测试计划**: docs/test-plans/T19-T23-test-plan.md
- **测试报告**: docs/test-reports/QA-执行报告-2026-04-12.md
- **单元测试**: test/unit/server/AiChatServiceTest.java
- **API测试**: test/api/AiChatApiTest.java

## 流转记录
| 时间 | 操作人 | 状态变更 | 备注 |
|---|---|---|---|
| 2026-04-12 | QA Agent | 创建任务文件 | 基于已有代码补建 |
| 2026-04-12 | QA Agent | qa:ready → qa:passed | 全部用例通过 |
