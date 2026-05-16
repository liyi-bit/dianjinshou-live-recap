package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.admin.vo.AiStatsVO;
import com.dianjinshou.modules.ai.entity.AiConversation;
import com.dianjinshou.modules.ai.entity.AiSession;
import com.dianjinshou.modules.ai.entity.ScriptGeneration;
import com.dianjinshou.modules.ai.entity.ScriptTemplate;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.mapper.AiConversationMapper;
import com.dianjinshou.modules.ai.mapper.AiSessionMapper;
import com.dianjinshou.modules.ai.mapper.ScriptGenerationMapper;
import com.dianjinshou.modules.ai.mapper.ScriptTemplateMapper;
import com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper;
import com.dianjinshou.modules.ai.service.SensitiveWordEngine;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAiServiceTest {

    @Mock private AiSessionMapper aiSessionMapper;
    @Mock private AiConversationMapper aiConversationMapper;
    @Mock private SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    @Mock private ScriptTemplateMapper scriptTemplateMapper;
    @Mock private ScriptGenerationMapper scriptGenerationMapper;
    @Mock private SensitiveWordEngine sensitiveWordEngine;

    private AdminAiService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.admin.AiSessionMapper");
            TableInfoHelper.initTableInfo(a1, AiSession.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.admin.ScriptTemplateMapper");
            TableInfoHelper.initTableInfo(a2, ScriptTemplate.class);

            MapperBuilderAssistant a3 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a3.setCurrentNamespace("com.dianjinshou.modules.admin.ScriptGenerationMapper");
            TableInfoHelper.initTableInfo(a3, ScriptGeneration.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new AdminAiService(aiSessionMapper, aiConversationMapper,
                sensitiveWordLibraryMapper, scriptTemplateMapper,
                scriptGenerationMapper, sensitiveWordEngine);
        setCurrentUser(1L, "super_admin", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getStats_success() {
        when(aiSessionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);
        when(aiConversationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(50L);
        when(scriptGenerationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(20L);
        when(sensitiveWordEngine.getWordCount()).thenReturn(1000);

        AiStatsVO stats = service.getStats();

        assertEquals(10L, stats.getTotalSessions());
        assertEquals(50L, stats.getTotalMessages());
        assertEquals(1000, stats.getSensitiveWordCount());
    }

    @Test
    void getStats_nonAdmin_throws() {
        setCurrentUser(2L, "operator", 100L);
        assertThrows(BusinessException.class, () -> service.getStats());
    }

    @Test
    void toggleScriptTemplate_success() {
        ScriptTemplate t = new ScriptTemplate();
        t.setId(1L);
        t.setIsActive(1);
        when(scriptTemplateMapper.selectById(1L)).thenReturn(t);
        when(scriptTemplateMapper.updateById(any(ScriptTemplate.class))).thenReturn(1);

        service.toggleScriptTemplate(1L);

        assertEquals(0, t.getIsActive());
        verify(scriptTemplateMapper).updateById(t);
    }

    @Test
    void toggleScriptTemplate_notFound_throws() {
        when(scriptTemplateMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.toggleScriptTemplate(999L));
    }
}
