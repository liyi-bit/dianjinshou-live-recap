package com.dianjinshou.modules.dictionary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.dictionary.entity.Dictionary;
import com.dianjinshou.modules.dictionary.entity.DictionaryKeyword;
import com.dianjinshou.modules.dictionary.mapper.DictionaryKeywordMapper;
import com.dianjinshou.modules.dictionary.mapper.DictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DictionaryService {

    private final DictionaryMapper dictionaryMapper;
    private final DictionaryKeywordMapper keywordMapper;

    public DictionaryService(DictionaryMapper dictionaryMapper,
                             DictionaryKeywordMapper keywordMapper) {
        this.dictionaryMapper = dictionaryMapper;
        this.keywordMapper = keywordMapper;
    }

    public List<Dictionary> listDictionaries() {
        Long orgId = SecurityContextHelper.currentOrgId();
        return dictionaryMapper.selectList(
                new LambdaQueryWrapper<Dictionary>()
                        .and(w -> w
                                .eq(Dictionary::getIsSystem, 1)
                                .or()
                                .eq(orgId != null, Dictionary::getOrgId, orgId))
                        .orderByAsc(Dictionary::getId));
    }

    @Transactional
    public Dictionary createDictionary(String name, String description) {
        Long userId = SecurityContextHelper.currentUserId();
        Long orgId = SecurityContextHelper.currentOrgId();

        Dictionary dict = new Dictionary();
        dict.setUserId(userId);
        dict.setOrgId(orgId);
        dict.setName(name);
        dict.setDescription(description);
        dict.setIsSystem(0);
        dictionaryMapper.insert(dict);
        return dict;
    }

    @Transactional
    public void updateDictionary(Long id, String name, String description) {
        Dictionary dict = getDictionaryWithAccess(id);
        if (dict.getIsSystem() != null && dict.getIsSystem() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统词库不可修改");
        }
        dict.setName(name);
        dict.setDescription(description);
        dictionaryMapper.updateById(dict);
    }

    @Transactional
    public void deleteDictionary(Long id) {
        Dictionary dict = getDictionaryWithAccess(id);
        if (dict.getIsSystem() != null && dict.getIsSystem() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统词库不可删除");
        }
        // Delete keywords first
        keywordMapper.delete(new LambdaQueryWrapper<DictionaryKeyword>()
                .eq(DictionaryKeyword::getDictionaryId, id));
        dictionaryMapper.deleteById(id);
    }

    public List<DictionaryKeyword> listKeywords(Long dictionaryId) {
        getDictionaryWithAccess(dictionaryId);
        return keywordMapper.selectList(
                new LambdaQueryWrapper<DictionaryKeyword>()
                        .eq(DictionaryKeyword::getDictionaryId, dictionaryId)
                        .orderByAsc(DictionaryKeyword::getCategory));
    }

    @Transactional
    public DictionaryKeyword addKeyword(Long dictionaryId, String category, String subCategory,
                                         String keyword, String description, String replacementSuggestion) {
        getDictionaryWithAccess(dictionaryId);

        DictionaryKeyword kw = new DictionaryKeyword();
        kw.setDictionaryId(dictionaryId);
        kw.setCategory(category);
        kw.setSubCategory(subCategory);
        kw.setKeyword(keyword);
        kw.setDescription(description);
        kw.setReplacementSuggestion(replacementSuggestion);
        keywordMapper.insert(kw);
        return kw;
    }

    @Transactional
    public void deleteKeyword(Long keywordId) {
        DictionaryKeyword kw = keywordMapper.selectById(keywordId);
        if (kw == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "关键词不存在");
        }
        getDictionaryWithAccess(kw.getDictionaryId());
        keywordMapper.deleteById(keywordId);
    }

    private Dictionary getDictionaryWithAccess(Long id) {
        Dictionary dict = dictionaryMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "词库不存在");
        }
        if (dict.getIsSystem() != null && dict.getIsSystem() == 1) {
            return dict; // system dictionaries are readable by all
        }
        Long currentOrgId = SecurityContextHelper.currentOrgId();
        String role = SecurityContextHelper.currentRole();
        if (!"super_admin".equals(role) && (currentOrgId == null || !currentOrgId.equals(dict.getOrgId()))) {
            throw new BusinessException(ErrorCode.CROSS_ORG_ACCESS, "无权访问该词库");
        }
        return dict;
    }
}
