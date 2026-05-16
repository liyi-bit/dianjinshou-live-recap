package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.admin.service.DailyAiQuotaService;
import com.dianjinshou.modules.ai.dto.GenerateScriptRequest;
import com.dianjinshou.modules.ai.entity.ScriptGeneration;
import com.dianjinshou.modules.ai.entity.ScriptTemplate;
import com.dianjinshou.modules.ai.mapper.ScriptGenerationMapper;
import com.dianjinshou.modules.ai.mapper.ScriptTemplateMapper;
import com.dianjinshou.modules.ai.vo.ScriptTemplateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScriptAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ScriptAssistantService.class);

    private final ScriptTemplateMapper scriptTemplateMapper;
    private final ScriptGenerationMapper scriptGenerationMapper;
    private final DailyAiQuotaService dailyQuota;

    public ScriptAssistantService(ScriptTemplateMapper scriptTemplateMapper,
                                   ScriptGenerationMapper scriptGenerationMapper,
                                   DailyAiQuotaService dailyQuota) {
        this.scriptTemplateMapper = scriptTemplateMapper;
        this.scriptGenerationMapper = scriptGenerationMapper;
        this.dailyQuota = dailyQuota;
    }

    public List<ScriptTemplateVO> listTemplates() {
        LambdaQueryWrapper<ScriptTemplate> query = new LambdaQueryWrapper<>();
        query.eq(ScriptTemplate::getIsActive, 1);
        query.orderByAsc(ScriptTemplate::getSortOrder);
        List<ScriptTemplate> templates = scriptTemplateMapper.selectList(query);
        List<ScriptTemplateVO> vos = new ArrayList<ScriptTemplateVO>();
        for (ScriptTemplate t : templates) {
            vos.add(ScriptTemplateVO.fromEntity(t));
        }
        return vos;
    }

    public ScriptGeneration generate(GenerateScriptRequest request) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();
        if (userId == null || orgId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // v1.1.2：AI 话术生成也走每日 10 次 AI 额度，超限直接拦截
        dailyQuota.checkBeforeAnalyze(userId);

        ScriptTemplate template = scriptTemplateMapper.selectById(request.getTemplateId());
        if (template == null || template.getIsActive() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在或已停用");
        }

        // Build prompt by replacing placeholders (AI generation is placeholder for now)
        String prompt = template.getPromptTemplate();
        String generatedText = "【AI 生成话术待接入】基于模板「" + template.getName() + "」生成的话术内容。";

        ScriptGeneration gen = new ScriptGeneration();
        gen.setUserId(userId);
        gen.setOrgId(orgId);
        gen.setTemplateId(request.getTemplateId());
        gen.setInputParams(request.getInputParams());
        gen.setGeneratedText(generatedText);
        gen.setAiModel("placeholder");
        gen.setTokensUsed(0);
        scriptGenerationMapper.insert(gen);

        log.info("Script generated: userId={}, templateId={}", userId, request.getTemplateId());
        return gen;
    }

    public Page<ScriptGeneration> listHistory(int page, int size) {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        LambdaQueryWrapper<ScriptGeneration> query = new LambdaQueryWrapper<>();
        query.eq(ScriptGeneration::getUserId, userId);
        query.orderByDesc(ScriptGeneration::getCreatedAt);
        return scriptGenerationMapper.selectPage(new Page<>(page, size), query);
    }

    public void rate(Long id, int rating) {
        Long userId = SecurityContextHelper.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (rating < 1 || rating > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分范围为1-5");
        }

        ScriptGeneration gen = scriptGenerationMapper.selectById(id);
        if (gen == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "生成记录不存在");
        }
        if (!gen.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号无该操作权限");
        }

        gen.setRating(rating);
        scriptGenerationMapper.updateById(gen);
    }
}
