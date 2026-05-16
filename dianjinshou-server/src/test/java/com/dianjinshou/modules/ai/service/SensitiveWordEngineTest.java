package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveWordEngineTest {

    @Mock
    private SensitiveWordLibraryMapper sensitiveWordLibraryMapper;

    private SensitiveWordEngine engine;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a.setCurrentNamespace("com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper2");
            TableInfoHelper.initTableInfo(a, SensitiveWordLibrary.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        engine = new SensitiveWordEngine(sensitiveWordLibraryMapper);
    }

    private List<SensitiveWordLibrary> buildWords() {
        List<SensitiveWordLibrary> words = new ArrayList<SensitiveWordLibrary>();

        SensitiveWordLibrary w1 = new SensitiveWordLibrary();
        w1.setWord("违禁词");
        w1.setCategory("违禁品");
        w1.setRiskLevel(3);
        w1.setReplacementSuggestion("合规词");
        w1.setPlatform("all");
        w1.setIsActive(1);
        words.add(w1);

        SensitiveWordLibrary w2 = new SensitiveWordLibrary();
        w2.setWord("全网最低价");
        w2.setCategory("虚假宣传");
        w2.setRiskLevel(2);
        w2.setReplacementSuggestion("优惠价格");
        w2.setPlatform("all");
        w2.setIsActive(1);
        words.add(w2);

        SensitiveWordLibrary w3 = new SensitiveWordLibrary();
        w3.setWord("加微信");
        w3.setCategory("引导场外");
        w3.setRiskLevel(2);
        w3.setReplacementSuggestion(null);
        w3.setPlatform("douyin");
        w3.setIsActive(1);
        words.add(w3);

        return words;
    }

    @Test
    void scan_findsMatchingWords() {
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(buildWords());
        engine.reload();

        List<SensitiveWordEngine.MatchResult> results = engine.scan("这个产品含有违禁词，全网最低价");

        assertEquals(2, results.size());
        assertEquals("违禁词", results.get(0).getWord());
        assertEquals("全网最低价", results.get(1).getWord());
    }

    @Test
    void scan_noMatch_returnsEmpty() {
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(buildWords());
        engine.reload();

        List<SensitiveWordEngine.MatchResult> results = engine.scan("这是一段正常的文本内容");

        assertTrue(results.isEmpty());
    }

    @Test
    void scan_withPlatformFilter() {
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(buildWords());
        engine.reload();

        // "加微信" is douyin-only; should appear when platform=douyin
        List<SensitiveWordEngine.MatchResult> results = engine.scan("请加微信联系我", "douyin");
        assertEquals(1, results.size());
        assertEquals("加微信", results.get(0).getWord());

        // Should NOT appear for kuaishou (platform mismatch)
        List<SensitiveWordEngine.MatchResult> results2 = engine.scan("请加微信联系我", "kuaishou");
        assertTrue(results2.isEmpty());
    }

    @Test
    void scan_returnsCorrectPositions() {
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(buildWords());
        engine.reload();

        List<SensitiveWordEngine.MatchResult> results = engine.scan("今天违禁词出现了");

        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getPosition()); // "违禁词" starts at index 2
        assertEquals(3, results.get(0).getRiskLevel());
        assertEquals("合规词", results.get(0).getReplacement());
    }

    @Test
    void getWordCount_afterReload() {
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(buildWords());
        engine.reload();

        assertEquals(3, engine.getWordCount());
    }

    @Test
    void scan_emptyText_returnsEmpty() {
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(buildWords());
        engine.reload();

        assertTrue(engine.scan("").isEmpty());
        assertTrue(engine.scan(null).isEmpty());
    }

    @Test
    void performanceBenchmark_10kWords() {
        // Build 10k words for performance testing
        List<SensitiveWordLibrary> words = new ArrayList<SensitiveWordLibrary>();
        for (int i = 0; i < 10000; i++) {
            SensitiveWordLibrary w = new SensitiveWordLibrary();
            w.setWord("敏感词" + i);
            w.setCategory("其他");
            w.setRiskLevel(1);
            w.setPlatform("all");
            w.setIsActive(1);
            words.add(w);
        }
        when(sensitiveWordLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(words);

        long loadStart = System.currentTimeMillis();
        engine.reload();
        long loadTime = System.currentTimeMillis() - loadStart;

        assertEquals(10000, engine.getWordCount());
        assertTrue(loadTime < 3000, "Loading 10k words should take <3s, took " + loadTime + "ms");

        // Scan 1000-char text
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            text.append("这是一段测试文本内容包含敏感词");
        }
        // Insert a known word to verify scan works
        String testText = text.toString() + "敏感词5000在这里";

        long scanStart = System.currentTimeMillis();
        List<SensitiveWordEngine.MatchResult> results = engine.scan(testText);
        long scanTime = System.currentTimeMillis() - scanStart;

        assertFalse(results.isEmpty());
        assertTrue(scanTime < 50, "Scanning 1000+ chars should take <50ms, took " + scanTime + "ms");
    }
}
