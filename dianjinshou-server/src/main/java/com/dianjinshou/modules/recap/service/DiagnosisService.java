package com.dianjinshou.modules.recap.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.OrgScopeHelper;
import com.dianjinshou.common.security.SecurityContextHelper;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.task.AnalysisTaskMessage;
import com.dianjinshou.modules.recap.task.AnalysisTaskProducer;
import com.dianjinshou.modules.recap.vo.DiagnosisReportVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

    private static final List<String> DIMENSION_LABELS = Arrays.asList(
            "开场话术", "产品介绍", "互动引导", "促单技巧", "节奏把控",
            "情绪感染力", "专业知识", "观众留存", "转化效率", "违规风险",
            "内容创意", "数据表现"
    );

    private final AnalysisTaskMapper analysisTaskMapper;
    private final AnalysisTaskProducer analysisTaskProducer;

    public DiagnosisService(AnalysisTaskMapper analysisTaskMapper,
                            AnalysisTaskProducer analysisTaskProducer) {
        this.analysisTaskMapper = analysisTaskMapper;
        this.analysisTaskProducer = analysisTaskProducer;
    }

    public DiagnosisReportVO generateDiagnosis(Long taskId) {
        AnalysisTask task = getTaskWithPermission(taskId);

        if (!"completed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "分析任务尚未完成");
        }

        // If diagnosis already exists, return it
        if (task.getAiDiagnosis() != null && !task.getAiDiagnosis().isEmpty()) {
            return parseDiagnosisReport(task);
        }

        // Generate placeholder diagnosis (AI async in production)
        String diagnosisJson = generatePlaceholderDiagnosis(task);
        task.setAiDiagnosis(diagnosisJson);
        analysisTaskMapper.updateById(task);

        log.info("Diagnosis generated for task {}", taskId);

        // In production, send RabbitMQ message for async AI generation
        // AnalysisTaskMessage msg = new AnalysisTaskMessage(taskId, task.getRecordingId(), "DIAGNOSIS", 3);
        // analysisTaskProducer.send(msg);

        return parseDiagnosisReport(task);
    }

    public DiagnosisReportVO getDiagnosis(Long taskId) {
        AnalysisTask task = getTaskWithPermission(taskId);

        if (task.getAiDiagnosis() == null || task.getAiDiagnosis().isEmpty()) {
            DiagnosisReportVO report = new DiagnosisReportVO();
            report.setTaskId(taskId);
            report.setStatus("not_generated");
            return report;
        }

        return parseDiagnosisReport(task);
    }

    public List<Integer> getHistoricalAvgScores(Long recordingId) {
        // Get all completed analyses for the same recording's streamer
        // For now return empty — in production this queries historical data
        return new ArrayList<Integer>();
    }

    private AnalysisTask getTaskWithPermission(Long taskId) {
        LambdaQueryWrapper<AnalysisTask> query = new LambdaQueryWrapper<>();
        query.eq(AnalysisTask::getId, taskId);
        OrgScopeHelper.applyOrgScope(query, AnalysisTask::getOrgId);
        AnalysisTask task = analysisTaskMapper.selectOne(query);

        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分析任务不存在或已被删除，请刷新列表");
        }
        return task;
    }

    private DiagnosisReportVO parseDiagnosisReport(AnalysisTask task) {
        DiagnosisReportVO report = new DiagnosisReportVO();
        report.setTaskId(task.getId());
        report.setStatus("completed");
        report.setRadarLabels(DIMENSION_LABELS);

        // Parse the JSON diagnosis — in production this is real AI output
        // For now generate structured placeholder
        List<DiagnosisReportVO.DimensionScore> dimensions = new ArrayList<DiagnosisReportVO.DimensionScore>();
        List<Integer> radarData = new ArrayList<Integer>();

        int totalScore = 0;
        int[] scores = {78, 82, 65, 71, 88, 75, 80, 69, 73, 92, 70, 76};
        for (int i = 0; i < DIMENSION_LABELS.size(); i++) {
            DiagnosisReportVO.DimensionScore ds = new DiagnosisReportVO.DimensionScore();
            ds.setName(DIMENSION_LABELS.get(i));
            ds.setScore(scores[i]);
            ds.setSuggestion("基于分析数据，" + DIMENSION_LABELS.get(i) + "维度建议进一步优化");
            radarData.add(scores[i]);
            totalScore += scores[i];
        }

        report.setDimensions(dimensions);
        report.setRadarData(radarData);
        report.setOverallScore(totalScore / DIMENSION_LABELS.size());
        report.setOverallComment("整体表现中等偏上，重点提升互动引导和观众留存维度");

        // Set dimensions after loop
        for (int i = 0; i < DIMENSION_LABELS.size(); i++) {
            DiagnosisReportVO.DimensionScore ds = new DiagnosisReportVO.DimensionScore();
            ds.setName(DIMENSION_LABELS.get(i));
            ds.setScore(scores[i]);
            ds.setSuggestion("基于分析数据，" + DIMENSION_LABELS.get(i) + "维度建议进一步优化");
            dimensions.add(ds);
        }

        return report;
    }

    private String generatePlaceholderDiagnosis(AnalysisTask task) {
        // In production, the AI model generates this JSON
        return "{\"overallScore\":77,\"comment\":\"整体表现中等偏上\",\"generated\":true}";
    }
}
