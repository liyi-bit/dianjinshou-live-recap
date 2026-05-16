package com.dianjinshou.modules.shortvideo.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.modules.shortvideo.entity.Creator;
import com.dianjinshou.modules.shortvideo.entity.CreatorVideo;
import com.dianjinshou.modules.shortvideo.mapper.CreatorMapper;
import com.dianjinshou.modules.shortvideo.mapper.CreatorVideoMapper;
import com.dianjinshou.modules.shortvideo.provider.CreatorDataProvider;
import com.dianjinshou.modules.shortvideo.vo.CreatorDetailVO;
import com.dianjinshou.modules.shortvideo.vo.CreatorVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorSearchServiceTest {

    @Mock
    private CreatorMapper creatorMapper;
    @Mock
    private CreatorVideoMapper creatorVideoMapper;
    @Mock
    private CreatorDataProvider creatorDataProvider;

    private CreatorSearchService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace("com.dianjinshou.modules.shortvideo.mapper.CreatorMapper");
            TableInfoHelper.initTableInfo(assistant, Creator.class);

            MapperBuilderAssistant assistant2 = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant2.setCurrentNamespace("com.dianjinshou.modules.shortvideo.mapper.CreatorVideoMapper");
            TableInfoHelper.initTableInfo(assistant2, CreatorVideo.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new CreatorSearchService(creatorMapper, creatorVideoMapper, creatorDataProvider);
    }

    @Test
    void searchCreators_localResults() {
        Creator c = new Creator();
        c.setId(1L);
        c.setPlatform("douyin");
        c.setCreatorId("abc");
        c.setNickname("测试达人");
        c.setFollowerCount(100000L);
        c.setVideoCount(50);
        c.setIndustry("美妆");
        when(creatorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(c));

        List<CreatorVO> results = service.searchCreators("测试", null, null, null, null, 1, 20);

        assertEquals(1, results.size());
        assertEquals("测试达人", results.get(0).getNickname());
    }

    @Test
    void searchCreators_fallbackToProvider() {
        when(creatorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        Creator external = new Creator();
        external.setPlatform("douyin");
        external.setCreatorId("ext_1");
        external.setNickname("外部达人");
        external.setFollowerCount(200000L);
        when(creatorDataProvider.searchCreators(anyString(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(external));
        when(creatorMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(creatorMapper.insert(any(Creator.class))).thenReturn(1);

        List<CreatorVO> results = service.searchCreators("外部", null, null, null, null, 1, 20);

        assertEquals(1, results.size());
        assertEquals("外部达人", results.get(0).getNickname());
        verify(creatorMapper).insert(any(Creator.class));
    }

    @Test
    void getCreatorDetail_success() {
        Creator c = new Creator();
        c.setId(1L);
        c.setPlatform("douyin");
        c.setCreatorId("abc");
        c.setNickname("测试达人");
        c.setFollowerCount(100000L);
        c.setVideoCount(50);
        when(creatorMapper.selectById(1L)).thenReturn(c);

        CreatorVideo v = new CreatorVideo();
        v.setId(1L);
        v.setCreatorId(1L);
        v.setTitle("视频1");
        v.setPlayCount(500000L);
        when(creatorVideoMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(v));

        CreatorDetailVO detail = service.getCreatorDetail(1L);

        assertNotNull(detail);
        assertEquals("测试达人", detail.getNickname());
        assertEquals(1, detail.getRecentVideos().size());
    }

    @Test
    void getCreatorDetail_notFound() {
        when(creatorMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getCreatorDetail(999L));
    }
}
