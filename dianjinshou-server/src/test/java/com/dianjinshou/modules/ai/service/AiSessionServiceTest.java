package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.ai.dto.CreateSessionRequest;
import com.dianjinshou.modules.ai.dto.SendMessageRequest;
import com.dianjinshou.modules.ai.entity.AiConversation;
import com.dianjinshou.modules.ai.entity.AiSession;
import com.dianjinshou.modules.ai.mapper.AiConversationMapper;
import com.dianjinshou.modules.ai.mapper.AiSessionMapper;
import com.dianjinshou.modules.ai.vo.AiSessionDetailVO;
import com.dianjinshou.modules.ai.vo.AiSessionVO;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSessionServiceTest {

    @Mock
    private AiSessionMapper aiSessionMapper;
    @Mock
    private AiConversationMapper aiConversationMapper;

    private AiSessionService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.ai.mapper.AiSessionMapper");
            TableInfoHelper.initTableInfo(a1, AiSession.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.ai.mapper.AiConversationMapper2");
            TableInfoHelper.initTableInfo(a2, AiConversation.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new AiSessionService(aiSessionMapper, aiConversationMapper);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createSession_success() {
        when(aiSessionMapper.insert(any(AiSession.class))).thenAnswer(inv -> {
            AiSession s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });

        CreateSessionRequest req = new CreateSessionRequest();
        req.setAssistantType("operation");
        req.setInitialMessage("帮我分析一下今天的直播数据");

        AiSessionVO result = service.createSession(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertTrue(result.getTitle().contains("帮我分析"));
    }

    @Test
    void createSession_invalidType_rejected() {
        CreateSessionRequest req = new CreateSessionRequest();
        req.setAssistantType("invalid");

        assertThrows(BusinessException.class, () -> service.createSession(req));
    }

    @Test
    void listSessions_success() {
        AiSession session = new AiSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setAssistantType("operation");
        session.setTitle("测试会话");
        session.setMessageCount(5);
        session.setStatus("active");

        Page<AiSession> page = new Page<>(1, 20, 1);
        page.setRecords(Arrays.asList(session));
        when(aiSessionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<AiSessionVO> result = service.listSessions(1, 20, "operation");

        assertEquals(1, result.getRecords().size());
        assertEquals("测试会话", result.getRecords().get(0).getTitle());
    }

    @Test
    void getSessionDetail_success() {
        AiSession session = new AiSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setAssistantType("operation");
        session.setTitle("测试");
        session.setMessageCount(1);
        session.setStatus("active");
        when(aiSessionMapper.selectById(1L)).thenReturn(session);
        when(aiConversationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        AiSessionDetailVO detail = service.getSessionDetail(1L);

        assertNotNull(detail);
        assertEquals("测试", detail.getTitle());
        assertNotNull(detail.getMessages());
    }

    @Test
    void getSessionDetail_notFound() {
        when(aiSessionMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getSessionDetail(999L));
    }

    @Test
    void sendMessage_success() {
        AiSession session = new AiSession();
        session.setId(1L);
        session.setUserId(1L);
        when(aiSessionMapper.selectById(1L)).thenReturn(session);
        when(aiConversationMapper.insert(any(AiConversation.class))).thenAnswer(inv -> {
            AiConversation c = inv.getArgument(0);
            c.setId(10L);
            return 1;
        });
        when(aiConversationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(aiSessionMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("测试消息");

        ChatMessageVO result = service.sendMessage(1L, req);

        assertNotNull(result);
        verify(aiConversationMapper, times(2)).insert(any(AiConversation.class));
    }

    @Test
    void deleteSession_success() {
        AiSession session = new AiSession();
        session.setId(1L);
        session.setUserId(1L);
        when(aiSessionMapper.selectById(1L)).thenReturn(session);
        when(aiSessionMapper.deleteById(1L)).thenReturn(1);

        service.deleteSession(1L);

        verify(aiSessionMapper).deleteById(1L);
    }
}
