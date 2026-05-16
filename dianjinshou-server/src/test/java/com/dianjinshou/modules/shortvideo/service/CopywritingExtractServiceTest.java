package com.dianjinshou.modules.shortvideo.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.shortvideo.dto.ExtractCopywritingRequest;
import com.dianjinshou.modules.shortvideo.entity.VideoCopywriting;
import com.dianjinshou.modules.shortvideo.mapper.VideoCopywritingMapper;
import com.dianjinshou.modules.shortvideo.vo.VideoCopywritingVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CopywritingExtractServiceTest {

    @Mock
    private VideoCopywritingMapper videoCopywritingMapper;
    @Mock
    private AnalysisTaskProducer analysisTaskProducer;

    private CopywritingExtractService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace("com.dianjinshou.modules.shortvideo.mapper.VideoCopywritingMapper");
            TableInfoHelper.initTableInfo(assistant, VideoCopywriting.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new CopywritingExtractService(videoCopywritingMapper, analysisTaskProducer);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void extractCopywriting_urlMode_success() {
        when(videoCopywritingMapper.insert(any(VideoCopywriting.class))).thenAnswer(inv -> {
            VideoCopywriting vc = inv.getArgument(0);
            vc.setId(1L);
            return 1;
        });

        ExtractCopywritingRequest req = new ExtractCopywritingRequest();
        req.setSourceType("url");
        req.setSourceUrl("https://example.com/video.mp4");
        req.setTitle("测试视频");

        VideoCopywritingVO result = service.extractCopywriting(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("pending", result.getStatus());
        verify(analysisTaskProducer).send(any());
    }

    @Test
    void extractCopywriting_localMode_success() {
        when(videoCopywritingMapper.insert(any(VideoCopywriting.class))).thenAnswer(inv -> {
            VideoCopywriting vc = inv.getArgument(0);
            vc.setId(2L);
            return 1;
        });

        ExtractCopywritingRequest req = new ExtractCopywritingRequest();
        req.setSourceType("local");
        req.setStorageKey("files/100/video.mp4");

        VideoCopywritingVO result = service.extractCopywriting(req);

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void extractCopywriting_invalidSourceType_rejected() {
        ExtractCopywritingRequest req = new ExtractCopywritingRequest();
        req.setSourceType("invalid");

        assertThrows(BusinessException.class, () -> service.extractCopywriting(req));
    }

    @Test
    void extractCopywriting_urlModeNoUrl_rejected() {
        ExtractCopywritingRequest req = new ExtractCopywritingRequest();
        req.setSourceType("url");

        assertThrows(BusinessException.class, () -> service.extractCopywriting(req));
    }

    @Test
    void getCopywriting_success() {
        VideoCopywriting entity = buildEntity(1L, 100L);
        when(videoCopywritingMapper.selectById(1L)).thenReturn(entity);

        VideoCopywritingVO result = service.getCopywriting(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getCopywriting_notFound() {
        when(videoCopywritingMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getCopywriting(999L));
    }

    @Test
    void recordCopy_success() {
        VideoCopywriting entity = buildEntity(1L, 100L);
        when(videoCopywritingMapper.selectById(1L)).thenReturn(entity);
        when(videoCopywritingMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        service.recordCopy(1L);

        verify(videoCopywritingMapper).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void listCopywriting_withFilter() {
        Page<VideoCopywriting> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(videoCopywritingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<VideoCopywritingVO> result = service.listCopywriting(1, 20, "completed");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    private VideoCopywriting buildEntity(Long id, Long orgId) {
        VideoCopywriting entity = new VideoCopywriting();
        entity.setId(id);
        entity.setUserId(1L);
        entity.setOrgId(orgId);
        entity.setSourceType("url");
        entity.setSourceUrl("https://example.com/video.mp4");
        entity.setTitle("测试");
        entity.setStatus("completed");
        entity.setCopyCount(0);
        entity.setWordCount(100);
        return entity;
    }
}
