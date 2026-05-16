package com.dianjinshou.modules.fileanalysis.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.fileanalysis.dto.CreateFileClipRequest;
import com.dianjinshou.modules.fileanalysis.entity.FileAnalysisTask;
import com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper;
import com.dianjinshou.modules.fileanalysis.vo.FileAnalysisVO;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileClipServiceTest {

    @Mock
    private FileAnalysisTaskMapper fileAnalysisTaskMapper;
    @Mock
    private AnalysisTaskProducer analysisTaskProducer;

    private FileClipService service;

    @BeforeAll
    static void initTableInfo() {
        // Already initialized in FileAnalysisServiceTest, but safe to call again
        try {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace("com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper");
            TableInfoHelper.initTableInfo(assistant, FileAnalysisTask.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new FileClipService(fileAnalysisTaskMapper, analysisTaskProducer);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createClip_success() {
        FileAnalysisTask parent = buildParent(1L, 100L, 600);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(parent);
        when(fileAnalysisTaskMapper.insert(any(FileAnalysisTask.class))).thenAnswer(inv -> {
            FileAnalysisTask t = inv.getArgument(0);
            t.setId(2L);
            return 1;
        });

        CreateFileClipRequest req = new CreateFileClipRequest();
        req.setClipStart(60);
        req.setClipEnd(120);
        req.setClipCategory("RETENTION");

        FileAnalysisVO result = service.createClip(1L, req);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("pending", result.getStatus());
        verify(analysisTaskProducer).send(any());
    }

    @Test
    void createClip_tooShort_rejected() {
        FileAnalysisTask parent = buildParent(1L, 100L, 600);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(parent);

        CreateFileClipRequest req = new CreateFileClipRequest();
        req.setClipStart(60);
        req.setClipEnd(65); // only 5 seconds
        req.setClipCategory("RETENTION");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createClip(1L, req));
        assertTrue(ex.getMessage().contains("10秒"));
    }

    @Test
    void createClip_exceedsDuration_rejected() {
        FileAnalysisTask parent = buildParent(1L, 100L, 300);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(parent);

        CreateFileClipRequest req = new CreateFileClipRequest();
        req.setClipStart(250);
        req.setClipEnd(350); // exceeds 300s duration
        req.setClipCategory("RETENTION");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createClip(1L, req));
        assertTrue(ex.getMessage().contains("超出"));
    }

    @Test
    void createClip_parentNotFound() {
        when(fileAnalysisTaskMapper.selectById(999L)).thenReturn(null);

        CreateFileClipRequest req = new CreateFileClipRequest();
        req.setClipStart(10);
        req.setClipEnd(30);
        req.setClipCategory("RETENTION");

        assertThrows(BusinessException.class, () -> service.createClip(999L, req));
    }

    @Test
    void listClips_success() {
        FileAnalysisTask parent = buildParent(1L, 100L, 600);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(parent);
        when(fileAnalysisTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<FileAnalysisVO> result = service.listClips(1L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    private FileAnalysisTask buildParent(Long id, Long orgId, int duration) {
        FileAnalysisTask task = new FileAnalysisTask();
        task.setId(id);
        task.setUserId(1L);
        task.setOrgId(orgId);
        task.setFileName("video.mp4");
        task.setStorageKey("org100/user1/video.mp4");
        task.setFileSize(1024L * 1024);
        task.setDuration(duration);
        task.setAiModel("doubao");
        task.setStatus("completed");
        return task;
    }
}
