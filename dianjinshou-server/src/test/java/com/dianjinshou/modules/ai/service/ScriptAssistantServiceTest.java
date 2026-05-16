package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.ai.dto.GenerateScriptRequest;
import com.dianjinshou.modules.ai.entity.ScriptGeneration;
import com.dianjinshou.modules.ai.entity.ScriptTemplate;
import com.dianjinshou.modules.ai.mapper.ScriptGenerationMapper;
import com.dianjinshou.modules.ai.mapper.ScriptTemplateMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptAssistantServiceTest {

    @Mock
    private ScriptTemplateMapper scriptTemplateMapper;
    @Mock
    private ScriptGenerationMapper scriptGenerationMapper;

    private ScriptAssistantService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a1 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a1.setCurrentNamespace("com.dianjinshou.modules.ai.mapper.ScriptTemplateMapper");
            TableInfoHelper.initTableInfo(a1, ScriptTemplate.class);

            MapperBuilderAssistant a2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a2.setCurrentNamespace("com.dianjinshou.modules.ai.mapper.ScriptGenerationMapper");
            TableInfoHelper.initTableInfo(a2, ScriptGeneration.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new ScriptAssistantService(scriptTemplateMapper, scriptGenerationMapper);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void listTemplates_success() {
        ScriptTemplate t = new ScriptTemplate();
        t.setId(1L);
        t.setName("开场白话术");
        t.setDescription("desc");
        t.setCategory("开场");
        t.setIcon("icon-play-arrow");
        t.setPromptTemplate("prompt");
        t.setInputFields("[]");
        t.setSortOrder(1);
        t.setIsActive(1);

        when(scriptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(t));

        List<ScriptTemplateVO> result = service.listTemplates();

        assertEquals(1, result.size());
        assertEquals("开场白话术", result.get(0).getName());
    }

    @Test
    void generate_success() {
        ScriptTemplate t = new ScriptTemplate();
        t.setId(1L);
        t.setName("促单话术");
        t.setPromptTemplate("prompt for {product}");
        t.setIsActive(1);
        when(scriptTemplateMapper.selectById(1L)).thenReturn(t);
        when(scriptGenerationMapper.insert(any(ScriptGeneration.class))).thenAnswer(inv -> {
            ScriptGeneration g = inv.getArgument(0);
            g.setId(10L);
            return 1;
        });

        GenerateScriptRequest req = new GenerateScriptRequest();
        req.setTemplateId(1L);
        req.setInputParams("{\"product\":\"手机\"}");

        ScriptGeneration result = service.generate(req);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertNotNull(result.getGeneratedText());
        verify(scriptGenerationMapper).insert(any(ScriptGeneration.class));
    }

    @Test
    void generate_templateNotFound_throws() {
        when(scriptTemplateMapper.selectById(999L)).thenReturn(null);

        GenerateScriptRequest req = new GenerateScriptRequest();
        req.setTemplateId(999L);

        assertThrows(BusinessException.class, () -> service.generate(req));
    }

    @Test
    void rate_success() {
        ScriptGeneration gen = new ScriptGeneration();
        gen.setId(1L);
        gen.setUserId(1L);
        when(scriptGenerationMapper.selectById(1L)).thenReturn(gen);
        when(scriptGenerationMapper.updateById(any(ScriptGeneration.class))).thenReturn(1);

        service.rate(1L, 5);

        verify(scriptGenerationMapper).updateById(any(ScriptGeneration.class));
    }

    @Test
    void rate_invalidRating_throws() {
        assertThrows(BusinessException.class, () -> service.rate(1L, 6));
        assertThrows(BusinessException.class, () -> service.rate(1L, 0));
    }

    @Test
    void rate_otherUser_throws() {
        ScriptGeneration gen = new ScriptGeneration();
        gen.setId(1L);
        gen.setUserId(999L);
        when(scriptGenerationMapper.selectById(1L)).thenReturn(gen);

        assertThrows(BusinessException.class, () -> service.rate(1L, 4));
    }
}
