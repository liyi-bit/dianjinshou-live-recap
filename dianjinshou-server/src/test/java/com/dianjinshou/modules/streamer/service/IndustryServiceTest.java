package com.dianjinshou.modules.streamer.service;

import com.dianjinshou.modules.streamer.entity.Industry;
import com.dianjinshou.modules.streamer.mapper.IndustryMapper;
import com.dianjinshou.modules.streamer.vo.IndustryTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IndustryServiceTest {

    private IndustryMapper industryMapper;
    private IndustryService industryService;

    @BeforeEach
    void setUp() {
        industryMapper = mock(IndustryMapper.class);
        industryService = new IndustryService(industryMapper);
    }

    @Test
    void tree_buildsHierarchy() {
        Industry parent = new Industry();
        parent.setId(1L);
        parent.setName("知识付费");
        parent.setParentId(null);
        parent.setLevel(1);
        parent.setCode("knowledge");

        Industry child1 = new Industry();
        child1.setId(15L);
        child1.setName("财商教育");
        child1.setParentId(1L);
        child1.setLevel(2);
        child1.setCode("knowledge_finance");

        Industry child2 = new Industry();
        child2.setId(16L);
        child2.setName("职业技能");
        child2.setParentId(1L);
        child2.setLevel(2);
        child2.setCode("knowledge_skill");

        when(industryMapper.selectList(any())).thenReturn(Arrays.asList(parent, child1, child2));

        List<IndustryTreeVO> tree = industryService.tree();

        assertEquals(1, tree.size());
        assertEquals("知识付费", tree.get(0).getName());
        assertEquals(2, tree.get(0).getChildren().size());
        assertEquals("财商教育", tree.get(0).getChildren().get(0).getName());
    }

    @Test
    void tree_emptyList() {
        when(industryMapper.selectList(any())).thenReturn(Arrays.asList());
        List<IndustryTreeVO> tree = industryService.tree();
        assertTrue(tree.isEmpty());
    }

    @Test
    void getIndustryName_found() {
        Industry industry = new Industry();
        industry.setId(1L);
        industry.setName("知识付费");
        when(industryMapper.selectById(1L)).thenReturn(industry);

        assertEquals("知识付费", industryService.getIndustryName(1L));
    }

    @Test
    void getIndustryName_notFound() {
        when(industryMapper.selectById(999L)).thenReturn(null);
        assertNull(industryService.getIndustryName(999L));
    }

    @Test
    void getIndustryName_nullId() {
        assertNull(industryService.getIndustryName(null));
    }
}
