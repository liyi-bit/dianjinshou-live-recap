package com.dianjinshou.modules.dictionary.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.dictionary.entity.Dictionary;
import com.dianjinshou.modules.dictionary.entity.DictionaryKeyword;
import com.dianjinshou.modules.dictionary.mapper.DictionaryKeywordMapper;
import com.dianjinshou.modules.dictionary.mapper.DictionaryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private DictionaryMapper dictionaryMapper;
    @Mock
    private DictionaryKeywordMapper keywordMapper;

    @InjectMocks
    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        SecurityUser user = new SecurityUser(1L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDictionary_success() {
        when(dictionaryMapper.insert(any(Dictionary.class))).thenReturn(1);

        Dictionary result = dictionaryService.createDictionary("我的词库", "描述");

        assertNotNull(result);
        assertEquals("我的词库", result.getName());
        assertEquals(0, result.getIsSystem());
        verify(dictionaryMapper).insert(any(Dictionary.class));
    }

    @Test
    void updateDictionary_systemDictForbidden() {
        Dictionary systemDict = new Dictionary();
        systemDict.setId(1L);
        systemDict.setIsSystem(1);
        systemDict.setOrgId(5L);
        when(dictionaryMapper.selectById(1L)).thenReturn(systemDict);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.updateDictionary(1L, "新名", "新描述"));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void deleteDictionary_systemDictForbidden() {
        Dictionary systemDict = new Dictionary();
        systemDict.setId(1L);
        systemDict.setIsSystem(1);
        systemDict.setOrgId(5L);
        when(dictionaryMapper.selectById(1L)).thenReturn(systemDict);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.deleteDictionary(1L));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void deleteDictionary_success() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setIsSystem(0);
        dict.setOrgId(5L);
        when(dictionaryMapper.selectById(1L)).thenReturn(dict);
        when(keywordMapper.delete(any())).thenReturn(3);
        when(dictionaryMapper.deleteById(1L)).thenReturn(1);

        dictionaryService.deleteDictionary(1L);

        verify(keywordMapper).delete(any());
        verify(dictionaryMapper).deleteById(1L);
    }

    @Test
    void addKeyword_success() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setIsSystem(0);
        dict.setOrgId(5L);
        when(dictionaryMapper.selectById(1L)).thenReturn(dict);
        when(keywordMapper.insert(any(DictionaryKeyword.class))).thenReturn(1);

        DictionaryKeyword result = dictionaryService.addKeyword(1L,
                "运营", "促单", "下单", "促单关键词", null);

        assertNotNull(result);
        assertEquals("下单", result.getKeyword());
    }

    @Test
    void deleteKeyword_notFound() {
        when(keywordMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.deleteKeyword(1L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getDictionary_crossOrgAccess() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setIsSystem(0);
        dict.setOrgId(99L); // different org
        when(dictionaryMapper.selectById(1L)).thenReturn(dict);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.listKeywords(1L));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }
}
