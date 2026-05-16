package com.dianjinshou.modules.streamer.controller;

import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.streamer.dto.CreateStreamerRequest;
import com.dianjinshou.modules.streamer.dto.StreamerQueryRequest;
import com.dianjinshou.modules.streamer.dto.UpdateStreamerRequest;
import com.dianjinshou.modules.streamer.service.StreamerService;
import com.dianjinshou.modules.streamer.vo.StreamerListVO;
import com.dianjinshou.modules.streamer.vo.StreamerStatsVO;
import com.dianjinshou.modules.streamer.vo.StreamerVO;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StreamerController API 测试
 *
 * 使用 @WebMvcTest 切片测试，Mock Service 层。
 * 注意：实际运行需要在 Spring Boot 测试环境下，可能需要排除安全配置或添加 @Import。
 */
@WebMvcTest(StreamerController.class)
@DisplayName("StreamerController API 测试")
class StreamerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StreamerService streamerService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/streamers";

    @Test
    @WithMockUser
    @DisplayName("A01: POST /streamers 创建直播间成功")
    void createStreamer_success() throws Exception {
        StreamerVO vo = new StreamerVO();
        vo.setId(1L);
        vo.setPlatform("douyin");
        vo.setAnchorName("测试主播");
        when(streamerService.create(any(CreateStreamerRequest.class))).thenReturn(vo);

        Map<String, Object> body = new HashMap<>();
        body.put("platform", "douyin");
        body.put("accountId", "dy12345");
        body.put("anchorName", "测试主播");

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.platform").value("douyin"))
                .andExpect(jsonPath("$.data.anchorName").value("测试主播"));
    }

    @Test
    @WithMockUser
    @DisplayName("A02: POST /streamers platform 为空返回 400")
    void createStreamer_missingPlatform() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", "dy12345");

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("A03: GET /streamers 分页列表")
    void listStreamers_success() throws Exception {
        PageResult<StreamerListVO> pageResult = PageResult.of(new ArrayList<>(), 0, 1, 10);
        when(streamerService.list(any(StreamerQueryRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("A04: GET /streamers/{id} 详情")
    void detailStreamer_success() throws Exception {
        StreamerVO vo = new StreamerVO();
        vo.setId(1L);
        vo.setPlatform("douyin");
        when(streamerService.detail(1L)).thenReturn(vo);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("A05: PUT /streamers/{id} 更新")
    void updateStreamer_success() throws Exception {
        StreamerVO vo = new StreamerVO();
        vo.setId(1L);
        vo.setAnchorName("新名称");
        when(streamerService.update(eq(1L), any(UpdateStreamerRequest.class))).thenReturn(vo);

        Map<String, Object> body = new HashMap<>();
        body.put("anchorName", "新名称");

        mockMvc.perform(put(BASE_URL + "/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.anchorName").value("新名称"));
    }

    @Test
    @WithMockUser
    @DisplayName("A06: DELETE /streamers/{id} 删除")
    void deleteStreamer_success() throws Exception {
        when(streamerService.delete(1L)).thenReturn(0);

        mockMvc.perform(delete(BASE_URL + "/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletedRecordings").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("A07: POST /streamers/{id}/monitor/start 开启监控")
    void startMonitor_success() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/monitor/start")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    @DisplayName("A08: POST /streamers/{id}/monitor/stop 停止监控")
    void stopMonitor_success() throws Exception {
        mockMvc.perform(post(BASE_URL + "/1/monitor/stop")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    @DisplayName("A09: GET /streamers/stats 统计")
    void stats_success() throws Exception {
        StreamerStatsVO statsVO = new StreamerStatsVO();
        statsVO.setTotal(5);
        statsVO.setMonitoring(2);
        statsVO.setOwnCount(3);
        when(streamerService.stats()).thenReturn(statsVO);

        mockMvc.perform(get(BASE_URL + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.monitoring").value(2));
    }
}
