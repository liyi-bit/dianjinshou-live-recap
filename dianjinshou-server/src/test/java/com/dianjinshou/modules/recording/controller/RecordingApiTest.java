package com.dianjinshou.modules.recording.controller;

import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.recording.dto.RecordingQueryRequest;
import com.dianjinshou.modules.recording.dto.RenameRequest;
import com.dianjinshou.modules.recording.service.RecordingService;
import com.dianjinshou.modules.recording.vo.RecordingListVO;
import com.dianjinshou.modules.recording.vo.RecordingVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RecordingController API 测试
 *
 * 使用 @WebMvcTest 切片测试，Mock Service 层。
 */
@WebMvcTest(RecordingController.class)
@DisplayName("RecordingController API 测试")
class RecordingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecordingService recordingService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/recordings";

    @Test
    @WithMockUser
    @DisplayName("B01: GET /recordings 分页列表")
    void listRecordings_success() throws Exception {
        PageResult<RecordingListVO> pageResult = PageResult.of(new ArrayList<>(), 0, 1, 10);
        when(recordingService.list(any(RecordingQueryRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("B02: GET /recordings/{id} 详情")
    void detailRecording_success() throws Exception {
        RecordingVO vo = new RecordingVO();
        vo.setId(1L);
        vo.setLocalFileName("测试录制");
        vo.setStatus("completed");
        when(recordingService.detail(1L)).thenReturn(vo);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.localFileName").value("测试录制"));
    }

    @Test
    @WithMockUser
    @DisplayName("B03: PUT /recordings/{id}/name 重命名成功")
    void renameRecording_success() throws Exception {
        RecordingVO vo = new RecordingVO();
        vo.setId(1L);
        vo.setLocalFileName("新名称");
        when(recordingService.rename(eq(1L), any(RenameRequest.class))).thenReturn(vo);

        Map<String, String> body = new HashMap<>();
        body.put("name", "新名称");

        mockMvc.perform(put(BASE_URL + "/1/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.localFileName").value("新名称"));
    }

    @Test
    @WithMockUser
    @DisplayName("B04: PUT /recordings/{id}/name 空名称返回 400")
    void renameRecording_emptyName() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("name", "");

        mockMvc.perform(put(BASE_URL + "/1/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("B05: POST /recordings/{id}/export 导出")
    void exportRecording_success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("url", null);
        data.put("message", "导出功能开发中");
        when(recordingService.export(1L)).thenReturn(data);

        mockMvc.perform(post(BASE_URL + "/1/export")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("导出功能开发中"));
    }

    @Test
    @WithMockUser
    @DisplayName("B06: DELETE /recordings 批量删除")
    void batchDeleteRecordings_success() throws Exception {
        when(recordingService.batchDelete(any())).thenReturn(2);

        Map<String, Object> body = new HashMap<>();
        body.put("ids", Arrays.asList(1L, 2L));

        mockMvc.perform(delete(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletedCount").value(2));
    }
}
