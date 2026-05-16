package com.dianjinshou.modules.recap.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.recap.dto.CreateClipAnalysisRequest;
import com.dianjinshou.modules.recap.dto.CreateClipDraftRequest;
import com.dianjinshou.modules.recap.dto.CreateFullAnalysisRequest;
import com.dianjinshou.modules.recap.dto.SaveNoteRequest;
import com.dianjinshou.modules.recap.dto.SubmitAsrResultRequest;
import com.dianjinshou.modules.recap.dto.SubmitClipAsrRequest;
import com.dianjinshou.modules.recap.dto.SaveOptimizationRequest;
import com.dianjinshou.modules.recap.service.AnalysisService;
import com.dianjinshou.modules.recap.vo.AnalysisTaskCreateVO;
import com.dianjinshou.modules.recap.vo.AnalysisTaskVO;
import com.dianjinshou.modules.recap.vo.AsrParagraphVO;
import com.dianjinshou.modules.recap.vo.KeywordListVO;
import com.dianjinshou.modules.recap.vo.NoteVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/full")
    @RateLimit(max = 10, windowSeconds = 60, key = "analysis:create")
    public ApiResponse<AnalysisTaskCreateVO> createFullAnalysis(@Valid @RequestBody CreateFullAnalysisRequest req) {
        return ApiResponse.success(analysisService.createFullAnalysis(req));
    }

    @PostMapping("/clip")
    @RateLimit(max = 10, windowSeconds = 60, key = "analysis:create")
    public ApiResponse<AnalysisTaskCreateVO> createClipAnalysis(@Valid @RequestBody CreateClipAnalysisRequest req) {
        return ApiResponse.success(analysisService.createClipAnalysis(req));
    }

    @PostMapping("/submit-asr")
    @RateLimit(max = 10, windowSeconds = 60, key = "analysis:submit-asr")
    public ApiResponse<AnalysisTaskCreateVO> submitAsrResult(@Valid @RequestBody SubmitAsrResultRequest req) {
        return ApiResponse.success(analysisService.submitAsrAndAnalyze(req));
    }

    @PostMapping("/submit-clip-asr")
    @RateLimit(max = 10, windowSeconds = 60, key = "analysis:submit-clip-asr")
    public ApiResponse<AnalysisTaskCreateVO> submitClipAsrResult(@Valid @RequestBody SubmitClipAsrRequest req) {
        return ApiResponse.success(analysisService.submitClipAsrAndAnalyze(req));
    }

    /**
     * 切片占位：立即返回一条 status=transcribing 的 taskId，前端用它在切片复盘列表里立刻渲染"逐字稿生成中"。
     * 后续 ffmpeg 截取 + ASR 完成时通过 /submit-clip-asr 携带 taskId 回填。
     */
    @PostMapping("/clip/draft")
    @RateLimit(max = 30, windowSeconds = 60, key = "analysis:clip-draft")
    public ApiResponse<AnalysisTaskCreateVO> createClipDraft(@Valid @RequestBody CreateClipDraftRequest req) {
        return ApiResponse.success(analysisService.createClipDraft(req));
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<AnalysisTaskVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long streamerId,
            @RequestParam(required = false) String clipCategory,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(analysisService.listTasks(type, status, streamerId, clipCategory, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AnalysisTaskVO> detail(@PathVariable Long id) {
        return ApiResponse.success(analysisService.detail(id));
    }

    @GetMapping("/{id}/paragraphs")
    public ApiResponse<PageResult<AsrParagraphVO>> getParagraphs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.success(analysisService.getParagraphs(id, page, size));
    }

    @GetMapping("/{id}/keywords")
    public ApiResponse<KeywordListVO> getKeywords(
            @PathVariable Long id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(analysisService.getKeywords(id, type, category, page, size));
    }

    @GetMapping("/{id}/diagnosis")
    public ApiResponse<AnalysisTaskVO> getDiagnosis(@PathVariable Long id) {
        return ApiResponse.success(analysisService.getDiagnosis(id));
    }

    @PostMapping("/{id}/optimization")
    public ApiResponse<Void> saveOptimization(@PathVariable Long id,
                                              @Valid @RequestBody SaveOptimizationRequest req) {
        analysisService.saveOptimization(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/notes")
    public ApiResponse<NoteVO> getNotes(@PathVariable Long id,
                                        @RequestParam String tabType) {
        return ApiResponse.success(analysisService.getNotes(id, tabType));
    }

    @PutMapping("/{id}/notes")
    public ApiResponse<NoteVO> saveNotes(@PathVariable Long id,
                                         @Valid @RequestBody SaveNoteRequest req) {
        return ApiResponse.success(analysisService.saveNotes(id, req));
    }

    @PostMapping("/{id}/re-analyze")
    public ApiResponse<AnalysisTaskCreateVO> reAnalyze(@PathVariable Long id) {
        return ApiResponse.success(analysisService.reAnalyze(id));
    }

    /**
     * v1.1.0 新增：用户在逐字稿详情页手动触发 AI 复盘。
     * 配额超限会抛 DAILY_QUOTA_EXHAUSTED(40006)，前端据此弹提示。
     */
    @PostMapping("/{id}/start-ai")
    public ApiResponse<AnalysisTaskCreateVO> startAiAnalysis(@PathVariable Long id) {
        return ApiResponse.success(analysisService.startAiAnalysis(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        analysisService.cancel(id);
        return ApiResponse.success();
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int count = analysisService.batchDelete(ids);
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", count);
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}/progress")
    public ApiResponse<Void> getProgress(@PathVariable Long id) {
        // SSE endpoint placeholder — actual implementation in T22
        return ApiResponse.success();
    }

    @PostMapping("/{id}/optimized-text")
    @RateLimit(max = 5, windowSeconds = 60, key = "analysis:optimize")
    public ApiResponse<String> generateOptimizedText(@PathVariable Long id) {
        String optimizedText = analysisService.generateOptimizedText(id);
        return ApiResponse.success(optimizedText);
    }

    @PostMapping("/{id}/classify-paragraphs")
    public ApiResponse<Void> classifyParagraphs(@PathVariable Long id) {
        analysisService.classifyParagraphs(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/script-breakdown")
    public ApiResponse<Void> generateScriptBreakdown(@PathVariable Long id) {
        // SSE streaming placeholder — actual implementation in T22
        return ApiResponse.success();
    }
}
