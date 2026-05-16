package com.dianjinshou.modules.ai.controller;

import com.dianjinshou.modules.ai.service.AiChatService;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import com.dianjinshou.modules.ai.vo.PresetQuestionVO;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T23 AI助手对话 API 接口测试
 * 使用 Spring MockMvc 测试 Controller 层请求/响应映射
 */
@WebMvcTest(AiChatController.class)
@DisplayName("T23: AI助手对话 API 接口测试")
class AiChatApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AiChatService aiChatService;

    // ========== POST /api/v1/ai/chat ==========
    @Nested
    @DisplayName("POST /ai/chat")
    class Chat {

        @Test
        @WithMockUser
        @DisplayName("正常对话 -- 200, 返回 assistant 消息")
        void shouldReturnChatResponse() throws Exception {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(1L);
            vo.setRole("assistant");
            vo.setContent("## 运营分析\n分析如下...");
            vo.setTokensUsed(50);
            when(aiChatService.chat(any())).thenReturn(vo);

            String body = "{\"assistantType\":\"operation\",\"message\":\"分析框架\"}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.role").value("assistant"))
                    .andExpect(jsonPath("$.data.content").isNotEmpty());
        }

        @Test
        @WithMockUser
        @DisplayName("带 taskId 上下文 -- 200")
        void shouldAcceptTaskIdContext() throws Exception {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(2L);
            vo.setRole("assistant");
            vo.setContent("分析结果");
            when(aiChatService.chat(any())).thenReturn(vo);

            String body = "{\"assistantType\":\"operation\",\"message\":\"test\",\"taskId\":42}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2));
        }

        @Test
        @WithMockUser
        @DisplayName("带 comparisonId 上下文 -- 200")
        void shouldAcceptComparisonIdContext() throws Exception {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(3L);
            vo.setRole("assistant");
            vo.setContent("对比分析");
            when(aiChatService.chat(any())).thenReturn(vo);

            String body = "{\"assistantType\":\"operation\",\"message\":\"test\",\"comparisonId\":7}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("带 presetQuestionId -- 200")
        void shouldAcceptPresetQuestionId() throws Exception {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(4L);
            vo.setRole("assistant");
            vo.setContent("框架分析");
            vo.setPresetQuestionId(1);
            when(aiChatService.chat(any())).thenReturn(vo);

            String body = "{\"assistantType\":\"operation\",\"message\":\"拆解框架\",\"presetQuestionId\":1}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.presetQuestionId").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("message 为空 -- 400")
        void shouldReturn400WhenMessageBlank() throws Exception {
            String body = "{\"assistantType\":\"operation\",\"message\":\"\"}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("assistantType 为空 -- 400")
        void shouldReturn400WhenAssistantTypeBlank() throws Exception {
            String body = "{\"assistantType\":\"\",\"message\":\"test\"}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("指定 aiModel=deepseek_r1 -- 200")
        void shouldAcceptDeepseekModel() throws Exception {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(5L);
            vo.setRole("assistant");
            vo.setContent("分析");
            vo.setThinking("让我思考...");
            when(aiChatService.chat(any())).thenReturn(vo);

            String body = "{\"assistantType\":\"operation\",\"message\":\"test\",\"aiModel\":\"deepseek_r1\"}";

            mockMvc.perform(post("/api/v1/ai/chat")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.thinking").isNotEmpty());
        }
    }

    // ========== GET /api/v1/ai/presets/{type} ==========
    @Nested
    @DisplayName("GET /ai/presets/{type}")
    class Presets {

        @Test
        @WithMockUser
        @DisplayName("operation 类型 -- 返回 12 个预设")
        void shouldReturn12OperationPresets() throws Exception {
            List<PresetQuestionVO> presets = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                presets.add(new PresetQuestionVO(i, "preset" + i, "desc" + i, "#fff"));
            }
            when(aiChatService.getPresets("operation")).thenReturn(presets);

            mockMvc.perform(get("/api/v1/ai/presets/operation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(12));
        }

        @Test
        @WithMockUser
        @DisplayName("compliance 类型 -- 返回 3 个预设")
        void shouldReturn3CompliancePresets() throws Exception {
            List<PresetQuestionVO> presets = Arrays.asList(
                    new PresetQuestionVO(1, "p1", "d1", "#f00"),
                    new PresetQuestionVO(2, "p2", "d2", "#0f0"),
                    new PresetQuestionVO(3, "p3", "d3", "#00f")
            );
            when(aiChatService.getPresets("compliance")).thenReturn(presets);

            mockMvc.perform(get("/api/v1/ai/presets/compliance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3));
        }

        @Test
        @WithMockUser
        @DisplayName("无效类型 -- 40001")
        void shouldReturn40001OnInvalidType() throws Exception {
            when(aiChatService.getPresets("invalid"))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "无效的助手类型: invalid"));

            mockMvc.perform(get("/api/v1/ai/presets/invalid"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40001));
        }
    }

    // ========== POST /api/v1/ai/model/switch ==========
    @Nested
    @DisplayName("POST /ai/model/switch")
    class SwitchModel {

        @Test
        @WithMockUser
        @DisplayName("切换到 doubao -- 200")
        void shouldSwitchToDoubao() throws Exception {
            Map<String, String> result = new HashMap<>();
            result.put("aiModel", "doubao");
            result.put("message", "模型切换成功");
            when(aiChatService.switchModel("doubao")).thenReturn(result);

            String body = "{\"aiModel\":\"doubao\"}";

            mockMvc.perform(post("/api/v1/ai/model/switch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.aiModel").value("doubao"))
                    .andExpect(jsonPath("$.data.message").value("模型切换成功"));
        }

        @Test
        @WithMockUser
        @DisplayName("切换到 deepseek_r1 -- 200")
        void shouldSwitchToDeepseek() throws Exception {
            Map<String, String> result = new HashMap<>();
            result.put("aiModel", "deepseek_r1");
            result.put("message", "模型切换成功");
            when(aiChatService.switchModel("deepseek_r1")).thenReturn(result);

            String body = "{\"aiModel\":\"deepseek_r1\"}";

            mockMvc.perform(post("/api/v1/ai/model/switch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.aiModel").value("deepseek_r1"));
        }

        @Test
        @WithMockUser
        @DisplayName("无效模型名 -- 40001")
        void shouldReturn40001OnInvalidModel() throws Exception {
            when(aiChatService.switchModel("gpt-4"))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "无效的AI模型: gpt-4"));

            String body = "{\"aiModel\":\"gpt-4\"}";

            mockMvc.perform(post("/api/v1/ai/model/switch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40001));
        }

        @Test
        @WithMockUser
        @DisplayName("aiModel 为空 -- 400")
        void shouldReturn400WhenModelBlank() throws Exception {
            String body = "{\"aiModel\":\"\"}";

            mockMvc.perform(post("/api/v1/ai/model/switch")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== GET /api/v1/ai/history ==========
    @Nested
    @DisplayName("GET /ai/history")
    class History {

        @Test
        @WithMockUser
        @DisplayName("无过滤条件 -- 200")
        void shouldReturnHistory() throws Exception {
            when(aiChatService.getHistory(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(new ArrayList<>(), 0L, 1, 20));

            mockMvc.perform(get("/api/v1/ai/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.page").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("按 taskId 过滤 -- 200")
        void shouldFilterByTaskId() throws Exception {
            ChatMessageVO msg = new ChatMessageVO();
            msg.setId(1L);
            msg.setRole("assistant");
            msg.setContent("response");

            when(aiChatService.getHistory(eq(42L), any(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(Collections.singletonList(msg), 1L, 1, 20));

            mockMvc.perform(get("/api/v1/ai/history")
                            .param("taskId", "42"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("按 assistantType 过滤 -- 200")
        void shouldFilterByAssistantType() throws Exception {
            when(aiChatService.getHistory(any(), any(), eq("compliance"), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(new ArrayList<>(), 0L, 1, 20));

            mockMvc.perform(get("/api/v1/ai/history")
                            .param("assistantType", "compliance"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("分页参数 page=2,size=5 -- 200")
        void shouldPaginate() throws Exception {
            when(aiChatService.getHistory(any(), any(), any(), eq(2), eq(5)))
                    .thenReturn(PageResult.of(new ArrayList<>(), 15L, 2, 5));

            mockMvc.perform(get("/api/v1/ai/history")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.size").value(5));
        }
    }
}
