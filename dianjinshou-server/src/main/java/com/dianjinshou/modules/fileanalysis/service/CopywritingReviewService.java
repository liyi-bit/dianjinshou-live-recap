package com.dianjinshou.modules.fileanalysis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.fileanalysis.dto.CopywritingReviewRequest;
import com.dianjinshou.modules.fileanalysis.entity.CopywritingReview;
import com.dianjinshou.modules.fileanalysis.mapper.CopywritingReviewMapper;
import com.dianjinshou.modules.fileanalysis.vo.CopywritingReviewVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CopywritingReviewService {

    private static final Logger log = LoggerFactory.getLogger(CopywritingReviewService.class);

    private final CopywritingReviewMapper copywritingReviewMapper;
    private final ObjectMapper objectMapper;

    public CopywritingReviewService(CopywritingReviewMapper copywritingReviewMapper,
                                     ObjectMapper objectMapper) {
        this.copywritingReviewMapper = copywritingReviewMapper;
        this.objectMapper = objectMapper;
    }

    public CopywritingReviewVO submitReview(CopywritingReviewRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        CopywritingReview review = new CopywritingReview();
        review.setUserId(userId);
        review.setOrgId(orgId);
        review.setTextContent(request.getTextContent());
        review.setIndustryId(request.getIndustryId());
        review.setStatus("pending");
        review.setCreatedAt(LocalDateTime.now());
        copywritingReviewMapper.insert(review);

        // Perform synchronous basic detection (sensitive word scan)
        // AI semantic analysis would be async via RabbitMQ in production
        Map<String, Object> resultMap = performBasicReview(request);
        int riskScore = calculateRiskScore(resultMap);

        try {
            String resultJson = objectMapper.writeValueAsString(resultMap);
            LambdaUpdateWrapper<CopywritingReview> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(CopywritingReview::getId, review.getId())
                    .set(CopywritingReview::getResult, resultJson)
                    .set(CopywritingReview::getRiskScore, riskScore)
                    .set(CopywritingReview::getStatus, "completed");
            copywritingReviewMapper.update(null, wrapper);

            review.setResult(resultJson);
            review.setRiskScore(riskScore);
            review.setStatus("completed");
        } catch (Exception e) {
            log.error("Failed to serialize review result", e);
            LambdaUpdateWrapper<CopywritingReview> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(CopywritingReview::getId, review.getId())
                    .set(CopywritingReview::getStatus, "failed");
            copywritingReviewMapper.update(null, wrapper);
            review.setStatus("failed");
        }

        log.info("Copywriting review completed: id={}, riskScore={}", review.getId(), riskScore);
        return CopywritingReviewVO.fromEntity(review);
    }

    public Page<CopywritingReviewVO> listReviews(int page, int size) {
        LambdaQueryWrapper<CopywritingReview> query = new LambdaQueryWrapper<>();
        query.eq(CopywritingReview::getUserId, SecurityContextHelper.currentUserId());
        query.orderByDesc(CopywritingReview::getCreatedAt);

        Page<CopywritingReview> entityPage = copywritingReviewMapper.selectPage(new Page<>(page, size), query);
        Page<CopywritingReviewVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(new ArrayList<CopywritingReviewVO>());
        for (CopywritingReview r : entityPage.getRecords()) {
            voPage.getRecords().add(CopywritingReviewVO.fromEntity(r));
        }
        return voPage;
    }

    private Map<String, Object> performBasicReview(CopywritingReviewRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, Object>> sensitiveWords = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> complianceIssues = new ArrayList<Map<String, Object>>();
        List<String> optimizationSuggestions = new ArrayList<String>();

        // Basic sensitive word patterns check (placeholder, real Trie engine will be in T25)
        String text = request.getTextContent();
        if (request.isCheckSensitive()) {
            // Placeholder: in production, this uses the SensitiveWordEngine Trie tree
            result.put("sensitiveWordCount", sensitiveWords.size());
        }

        if (request.isCheckCompliance()) {
            // Placeholder: in production, this calls AI model for semantic analysis
            result.put("complianceIssueCount", complianceIssues.size());
        }

        optimizationSuggestions.add("建议增加更多互动性话术");
        optimizationSuggestions.add("建议控制单句长度在30字以内");

        result.put("sensitiveWords", sensitiveWords);
        result.put("complianceIssues", complianceIssues);
        result.put("optimizationSuggestions", optimizationSuggestions);
        return result;
    }

    private int calculateRiskScore(Map<String, Object> resultMap) {
        int score = 0;
        Object swCount = resultMap.get("sensitiveWordCount");
        if (swCount instanceof Integer) {
            score += (Integer) swCount * 10;
        }
        Object ciCount = resultMap.get("complianceIssueCount");
        if (ciCount instanceof Integer) {
            score += (Integer) ciCount * 20;
        }
        return Math.min(score, 100);
    }
}
