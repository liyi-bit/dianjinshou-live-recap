package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.modules.ai.dto.ComplianceCheckRequest;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper;
import com.dianjinshou.modules.ai.vo.ComplianceCheckResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ComplianceCheckService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceCheckService.class);
    private static final Set<String> VALID_SCENARIOS = new HashSet<String>(
            Arrays.asList("live_speech", "product_desc", "ad_copy", "comment"));

    private final SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    private final SensitiveWordEngine sensitiveWordEngine;

    public ComplianceCheckService(SensitiveWordLibraryMapper sensitiveWordLibraryMapper,
                                   SensitiveWordEngine sensitiveWordEngine) {
        this.sensitiveWordLibraryMapper = sensitiveWordLibraryMapper;
        this.sensitiveWordEngine = sensitiveWordEngine;
    }

    public ComplianceCheckResultVO check(ComplianceCheckRequest request) {
        if (!VALID_SCENARIOS.contains(request.getScenario())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的检测场景");
        }

        String text = request.getTextContent();

        // Trie-based scan with optional platform filter
        List<SensitiveWordEngine.MatchResult> matches = sensitiveWordEngine.scan(text, request.getPlatform());

        List<ComplianceCheckResultVO.HitWord> hits = new ArrayList<ComplianceCheckResultVO.HitWord>();
        for (SensitiveWordEngine.MatchResult mr : matches) {
            ComplianceCheckResultVO.HitWord hit = new ComplianceCheckResultVO.HitWord();
            hit.setWord(mr.getWord());
            hit.setPosition(mr.getPosition());
            hit.setCategory(mr.getCategory());
            hit.setRiskLevel(mr.getRiskLevel());
            hit.setReplacement(mr.getReplacement());
            hits.add(hit);
        }

        // Calculate risk score
        int riskScore = 0;
        for (ComplianceCheckResultVO.HitWord h : hits) {
            riskScore += h.getRiskLevel() * 15;
        }
        if (riskScore > 100) riskScore = 100;

        String riskLevel;
        if (riskScore == 0) riskLevel = "safe";
        else if (riskScore <= 20) riskLevel = "low";
        else if (riskScore <= 50) riskLevel = "medium";
        else if (riskScore <= 80) riskLevel = "high";
        else riskLevel = "critical";

        List<String> suggestions = new ArrayList<String>();
        for (ComplianceCheckResultVO.HitWord h : hits) {
            if (h.getReplacement() != null) {
                suggestions.add("将「" + h.getWord() + "」替换为「" + h.getReplacement() + "」");
            }
        }

        ComplianceCheckResultVO result = new ComplianceCheckResultVO();
        result.setHitWords(hits);
        result.setRiskScore(riskScore);
        result.setRiskLevel(riskLevel);
        result.setSuggestions(suggestions);
        result.setAiAnalysis("基于敏感词扫描完成，AI 语义验证待接入");

        log.info("Compliance check: scenario={}, hits={}, riskScore={}", request.getScenario(), hits.size(), riskScore);
        return result;
    }

    public Page<SensitiveWordLibrary> listWords(int page, int size, String category, String keyword) {
        LambdaQueryWrapper<SensitiveWordLibrary> query = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            query.eq(SensitiveWordLibrary::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(SensitiveWordLibrary::getWord, keyword);
        }
        query.orderByDesc(SensitiveWordLibrary::getRiskLevel);
        return sensitiveWordLibraryMapper.selectPage(new Page<>(page, size), query);
    }

    public SensitiveWordLibrary addWord(SensitiveWordLibrary word) {
        word.setSource("custom");
        word.setIsActive(1);
        sensitiveWordLibraryMapper.insert(word);
        sensitiveWordEngine.reload();
        return word;
    }

    public void updateWord(Long id, SensitiveWordLibrary update) {
        SensitiveWordLibrary existing = sensitiveWordLibraryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }
        update.setId(id);
        sensitiveWordLibraryMapper.updateById(update);
        sensitiveWordEngine.reload();
    }

    public void deleteWord(Long id) {
        SensitiveWordLibrary existing = sensitiveWordLibraryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "敏感词不存在");
        }
        if ("system".equals(existing.getSource())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "系统敏感词不可删除");
        }
        sensitiveWordLibraryMapper.deleteById(id);
        sensitiveWordEngine.reload();
    }
}
