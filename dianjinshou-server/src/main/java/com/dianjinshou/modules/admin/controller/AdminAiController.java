package com.dianjinshou.modules.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.admin.service.AdminAiService;
import com.dianjinshou.modules.admin.vo.AiStatsVO;
import com.dianjinshou.modules.ai.entity.ScriptTemplate;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.service.SensitiveWordImportService;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AdminAiController {

    private final AdminAiService adminAiService;
    private final SensitiveWordImportService sensitiveWordImportService;

    public AdminAiController(AdminAiService adminAiService,
                              SensitiveWordImportService sensitiveWordImportService) {
        this.adminAiService = adminAiService;
        this.sensitiveWordImportService = sensitiveWordImportService;
    }

    @GetMapping("/stats")
    public ApiResponse<AiStatsVO> stats() {
        return ApiResponse.success(adminAiService.getStats());
    }

    @GetMapping("/sensitive-words")
    public ApiResponse<Page<SensitiveWordLibrary>> sensitiveWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminAiService.listSensitiveWords(page, size, category, keyword));
    }

    @PostMapping("/sensitive-words/import")
    public ApiResponse<SensitiveWordImportService.ImportResult> importSensitiveWords(
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

    @GetMapping("/script-templates")
    public ApiResponse<Page<ScriptTemplate>> scriptTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminAiService.listScriptTemplates(page, size));
    }

    @PutMapping("/script-templates/{id}")
    public ApiResponse<Void> updateScriptTemplate(
            @PathVariable Long id,
            @RequestBody ScriptTemplate update) {
        adminAiService.updateScriptTemplate(id, update);
        return ApiResponse.success();
    }

    @PutMapping("/script-templates/{id}/toggle")
    public ApiResponse<Void> toggleScriptTemplate(@PathVariable Long id) {
        adminAiService.toggleScriptTemplate(id);
        return ApiResponse.success();
    }
}
