package com.dianjinshou.modules.settings.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.settings.dto.AccountSettingsRequest;
import com.dianjinshou.modules.settings.dto.SubAccountRequest;
import com.dianjinshou.modules.settings.service.SettingsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> getAccountSettings() {
        return ApiResponse.success(settingsService.getAccountSettings());
    }

    @PutMapping("/account")
    public ApiResponse<Void> updateAccountSettings(@RequestBody AccountSettingsRequest request) {
        settingsService.updateAccountSettings(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/membership")
    public ApiResponse<Map<String, Object>> getMembership() {
        return ApiResponse.success(settingsService.getMembership());
    }

    @GetMapping("/sub-accounts")
    public ApiResponse<List<Map<String, Object>>> getSubAccounts() {
        return ApiResponse.success(settingsService.getSubAccounts());
    }

    @PostMapping("/sub-accounts")
    public ApiResponse<Void> createSubAccount(@Valid @RequestBody SubAccountRequest request) {
        settingsService.createSubAccount(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/sub-accounts/{id}")
    public ApiResponse<Void> deleteSubAccount(@PathVariable Long id) {
        settingsService.deleteSubAccount(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = settingsService.uploadAvatar(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return ApiResponse.success(result);
    }

}
