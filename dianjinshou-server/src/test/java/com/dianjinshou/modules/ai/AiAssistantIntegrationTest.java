package com.dianjinshou.modules.ai;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.ai.dto.ComplianceCheckRequest;
import com.dianjinshou.modules.ai.dto.CreateSessionRequest;
import com.dianjinshou.modules.ai.dto.GenerateScriptRequest;
import com.dianjinshou.modules.ai.dto.SendMessageRequest;
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
import com.dianjinshou.modules.ai.service.AiSessionService;
import com.dianjinshou.modules.ai.service.ComplianceCheckService;
import com.dianjinshou.modules.ai.service.ScriptAssistantService;
import com.dianjinshou.modules.ai.service.SensitiveWordEngine;
import com.dianjinshou.modules.ai.vo.AiSessionVO;
import com.dianjinshou.modules.ai.vo.ComplianceCheckResultVO;
import com.dianjinshou.modules.ai.vo.ScriptTemplateVO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAssistantIntegrationTest {

    @Mock private AiSessionMapper aiSessionMapper;
    @Mock private AiConversationMapper aiConversationMapper;
    @Mock private ScriptTemplateMapper scriptTemplateMapper;
    @Mock private ScriptGenerationMapper scriptGenerationMapper;
    @Mock private SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    @Mock private SensitiveWordEngine sensitiveWordEngine;

    private AiSessionService sessionService;
    private ComplianceCheckService complianceService;
    private ScriptAssistantService scriptService;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.ai.integ.AiSessionMapper");
            TableInfoHelper.initTableInfo(a1, AiSession.class);
            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.ai.integ.AiConversationMapper");
            TableInfoHelper.initTableInfo(a2, AiConversation.class);
            MapperBuilderAssistant a3 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a3.setCurrentNamespace("com.dianjinshou.modules.ai.integ.ScriptTemplateMapper");
            TableInfoHelper.initTableInfo(a3, ScriptTemplate.class);
            MapperBuilderAssistant a4 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a4.setCurrentNamespace("com.dianjinshou.modules.ai.integ.ScriptGenerationMapper");
            TableInfoHelper.initTableInfo(a4, ScriptGeneration.class);
            MapperBuilderAssistant a5 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a5.setCurrentNamespace("com.dianjinshou.modules.ai.integ.SensitiveWordLibraryMapper");
            TableInfoHelper.initTableInfo(a5, SensitiveWordLibrary.class);
        } catch (Exception ignored) {}
    }

    @BeforeEach
    void setUp() {
        sessionService = new AiSessionService(aiSessionMapper, aiConversationMapper);
        complianceService = new ComplianceCheckService(sensitiveWordLibraryMapper, sensitiveWordEngine);
        scriptService = new ScriptAssistantService(scriptTemplateMapper, scriptGenerationMapper);
        setCurrentUser(1L, "super_admin", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private AiSession makeSession(Long id) {
        AiSession s = new AiSession();
        s.setId(id); s.setUserId(1L); s.setOrgId(100L);
        s.setAssistantType("operation"); s.setStatus("active");
        return s;
    }

    private ComplianceCheckRequest makeComplianceReq(String text, String scenario, String platform) {
        ComplianceCheckRequest req = new ComplianceCheckRequest();
        req.setTextContent(text);
        req.setScenario(scenario);
        req.setPlatform(platform);
        return req;
    }

    // 1. Create session
    @Test
    void session_create() {
        when(aiSessionMapper.insert(any(AiSession.class))).thenReturn(1);
        CreateSessionRequest req = new CreateSessionRequest();
        req.setAssistantType("operation");
        AiSessionVO vo = sessionService.createSession(req);
        assertNotNull(vo);
    }

    // 2. Send message
    @Test
    void session_sendMessage() {
        when(aiSessionMapper.selectById(1L)).thenReturn(makeSession(1L));
        when(aiConversationMapper.insert(any(AiConversation.class))).thenReturn(1);
        when(aiConversationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(aiSessionMapper.update(isNull(), any())).thenReturn(1);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("你好");
        sessionService.sendMessage(1L, req);
        verify(aiConversationMapper, atLeast(1)).insert(any(AiConversation.class));
    }

    // 3. Delete session
    @Test
    void session_delete() {
        when(aiSessionMapper.selectById(1L)).thenReturn(makeSession(1L));
        when(aiSessionMapper.deleteById(1L)).thenReturn(1);
        sessionService.deleteSession(1L);
        verify(aiSessionMapper).deleteById(1L);
    }

    // 4. List sessions pagination
    @Test
    void session_listPagination() {
        Page<AiSession> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(makeSession(1L)));
        when(aiSessionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<AiSessionVO> result = sessionService.listSessions(1, 20, "operation");
        assertEquals(1, result.getTotal());
    }

    // 5. Delete session not found
    @Test
    void session_deleteNotFound_throws() {
        when(aiSessionMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> sessionService.deleteSession(999L));
    }

    // 6. Cross-org session access
    @Test
    void session_crossOrg_throws() {
        setCurrentUser(2L, "operator", 200L);
        when(aiSessionMapper.selectById(1L)).thenReturn(makeSession(1L)); // orgId=100
        assertThrows(BusinessException.class, () -> sessionService.deleteSession(1L));
    }

    // 7. Compliance no hits
    @Test
    void compliance_noHits() {
        when(sensitiveWordEngine.scan(anyString(), isNull())).thenReturn(Collections.emptyList());
        ComplianceCheckResultVO r = complianceService.check(makeComplianceReq("正常文本", "live_speech", null));
        assertEquals(Integer.valueOf(0), r.getRiskScore());
        assertTrue(r.getHitWords().isEmpty());
    }

    // 8. Compliance with hits
    @Test
    void compliance_withHits() {
        SensitiveWordEngine.MatchResult hit = new SensitiveWordEngine.MatchResult();
        hit.setWord("违禁"); hit.setPosition(5); hit.setCategory("涉政"); hit.setRiskLevel(5);
        when(sensitiveWordEngine.scan(anyString(), isNull())).thenReturn(Collections.singletonList(hit));

        ComplianceCheckResultVO r = complianceService.check(makeComplianceReq("含违禁词", "live_speech", null));
        assertTrue(r.getRiskScore() > 0);
        assertEquals(1, r.getHitWords().size());
    }

    // 9. Compliance with platform
    @Test
    void compliance_withPlatform() {
        when(sensitiveWordEngine.scan(anyString(), eq("kuaishou"))).thenReturn(Collections.emptyList());
        complianceService.check(makeComplianceReq("测试", "live_speech", "kuaishou"));
        verify(sensitiveWordEngine).scan(anyString(), eq("kuaishou"));
    }

    // 10. Compliance all scenarios
    @Test
    void compliance_allScenarios() {
        when(sensitiveWordEngine.scan(anyString(), isNull())).thenReturn(Collections.emptyList());
        for (String s : new String[]{"live_speech", "product_desc", "ad_copy", "comment"}) {
            assertNotNull(complianceService.check(makeComplianceReq("测试", s, null)));
        }
    }

    // 11. Compliance invalid scenario
    @Test
    void compliance_invalidScenario_throws() {
        assertThrows(BusinessException.class, () -> complianceService.check(makeComplianceReq("test", "invalid", null)));
    }

    // 12. Add sensitive word
    @Test
    void compliance_addWord() {
        when(sensitiveWordLibraryMapper.insert(any(SensitiveWordLibrary.class))).thenReturn(1);
        doNothing().when(sensitiveWordEngine).reload();
        SensitiveWordLibrary w = new SensitiveWordLibrary();
        w.setWord("测试词"); w.setCategory("其他"); w.setRiskLevel(3);
        complianceService.addWord(w);
        verify(sensitiveWordEngine).reload();
    }

    // 13. Delete system word fails
    @Test
    void compliance_deleteSystemWord_throws() {
        SensitiveWordLibrary w = new SensitiveWordLibrary();
        w.setId(1L); w.setSource("system");
        when(sensitiveWordLibraryMapper.selectById(1L)).thenReturn(w);
        assertThrows(BusinessException.class, () -> complianceService.deleteWord(1L));
    }

    // 14. Delete custom word
    @Test
    void compliance_deleteCustomWord() {
        SensitiveWordLibrary w = new SensitiveWordLibrary();
        w.setId(1L); w.setSource("custom");
        when(sensitiveWordLibraryMapper.selectById(1L)).thenReturn(w);
        when(sensitiveWordLibraryMapper.deleteById(1L)).thenReturn(1);
        doNothing().when(sensitiveWordEngine).reload();
        complianceService.deleteWord(1L);
        verify(sensitiveWordLibraryMapper).deleteById(1L);
    }

    // 15. Script generate
    @Test
    void script_generate() {
        ScriptTemplate t = new ScriptTemplate();
        t.setId(1L); t.setName("欢迎话术"); t.setIsActive(1);
        t.setPromptTemplate("生成{product}欢迎话术");
        when(scriptTemplateMapper.selectById(1L)).thenReturn(t);
        when(scriptGenerationMapper.insert(any(ScriptGeneration.class))).thenReturn(1);

        GenerateScriptRequest req = new GenerateScriptRequest();
        req.setTemplateId(1L);
        ScriptGeneration gen = scriptService.generate(req);
        assertNotNull(gen.getGeneratedText());
    }

    // 16. Script rate
    @Test
    void script_rate() {
        ScriptGeneration g = new ScriptGeneration();
        g.setId(1L); g.setUserId(1L);
        when(scriptGenerationMapper.selectById(1L)).thenReturn(g);
        when(scriptGenerationMapper.updateById(any(ScriptGeneration.class))).thenReturn(1);
        scriptService.rate(1L, 4);
        verify(scriptGenerationMapper).updateById(any(ScriptGeneration.class));
    }

    // 17. Script rate invalid
    @Test
    void script_rateInvalid_throws() {
        assertThrows(BusinessException.class, () -> scriptService.rate(1L, 6));
    }

    // 18. Script inactive template
    @Test
    void script_inactiveTemplate_throws() {
        ScriptTemplate t = new ScriptTemplate();
        t.setId(1L); t.setIsActive(0);
        when(scriptTemplateMapper.selectById(1L)).thenReturn(t);
        GenerateScriptRequest req = new GenerateScriptRequest();
        req.setTemplateId(1L);
        assertThrows(BusinessException.class, () -> scriptService.generate(req));
    }
}
