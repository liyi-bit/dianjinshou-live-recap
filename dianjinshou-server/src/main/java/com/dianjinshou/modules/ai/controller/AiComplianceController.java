package com.dianjinshou.modules.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.RateLimit;
import com.dianjinshou.modules.ai.dto.ComplianceCheckRequest;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.service.ComplianceCheckService;
import com.dianjinshou.modules.ai.service.SensitiveWordImportService;
import com.dianjinshou.modules.ai.vo.ComplianceCheckResultVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai/compliance")
public class AiComplianceController {

    private final ComplianceCheckService complianceCheckService;
    private final SensitiveWordImportService sensitiveWordImportService;

    public AiComplianceController(ComplianceCheckService complianceCheckService,
                                   SensitiveWordImportService sensitiveWordImportService) {
        this.complianceCheckService = complianceCheckService;
        this.sensitiveWordImportService = sensitiveWordImportService;
    }

    @PostMapping("/check")
    @RateLimit(max = 20, windowSeconds = 60, key = "ai:compliance")
    public ApiResponse<ComplianceCheckResultVO> check(@Valid @RequestBody ComplianceCheckRequest request) {
        return ApiResponse.success(complianceCheckService.check(request));
    }

    @GetMapping("/library")
    public ApiResponse<Page<SensitiveWordLibrary>> listWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(complianceCheckService.listWords(page, size, category, keyword));
    }

    @PostMapping("/library")
    public ApiResponse<SensitiveWordLibrary> addWord(@RequestBody SensitiveWordLibrary word) {
        return ApiResponse.success(complianceCheckService.addWord(word));
    }

    @PutMapping("/library/{id}")
    public ApiResponse<Void> updateWord(@PathVariable Long id, @RequestBody SensitiveWordLibrary word) {
        complianceCheckService.updateWord(id, word);
        return ApiResponse.success();
    }

    @DeleteMapping("/library/{id}")
    public ApiResponse<Void> deleteWord(@PathVariable Long id) {
        complianceCheckService.deleteWord(id);
        return ApiResponse.success();
    }

    @PostMapping("/library/import")
    public ApiResponse<SensitiveWordImportService.ImportResult> importWords(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "其他") String category) {
        String filename = file.getOriginalFilename();
        try {
            if (filename != null && filename.endsWith(".csv")) {
                return ApiResponse.success(sensitiveWordImportService.importCsv(file.getInputStream()));
            } else if (filename != null && filename.endsWith(".txt")) {
                return ApiResponse.success(sensitiveWordImportService.importTxt(file.getInputStream(), category));
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持CSV和TXT文件");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "文件读取失败");
        }
    }
}
