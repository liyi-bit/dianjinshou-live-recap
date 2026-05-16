package com.dianjinshou.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
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
import org.springframework.stereotype.Service;

@Service
public class AdminAiService {

    private final AiSessionMapper aiSessionMapper;
    private final AiConversationMapper aiConversationMapper;
    private final SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    private final ScriptTemplateMapper scriptTemplateMapper;
    private final ScriptGenerationMapper scriptGenerationMapper;
    private final SensitiveWordEngine sensitiveWordEngine;

    public AdminAiService(AiSessionMapper aiSessionMapper,
                          AiConversationMapper aiConversationMapper,
                          SensitiveWordLibraryMapper sensitiveWordLibraryMapper,
                          ScriptTemplateMapper scriptTemplateMapper,
                          ScriptGenerationMapper scriptGenerationMapper,
                          SensitiveWordEngine sensitiveWordEngine) {
        this.aiSessionMapper = aiSessionMapper;
        this.aiConversationMapper = aiConversationMapper;
        this.sensitiveWordLibraryMapper = sensitiveWordLibraryMapper;
        this.scriptTemplateMapper = scriptTemplateMapper;
        this.scriptGenerationMapper = scriptGenerationMapper;
        this.sensitiveWordEngine = sensitiveWordEngine;
    }

    public AiStatsVO getStats() {
        requireAdmin();

        AiStatsVO stats = new AiStatsVO();
        stats.setTotalSessions(aiSessionMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setTotalMessages(aiConversationMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setOperationSessions(aiSessionMapper.selectCount(
                new LambdaQueryWrapper<AiSession>().eq(AiSession::getAssistantType, "operation")));
        stats.setComplianceSessions(aiSessionMapper.selectCount(
                new LambdaQueryWrapper<AiSession>().eq(AiSession::getAssistantType, "compliance")));
        stats.setScriptSessions(aiSessionMapper.selectCount(
                new LambdaQueryWrapper<AiSession>().eq(AiSession::getAssistantType, "script")));
        stats.setTotalGenerations(scriptGenerationMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setSensitiveWordCount(sensitiveWordEngine.getWordCount());

        return stats;
    }

    public Page<SensitiveWordLibrary> listSensitiveWords(int page, int size, String category, String keyword) {
        requireAdmin();

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

    public Page<ScriptTemplate> listScriptTemplates(int page, int size) {
        requireAdmin();

        LambdaQueryWrapper<ScriptTemplate> query = new LambdaQueryWrapper<>();
        query.orderByAsc(ScriptTemplate::getSortOrder);
        return scriptTemplateMapper.selectPage(new Page<>(page, size), query);
    }

    public void updateScriptTemplate(Long id, ScriptTemplate update) {
        requireAdmin();

        ScriptTemplate existing = scriptTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        update.setId(id);
        scriptTemplateMapper.updateById(update);
    }

    public void toggleScriptTemplate(Long id) {
        requireAdmin();

        ScriptTemplate existing = scriptTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        existing.setIsActive(existing.getIsActive() == 1 ? 0 : 1);
        scriptTemplateMapper.updateById(existing);
    }

    private void requireAdmin() {
        SecurityUser user = SecurityContextHelper.currentUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String role = user.getRole();
        if (!"super_admin".equals(role) && !"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "此操作仅限管理员，请联系管理员开通");
        }
    }
}
