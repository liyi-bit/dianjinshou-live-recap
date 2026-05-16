package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.ai.dto.CreateSessionRequest;
import com.dianjinshou.modules.ai.dto.SendMessageRequest;
import com.dianjinshou.modules.ai.entity.AiConversation;
import com.dianjinshou.modules.ai.entity.AiSession;
import com.dianjinshou.modules.ai.mapper.AiConversationMapper;
import com.dianjinshou.modules.ai.mapper.AiSessionMapper;
import com.dianjinshou.modules.ai.vo.AiSessionDetailVO;
import com.dianjinshou.modules.ai.vo.AiSessionVO;
import com.dianjinshou.modules.ai.vo.ChatMessageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiSessionService {

    private static final Logger log = LoggerFactory.getLogger(AiSessionService.class);
    private static final Set<String> VALID_TYPES = new HashSet<String>(
            Arrays.asList("operation", "compliance", "script"));

    private final AiSessionMapper aiSessionMapper;
    private final AiConversationMapper aiConversationMapper;

    public AiSessionService(AiSessionMapper aiSessionMapper,
                            AiConversationMapper aiConversationMapper) {
        this.aiSessionMapper = aiSessionMapper;
        this.aiConversationMapper = aiConversationMapper;
    }

    public AiSessionVO createSession(CreateSessionRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!VALID_TYPES.contains(request.getAssistantType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的助手类型");
        }

        AiSession session = new AiSession();
        session.setUserId(userId);
        session.setOrgId(orgId);
        session.setAssistantType(request.getAssistantType());
        session.setMessageCount(0);
        session.setStatus("active");

        // Auto-generate title from initial message
        if (request.getInitialMessage() != null && !request.getInitialMessage().isEmpty()) {
            String title = request.getInitialMessage();
            if (title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }
            session.setTitle(title);
        } else {
            session.setTitle("新会话");
        }

        aiSessionMapper.insert(session);

        // If initial message provided, save it
        if (request.getInitialMessage() != null && !request.getInitialMessage().isEmpty()) {
            saveMessage(session.getId(), userId, "user", request.getInitialMessage(), null);
            updateSessionMessageCount(session.getId());
        }

        log.info("AI session created: id={}, type={}", session.getId(), request.getAssistantType());
        return AiSessionVO.fromEntity(session);
    }

    public Page<AiSessionVO> listSessions(int page, int size, String assistantType) {
        Long userId = SecurityContextHelper.currentUserId();

        LambdaQueryWrapper<AiSession> query = new LambdaQueryWrapper<>();
        query.eq(AiSession::getUserId, userId);
        if (assistantType != null && !assistantType.isEmpty()) {
            query.eq(AiSession::getAssistantType, assistantType);
        }
        query.orderByDesc(AiSession::getLastMessageAt);

        Page<AiSession> entityPage = aiSessionMapper.selectPage(new Page<>(page, size), query);
        Page<AiSessionVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(new ArrayList<AiSessionVO>());
        for (AiSession s : entityPage.getRecords()) {
            voPage.getRecords().add(AiSessionVO.fromEntity(s));
        }
        return voPage;
    }

    public AiSessionDetailVO getSessionDetail(Long id) {
        AiSession session = aiSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        validateOwner(session);

        AiSessionDetailVO detail = new AiSessionDetailVO();
        detail.setId(session.getId());
        detail.setAssistantType(session.getAssistantType());
        detail.setTitle(session.getTitle());
        detail.setMessageCount(session.getMessageCount());
        detail.setLastMessageAt(session.getLastMessageAt());
        detail.setStatus(session.getStatus());
        detail.setCreatedAt(session.getCreatedAt());

        // Load messages
        LambdaQueryWrapper<AiConversation> msgQuery = new LambdaQueryWrapper<>();
        msgQuery.eq(AiConversation::getSessionId, id)
                .orderByAsc(AiConversation::getCreatedAt);
        List<AiConversation> conversations = aiConversationMapper.selectList(msgQuery);
        List<ChatMessageVO> messages = new ArrayList<ChatMessageVO>();
        for (AiConversation c : conversations) {
            messages.add(ChatMessageVO.fromEntity(c));
        }
        detail.setMessages(messages);

        return detail;
    }

    public void updateTitle(Long id, String title) {
        AiSession session = aiSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        validateOwner(session);

        LambdaUpdateWrapper<AiSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiSession::getId, id).set(AiSession::getTitle, title);
        aiSessionMapper.update(null, wrapper);
    }

    public void deleteSession(Long id) {
        AiSession session = aiSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        validateOwner(session);
        aiSessionMapper.deleteById(id);
        log.info("AI session deleted: id={}", id);
    }

    public ChatMessageVO sendMessage(Long sessionId, SendMessageRequest request) {
        AiSession session = aiSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        validateOwner(session);

        Long userId = SecurityContextHelper.currentUserId();

        // Save user message
        AiConversation userMsg = saveMessage(sessionId, userId, "user",
                request.getContent(), request.getPresetQuestionId());

        // In production: call AI API (SSE streaming) and save assistant response
        // For now: placeholder response
        String aiReply = "【AI 回复占位】您的问题已收到，正在生成回复...";
        saveMessage(sessionId, userId, "assistant", aiReply, null);

        updateSessionMessageCount(sessionId);

        return ChatMessageVO.fromEntity(userMsg);
    }

    private AiConversation saveMessage(Long sessionId, Long userId, String role, String content, Integer presetQuestionId) {
        AiConversation conv = new AiConversation();
        conv.setSessionId(sessionId);
        conv.setUserId(userId);
        conv.setRole(role);
        conv.setContent(content);
        conv.setPresetQuestionId(presetQuestionId);
        conv.setDeleted(0);
        aiConversationMapper.insert(conv);
        return conv;
    }

    private void updateSessionMessageCount(Long sessionId) {
        long count = aiConversationMapper.selectCount(
                new LambdaQueryWrapper<AiConversation>().eq(AiConversation::getSessionId, sessionId));
        LambdaUpdateWrapper<AiSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiSession::getId, sessionId)
                .set(AiSession::getMessageCount, (int) count)
                .set(AiSession::getLastMessageAt, LocalDateTime.now());
        aiSessionMapper.update(null, wrapper);
    }

    private void validateOwner(AiSession session) {
        Long userId = SecurityContextHelper.currentUserId();
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此会话");
        }
    }
}
