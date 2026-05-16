package com.dianjinshou.modules.fileanalysis.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.fileanalysis.dto.CreateFileAnalysisRequest;
import com.dianjinshou.modules.fileanalysis.entity.FileAnalysisTask;
import com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper;
import com.dianjinshou.modules.fileanalysis.vo.FileAnalysisVO;
import com.dianjinshou.modules.recap.task.AnalysisTaskMessage;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileAnalysisServiceTest {

    @Mock
    private FileAnalysisTaskMapper fileAnalysisTaskMapper;
    @Mock
    private AnalysisTaskProducer analysisTaskProducer;

    private FileAnalysisService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("com.dianjinshou.modules.fileanalysis.mapper.FileAnalysisTaskMapper");
        TableInfoHelper.initTableInfo(assistant, FileAnalysisTask.class);
    }

    @BeforeEach
    void setUp() {
        service = new FileAnalysisService(fileAnalysisTaskMapper, analysisTaskProducer);
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createAnalysis_success() {
        when(fileAnalysisTaskMapper.insert(any(FileAnalysisTask.class))).thenAnswer(inv -> {
            FileAnalysisTask task = inv.getArgument(0);
            task.setId(1L);
            return 1;
        });

        CreateFileAnalysisRequest req = new CreateFileAnalysisRequest();
        req.setFileName("test.mp4");
        req.setStorageKey("org100/user1/test.mp4");
        req.setAiModel("deepseek_r1");

        FileAnalysisVO result = service.createAnalysis(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("pending", result.getStatus());
        assertEquals("deepseek_r1", result.getAiModel());

        ArgumentCaptor<AnalysisTaskMessage> captor = ArgumentCaptor.forClass(AnalysisTaskMessage.class);
        verify(analysisTaskProducer).send(captor.capture());
        assertEquals("FILE_ANALYSIS", captor.getValue().getType());
        assertEquals(1L, captor.getValue().getFileAnalysisTaskId());
    }

    @Test
    void createAnalysis_defaultAiModel() {
        when(fileAnalysisTaskMapper.insert(any(FileAnalysisTask.class))).thenAnswer(inv -> {
            FileAnalysisTask task = inv.getArgument(0);
            task.setId(2L);
            return 1;
        });

        CreateFileAnalysisRequest req = new CreateFileAnalysisRequest();
        req.setFileName("test.mp4");
        req.setStorageKey("key");

        FileAnalysisVO result = service.createAnalysis(req);

        assertEquals("doubao", result.getAiModel());
    }

    @Test
    void listAnalyses_withFilters() {
        Page<FileAnalysisTask> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(fileAnalysisTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<FileAnalysisVO> result = service.listAnalyses(1, 20, "completed", "test");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void getAnalysis_success() {
        FileAnalysisTask task = buildTask(1L, 100L);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(task);

        FileAnalysisVO result = service.getAnalysis(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getAnalysis_notFound() {
        when(fileAnalysisTaskMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getAnalysis(999L));
    }

    @Test
    void getAnalysis_crossOrg_rejected() {
        FileAnalysisTask task = buildTask(1L, 200L);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.getAnalysis(1L));
    }

    @Test
    void deleteAnalysis_success() {
        FileAnalysisTask task = buildTask(1L, 100L);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(task);
        when(fileAnalysisTaskMapper.deleteById(1L)).thenReturn(1);

        service.deleteAnalysis(1L);

        verify(fileAnalysisTaskMapper).deleteById(1L);
    }

    @Test
    void deleteAnalysis_crossOrg_rejected() {
        FileAnalysisTask task = buildTask(1L, 200L);
        when(fileAnalysisTaskMapper.selectById(1L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.deleteAnalysis(1L));
    }

    private FileAnalysisTask buildTask(Long id, Long orgId) {
        FileAnalysisTask task = new FileAnalysisTask();
        task.setId(id);
        task.setUserId(1L);
        task.setOrgId(orgId);
        task.setFileName("test.mp4");
        task.setStorageKey("org100/user1/test.mp4");
        task.setFileSize(1024L);
        task.setAiModel("doubao");
        task.setStatus("completed");
        return task;
    }
}
