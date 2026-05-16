package com.dianjinshou.modules.recap.controller;

import com.dianjinshou.modules.recap.service.AnalysisService;
import com.dianjinshou.modules.recap.service.DiagnosisService;
import com.dianjinshou.modules.recap.vo.AnalysisTaskCreateVO;
import com.dianjinshou.modules.recap.vo.AnalysisTaskVO;
import com.dianjinshou.modules.recap.vo.DiagnosisReportVO;
import com.dianjinshou.modules.recap.vo.KeywordListVO;
import com.dianjinshou.modules.recap.vo.NoteVO;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T19 AI分析任务 API 接口测试
 * 使用 Spring MockMvc 测试 Controller 层请求/响应映射
 */
@WebMvcTest({AnalysisController.class, DiagnosisController.class})
@DisplayName("T19: AI分析任务 API 接口测试")
class AnalysisApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AnalysisService analysisService;
    @MockBean private DiagnosisService diagnosisService;

    // ========== POST /api/v1/analysis/full ==========
    @Nested
    @DisplayName("POST /analysis/full")
    class CreateFullAnalysis {

        @Test
        @WithMockUser
        @DisplayName("正常创建 -- 200")
        void shouldReturn200OnValidRequest() throws Exception {
            when(analysisService.createFullAnalysis(any()))
                    .thenReturn(AnalysisTaskCreateVO.of(1L, "pending"));

            String body = "{\"recordingId\":1,\"industry\":\"beauty\"}";

            mockMvc.perform(post("/api/v1/analysis/full")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.taskId").value(1))
                    .andExpect(jsonPath("$.data.status").value("pending"));
        }

        @Test
        @WithMockUser
        @DisplayName("recordingId 为 null -- 400 参数校验失败")
        void shouldReturn400WhenRecordingIdNull() throws Exception {
            String body = "{\"industry\":\"beauty\"}";

            mockMvc.perform(post("/api/v1/analysis/full")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("录制不存在 -- 40400")
        void shouldReturn40400WhenRecordingNotFound() throws Exception {
            when(analysisService.createFullAnalysis(any()))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "录制记录不存在"));

            String body = "{\"recordingId\":999}";

            mockMvc.perform(post("/api/v1/analysis/full")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40400));
        }
    }

    // ========== POST /api/v1/analysis/clip ==========
    @Nested
    @DisplayName("POST /analysis/clip")
    class CreateClipAnalysis {

        @Test
        @WithMockUser
        @DisplayName("正常创建切片分析 -- 200")
        void shouldReturn200OnValidClipRequest() throws Exception {
            when(analysisService.createClipAnalysis(any()))
                    .thenReturn(AnalysisTaskCreateVO.of(2L, "pending"));

            String body = "{\"recordingId\":1,\"clipStart\":0,\"clipEnd\":60,\"clipCategory\":\"RETENTION\"}";

            mockMvc.perform(post("/api/v1/analysis/clip")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taskId").value(2));
        }

        @Test
        @WithMockUser
        @DisplayName("clipCategory 为空 -- 400")
        void shouldReturn400WhenClipCategoryBlank() throws Exception {
            String body = "{\"recordingId\":1,\"clipStart\":0,\"clipEnd\":60,\"clipCategory\":\"\"}";

            mockMvc.perform(post("/api/v1/analysis/clip")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("无效切片分类 -- 40001")
        void shouldReturn40001OnInvalidClipCategory() throws Exception {
            when(analysisService.createClipAnalysis(any()))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "无效的切片分类: WRONG"));

            String body = "{\"recordingId\":1,\"clipStart\":0,\"clipEnd\":60,\"clipCategory\":\"WRONG\"}";

            mockMvc.perform(post("/api/v1/analysis/clip")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40001));
        }

        @Test
        @WithMockUser
        @DisplayName("clipStart >= clipEnd -- 40001")
        void shouldReturn40001WhenStartGteEnd() throws Exception {
            when(analysisService.createClipAnalysis(any()))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "切片开始时间必须小于结束时间"));

            String body = "{\"recordingId\":1,\"clipStart\":100,\"clipEnd\":50,\"clipCategory\":\"RETENTION\"}";

            mockMvc.perform(post("/api/v1/analysis/clip")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40001));
        }
    }

    // ========== GET /api/v1/analysis/{id} ==========
    @Nested
    @DisplayName("GET /analysis/{id}")
    class GetDetail {

        @Test
        @WithMockUser
        @DisplayName("正常获取详情 -- 200")
        void shouldReturnDetail() throws Exception {
            AnalysisTaskVO vo = new AnalysisTaskVO();
            vo.setId(1L);
            vo.setStatus("completed");
            vo.setType("full");
            when(analysisService.detail(1L)).thenReturn(vo);

            mockMvc.perform(get("/api/v1/analysis/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("completed"));
        }

        @Test
        @WithMockUser
        @DisplayName("任务不存在 -- 40400")
        void shouldReturn40400() throws Exception {
            when(analysisService.detail(999L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在"));

            mockMvc.perform(get("/api/v1/analysis/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40400));
        }
    }

    // ========== GET /api/v1/analysis/{id}/paragraphs ==========
    @Nested
    @DisplayName("GET /analysis/{id}/paragraphs")
    class GetParagraphs {

        @Test
        @WithMockUser
        @DisplayName("正常分页 -- 200")
        void shouldReturnParagraphs() throws Exception {
            when(analysisService.getParagraphs(eq(1L), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(new ArrayList<>(), 0L, 1, 100));

            mockMvc.perform(get("/api/v1/analysis/1/paragraphs")
                            .param("page", "1")
                            .param("size", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }

    // ========== GET /api/v1/analysis/{id}/keywords ==========
    @Nested
    @DisplayName("GET /analysis/{id}/keywords")
    class GetKeywords {

        @Test
        @WithMockUser
        @DisplayName("正常获取关键词 -- 200")
        void shouldReturnKeywords() throws Exception {
            Map<String, Integer> stats = new HashMap<>();
            stats.put("totalOperational", 5);
            stats.put("totalSensitive", 2);
            KeywordListVO vo = KeywordListVO.of(new ArrayList<>(), stats, 7);

            when(analysisService.getKeywords(eq(1L), any(), any(), anyInt(), anyInt()))
                    .thenReturn(vo);

            mockMvc.perform(get("/api/v1/analysis/1/keywords"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stats.totalOperational").value(5))
                    .andExpect(jsonPath("$.data.stats.totalSensitive").value(2));
        }
    }

    // ========== POST /api/v1/analysis/{id}/optimization ==========
    @Nested
    @DisplayName("POST /analysis/{id}/optimization")
    class SaveOptimization {

        @Test
        @WithMockUser
        @DisplayName("正常保存 -- 200")
        void shouldSaveOptimization() throws Exception {
            String body = "{\"action\":\"test action\",\"goal\":\"test goal\"}";

            mockMvc.perform(post("/api/v1/analysis/1/optimization")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser
        @DisplayName("action 为空 -- 400")
        void shouldReturn400WhenActionBlank() throws Exception {
            String body = "{\"action\":\"\",\"goal\":\"test\"}";

            mockMvc.perform(post("/api/v1/analysis/1/optimization")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("action 超过 500 字 -- 400")
        void shouldReturn400WhenActionTooLong() throws Exception {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 501; i++) sb.append('a');
            String longAction = sb.toString();
            String body = "{\"action\":\"" + longAction + "\",\"goal\":\"test\"}";

            mockMvc.perform(post("/api/v1/analysis/1/optimization")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("goal 超过 500 字 -- 400")
        void shouldReturn400WhenGoalTooLong() throws Exception {
            StringBuilder sbg = new StringBuilder();
            for (int i = 0; i < 501; i++) sbg.append('g');
            String longGoal = sbg.toString();
            String body = "{\"action\":\"valid\",\"goal\":\"" + longGoal + "\"}";

            mockMvc.perform(post("/api/v1/analysis/1/optimization")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== GET/PUT /api/v1/analysis/{id}/notes ==========
    @Nested
    @DisplayName("笔记 CRUD")
    class Notes {

        @Test
        @WithMockUser
        @DisplayName("GET 笔记 -- 200")
        void shouldReturnNotes() throws Exception {
            NoteVO noteVO = new NoteVO();
            noteVO.setTabType("MINUTE_SEGMENTS");
            noteVO.setContentHtml("<p>test</p>");
            when(analysisService.getNotes(eq(1L), eq("MINUTE_SEGMENTS"))).thenReturn(noteVO);

            mockMvc.perform(get("/api/v1/analysis/1/notes")
                            .param("tabType", "MINUTE_SEGMENTS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tabType").value("MINUTE_SEGMENTS"));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT 保存笔记 -- 200")
        void shouldSaveNotes() throws Exception {
            NoteVO noteVO = new NoteVO();
            noteVO.setTabType("MINUTE_SEGMENTS");
            noteVO.setContentHtml("<p>saved</p>");
            when(analysisService.saveNotes(eq(1L), any())).thenReturn(noteVO);

            String body = "{\"tabType\":\"MINUTE_SEGMENTS\",\"contentHtml\":\"<p>saved</p>\"}";

            mockMvc.perform(put("/api/v1/analysis/1/notes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contentHtml").value("<p>saved</p>"));
        }

        @Test
        @WithMockUser
        @DisplayName("PUT tabType 为空 -- 400")
        void shouldReturn400WhenTabTypeBlank() throws Exception {
            String body = "{\"tabType\":\"\",\"contentHtml\":\"<p>test</p>\"}";

            mockMvc.perform(put("/api/v1/analysis/1/notes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /api/v1/analysis/{id}/re-analyze ==========
    @Nested
    @DisplayName("POST /analysis/{id}/re-analyze")
    class ReAnalyze {

        @Test
        @WithMockUser
        @DisplayName("正常重新分析 -- 200")
        void shouldReAnalyze() throws Exception {
            when(analysisService.reAnalyze(1L))
                    .thenReturn(AnalysisTaskCreateVO.of(1L, "pending"));

            mockMvc.perform(post("/api/v1/analysis/1/re-analyze")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("pending"));
        }
    }

    // ========== POST /api/v1/analysis/{id}/cancel ==========
    @Nested
    @DisplayName("POST /analysis/{id}/cancel")
    class Cancel {

        @Test
        @WithMockUser
        @DisplayName("正常取消 -- 200")
        void shouldCancel() throws Exception {
            mockMvc.perform(post("/api/v1/analysis/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @WithMockUser
        @DisplayName("取消已完成任务 -- 40001")
        void shouldReturn40001WhenTaskCompleted() throws Exception {
            doThrow(new BusinessException(ErrorCode.PARAM_ERROR, "任务已结束，无法取消"))
                    .when(analysisService).cancel(1L);

            mockMvc.perform(post("/api/v1/analysis/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40001));
        }
    }

    // ========== DiagnosisController ==========
    @Nested
    @DisplayName("诊断报告接口")
    class Diagnosis {

        @Test
        @WithMockUser
        @DisplayName("POST 生成诊断 -- 200")
        void shouldGenerateDiagnosis() throws Exception {
            DiagnosisReportVO vo = new DiagnosisReportVO();
            vo.setTaskId(1L);
            vo.setStatus("completed");
            vo.setOverallScore(77);
            when(diagnosisService.generateDiagnosis(1L)).thenReturn(vo);

            mockMvc.perform(post("/api/v1/analysis/1/diagnosis")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.overallScore").value(77));
        }

        @Test
        @WithMockUser
        @DisplayName("GET 诊断报告 -- 200")
        void shouldGetDiagnosisReport() throws Exception {
            DiagnosisReportVO vo = new DiagnosisReportVO();
            vo.setTaskId(1L);
            vo.setStatus("not_generated");
            when(diagnosisService.getDiagnosis(1L)).thenReturn(vo);

            mockMvc.perform(get("/api/v1/analysis/1/diagnosis-report"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("not_generated"));
        }

        @Test
        @WithMockUser
        @DisplayName("POST 生成诊断 -- 任务未完成 40002")
        void shouldReturn40002WhenNotCompleted() throws Exception {
            when(diagnosisService.generateDiagnosis(1L))
                    .thenThrow(new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "分析任务尚未完成"));

            mockMvc.perform(post("/api/v1/analysis/1/diagnosis")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40002));
        }
    }

    // ========== GET /api/v1/analysis/{id}/progress ==========
    @Test
    @WithMockUser
    @DisplayName("GET progress placeholder -- 200")
    void shouldReturnProgressPlaceholder() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
