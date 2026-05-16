package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.ai.dto.ChatRequest;
import com.dianjinshou.modules.ai.entity.AiConversation;
import com.dianjinshou.modules.ai.mapper.AiConversationMapper;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import com.dianjinshou.modules.ai.vo.PresetQuestionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private AiConversationMapper conversationMapper;

    @InjectMocks
    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        SecurityUser user = new SecurityUser(1L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chat_success() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("分析这场直播的互动情况");
        req.setTaskId(1L);

        ChatMessageVO result = aiChatService.chat(req);

        assertNotNull(result);
        assertEquals("assistant", result.getRole());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("运营分析"));
        // Should insert 2 messages (user + assistant)
        verify(conversationMapper, times(2)).insert(any(AiConversation.class));
    }

    @Test
    void chat_withDeepSeek() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("测试");
        req.setAiModel("deepseek_r1");

        ChatMessageVO result = aiChatService.chat(req);

        assertNotNull(result);
        assertNotNull(result.getThinking());
    }

    @Test
    void chat_complianceAssistant() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("compliance");
        req.setMessage("检查违规");

        ChatMessageVO result = aiChatService.chat(req);

        assertNotNull(result);
        assertTrue(result.getContent().contains("合规分析"));
    }

    @Test
    void chat_scriptAssistant() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("script");
        req.setMessage("优化话术");

        ChatMessageVO result = aiChatService.chat(req);

        assertNotNull(result);
        assertTrue(result.getContent().contains("话术优化"));
    }

    @Test
    void getPresets_operation() {
        List<PresetQuestionVO> presets = aiChatService.getPresets("operation");

        assertNotNull(presets);
        assertEquals(12, presets.size());
        assertEquals("拆解直播框架和风格", presets.get(0).getTitle());
        assertEquals("#2B6BFF", presets.get(0).getColor());
    }

    @Test
    void getPresets_compliance() {
        List<PresetQuestionVO> presets = aiChatService.getPresets("compliance");
        assertEquals(3, presets.size());
    }

    @Test
    void getPresets_invalidType() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.getPresets("invalid"));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void switchModel_doubao() {
        Map<String, String> result = aiChatService.switchModel("doubao");
        assertEquals("doubao", result.get("aiModel"));
        assertEquals("模型切换成功", result.get("message"));
    }

    @Test
    void switchModel_deepseek() {
        Map<String, String> result = aiChatService.switchModel("deepseek_r1");
        assertEquals("deepseek_r1", result.get("aiModel"));
    }

    @Test
    void switchModel_invalid() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> aiChatService.switchModel("gpt4"));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    void getHistory_success() {
        AiConversation conv1 = new AiConversation();
        conv1.setId(1L);
        conv1.setRole("user");
        conv1.setContent("测试问题");

        AiConversation conv2 = new AiConversation();
        conv2.setId(2L);
        conv2.setRole("assistant");
        conv2.setContent("测试回答");

        Page<AiConversation> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Arrays.asList(conv1, conv2));
        mockPage.setTotal(2);

        when(conversationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        PageResult<ChatMessageVO> result = aiChatService.getHistory(1L, null, "operation", 1, 20);

        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        assertEquals("user", result.getItems().get(0).getRole());
        assertEquals("assistant", result.getItems().get(1).getRole());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void chat_savesTaskIdContext() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");
        req.setTaskId(42L);

        aiChatService.chat(req);

        verify(conversationMapper, times(2)).insert(argThat(conv -> {
            if (conv instanceof AiConversation) {
                return ((AiConversation) conv).getTaskId().equals(42L);
            }
            return false;
        }));
    }

    @Test
    void chat_savesComparisonIdContext() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");
        req.setComparisonId(7L);

        aiChatService.chat(req);

        verify(conversationMapper, times(2)).insert(argThat(conv -> {
            if (conv instanceof AiConversation) {
                return ((AiConversation) conv).getComparisonId().equals(7L);
            }
            return false;
        }));
    }

    @Test
    void chat_savesPresetQuestionId() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");
        req.setPresetQuestionId(3);

        aiChatService.chat(req);

        verify(conversationMapper, atLeastOnce()).insert(argThat(conv -> {
            if (conv instanceof AiConversation) {
                AiConversation c = (AiConversation) conv;
                return "user".equals(c.getRole()) && Integer.valueOf(3).equals(c.getPresetQuestionId());
            }
            return false;
        }));
    }

    @Test
    void chat_deepseek_hasThinking() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");
        req.setAiModel("deepseek_r1");

        aiChatService.chat(req);

        verify(conversationMapper, atLeastOnce()).insert(argThat(conv -> {
            if (conv instanceof AiConversation) {
                AiConversation c = (AiConversation) conv;
                if ("assistant".equals(c.getRole())) {
                    return c.getThinking() != null && !c.getThinking().isEmpty();
                }
            }
            return false;
        }));
    }

    @Test
    void chat_doubao_noThinking() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");
        req.setAiModel("doubao");

        aiChatService.chat(req);

        verify(conversationMapper, atLeastOnce()).insert(argThat(conv -> {
            if (conv instanceof AiConversation) {
                AiConversation c = (AiConversation) conv;
                if ("assistant".equals(c.getRole())) {
                    return c.getThinking() == null;
                }
            }
            return false;
        }));
    }

    @Test
    void chat_defaultModel_isDoubao() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");
        // aiModel not set

        aiChatService.chat(req);

        verify(conversationMapper, atLeastOnce()).insert(argThat(conv -> {
            if (conv instanceof AiConversation) {
                AiConversation c = (AiConversation) conv;
                if ("user".equals(c.getRole())) {
                    return "doubao".equals(c.getAiModel());
                }
            }
            return false;
        }));
    }

    @Test
    void chat_truncatesHistoryOver50Rounds() {
        when(conversationMapper.insert(any(AiConversation.class))).thenReturn(1);

        List<AiConversation> existingHistory = new ArrayList<>();
        for (int i = 0; i < 102; i++) {
            AiConversation c = new AiConversation();
            c.setId((long) (i + 1));
            c.setUserId(1L);
            c.setAssistantType("operation");
            c.setRole(i % 2 == 0 ? "user" : "assistant");
            existingHistory.add(c);
        }
        when(conversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(existingHistory);

        ChatRequest req = new ChatRequest();
        req.setAssistantType("operation");
        req.setMessage("test");

        aiChatService.chat(req);

        // Should delete 102 - 100 = 2 oldest messages
        verify(conversationMapper, times(2)).deleteById(anyLong());
    }
}
