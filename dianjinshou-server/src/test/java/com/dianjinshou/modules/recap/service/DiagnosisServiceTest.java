package com.dianjinshou.modules.recap.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.recap.vo.DiagnosisReportVO;
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
class DiagnosisServiceTest {

    @Mock private AnalysisTaskMapper analysisTaskMapper;
    @Mock private AnalysisTaskProducer analysisTaskProducer;

    private DiagnosisService service;

    @BeforeAll
    static void initTableInfo() {
        try {
            MapperBuilderAssistant a = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            a.setCurrentNamespace("com.dianjinshou.modules.recap.diagnosis.AnalysisTaskMapper");
            TableInfoHelper.initTableInfo(a, AnalysisTask.class);
        } catch (Exception ignored) { }
    }

    @BeforeEach
    void setUp() {
        service = new DiagnosisService(analysisTaskMapper, analysisTaskProducer);
        setCurrentUser(1L, "super_admin", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void generateDiagnosis_success() {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setOrgId(100L);
        task.setStatus("completed");
        task.setAiDiagnosis(null);

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        when(analysisTaskMapper.updateById(any(AnalysisTask.class))).thenReturn(1);

        DiagnosisReportVO report = service.generateDiagnosis(1L);

        assertNotNull(report);
        assertEquals("completed", report.getStatus());
        assertTrue(report.getOverallScore() > 0);
        assertEquals(12, report.getRadarLabels().size());
        assertEquals(12, report.getRadarData().size());
        verify(analysisTaskMapper).updateById(any(AnalysisTask.class));
    }

    @Test
    void generateDiagnosis_alreadyExists_returnsExisting() {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setOrgId(100L);
        task.setStatus("completed");
        task.setAiDiagnosis("{\"overallScore\":77}");

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        DiagnosisReportVO report = service.generateDiagnosis(1L);

        assertNotNull(report);
        assertEquals("completed", report.getStatus());
        // Should NOT update since diagnosis already exists
        verify(analysisTaskMapper, never()).updateById(any());
    }

    @Test
    void generateDiagnosis_notCompleted_throws() {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setOrgId(100L);
        task.setStatus("processing");

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.generateDiagnosis(1L));
    }

    @Test
    void getDiagnosis_notGenerated() {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setOrgId(100L);
        task.setAiDiagnosis(null);

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        DiagnosisReportVO report = service.getDiagnosis(1L);

        assertEquals("not_generated", report.getStatus());
    }

    // ========== QA 增量: 边界测试 ==========

    @Test
    void getDiagnosis_withExistingDiagnosis_returnsReport() {
        AnalysisTask task = new AnalysisTask();
        task.setId(1L);
        task.setOrgId(100L);
        task.setStatus("completed");
        task.setAiDiagnosis("{\"generated\":true}");

        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        DiagnosisReportVO report = service.getDiagnosis(1L);
        assertNotNull(report);
        assertEquals("completed", report.getStatus());
        assertNotNull(report.getRadarLabels());
        assertEquals(12, report.getRadarLabels().size());
    }

    @Test
    void generateDiagnosis_taskNotFound_throws() {
        when(analysisTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.generateDiagnosis(999L));
    }

    @Test
    void getHistoricalAvgScores_returnsEmpty() {
        assertTrue(service.getHistoricalAvgScores(1L).isEmpty());
    }
}
