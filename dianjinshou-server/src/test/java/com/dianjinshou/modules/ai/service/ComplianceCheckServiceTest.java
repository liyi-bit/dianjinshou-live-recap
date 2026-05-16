package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.modules.ai.dto.ComplianceCheckRequest;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper;
import com.dianjinshou.modules.ai.vo.ComplianceCheckResultVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceCheckServiceTest {

    @Mock
    private SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    @Mock
    private SensitiveWordEngine sensitiveWordEngine;

    private ComplianceCheckService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a.setCurrentNamespace("com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper");
            TableInfoHelper.initTableInfo(a, SensitiveWordLibrary.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new ComplianceCheckService(sensitiveWordLibraryMapper, sensitiveWordEngine);
    }

    @Test
    void check_noHits_safe() {
        when(sensitiveWordEngine.scan(anyString(), any()))
                .thenReturn(Collections.<SensitiveWordEngine.MatchResult>emptyList());

        ComplianceCheckRequest req = new ComplianceCheckRequest();
        req.setScenario("live_speech");
        req.setTextContent("这是一段正常的直播内容");

        ComplianceCheckResultVO result = service.check(req);

        assertEquals(0, result.getRiskScore());
        assertEquals("safe", result.getRiskLevel());
        assertTrue(result.getHitWords().isEmpty());
    }

    @Test
    void check_withHits_calculatesRisk() {
        SensitiveWordEngine.MatchResult mr1 = new SensitiveWordEngine.MatchResult();
        mr1.setWord("违禁词");
        mr1.setPosition(6);
        mr1.setCategory("banned");
        mr1.setRiskLevel(3);
        mr1.setReplacement("合规词");

        SensitiveWordEngine.MatchResult mr2 = new SensitiveWordEngine.MatchResult();
        mr2.setWord("敏感词");
        mr2.setPosition(10);
        mr2.setCategory("sensitive");
        mr2.setRiskLevel(2);
        mr2.setReplacement(null);

        List<SensitiveWordEngine.MatchResult> matches = new ArrayList<SensitiveWordEngine.MatchResult>();
        matches.add(mr1);
        matches.add(mr2);

        when(sensitiveWordEngine.scan(anyString(), any())).thenReturn(matches);

        ComplianceCheckRequest req = new ComplianceCheckRequest();
        req.setScenario("product_desc");
        req.setTextContent("这个产品含有违禁词和敏感词");

        ComplianceCheckResultVO result = service.check(req);

        assertEquals(2, result.getHitWords().size());
        assertEquals(75, result.getRiskScore()); // 3*15 + 2*15 = 75
        assertEquals("high", result.getRiskLevel());
        assertEquals(1, result.getSuggestions().size());
        assertTrue(result.getSuggestions().get(0).contains("合规词"));
    }

    @Test
    void check_invalidScenario_throws() {
        ComplianceCheckRequest req = new ComplianceCheckRequest();
        req.setScenario("invalid_scenario");
        req.setTextContent("test");

        assertThrows(BusinessException.class, () -> service.check(req));
    }

    @Test
    void check_riskScoreCappedAt100() {
        SensitiveWordEngine.MatchResult mr1 = new SensitiveWordEngine.MatchResult();
        mr1.setWord("词一");
        mr1.setPosition(2);
        mr1.setCategory("banned");
        mr1.setRiskLevel(5);
        mr1.setReplacement(null);

        SensitiveWordEngine.MatchResult mr2 = new SensitiveWordEngine.MatchResult();
        mr2.setWord("词二");
        mr2.setPosition(5);
        mr2.setCategory("banned");
        mr2.setRiskLevel(5);
        mr2.setReplacement(null);

        List<SensitiveWordEngine.MatchResult> matches = new ArrayList<SensitiveWordEngine.MatchResult>();
        matches.add(mr1);
        matches.add(mr2);

        when(sensitiveWordEngine.scan(anyString(), any())).thenReturn(matches);

        ComplianceCheckRequest req = new ComplianceCheckRequest();
        req.setScenario("ad_copy");
        req.setTextContent("含有词一和词二的广告");

        ComplianceCheckResultVO result = service.check(req);

        assertEquals(100, result.getRiskScore());
        assertEquals("critical", result.getRiskLevel());
    }

    @Test
    void addWord_setsDefaults() {
        when(sensitiveWordLibraryMapper.insert(any(SensitiveWordLibrary.class))).thenReturn(1);
        doNothing().when(sensitiveWordEngine).reload();

        SensitiveWordLibrary word = new SensitiveWordLibrary();
        word.setWord("测试敏感词");
        word.setCategory("test");
        word.setRiskLevel(1);

        SensitiveWordLibrary result = service.addWord(word);

        assertEquals("custom", result.getSource());
        assertEquals(1, result.getIsActive());
        verify(sensitiveWordLibraryMapper).insert(word);
        verify(sensitiveWordEngine).reload();
    }

    @Test
    void deleteWord_systemWord_throws() {
        SensitiveWordLibrary existing = new SensitiveWordLibrary();
        existing.setId(1L);
        existing.setSource("system");
        when(sensitiveWordLibraryMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> service.deleteWord(1L));
        verify(sensitiveWordLibraryMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteWord_customWord_success() {
        SensitiveWordLibrary existing = new SensitiveWordLibrary();
        existing.setId(2L);
        existing.setSource("custom");
        when(sensitiveWordLibraryMapper.selectById(2L)).thenReturn(existing);
        when(sensitiveWordLibraryMapper.deleteById(2L)).thenReturn(1);
        doNothing().when(sensitiveWordEngine).reload();

        service.deleteWord(2L);

        verify(sensitiveWordLibraryMapper).deleteById(2L);
        verify(sensitiveWordEngine).reload();
    }

    @Test
    void updateWord_notFound_throws() {
        when(sensitiveWordLibraryMapper.selectById(999L)).thenReturn(null);

        SensitiveWordLibrary update = new SensitiveWordLibrary();
        update.setWord("new");

        assertThrows(BusinessException.class, () -> service.updateWord(999L, update));
    }
}
