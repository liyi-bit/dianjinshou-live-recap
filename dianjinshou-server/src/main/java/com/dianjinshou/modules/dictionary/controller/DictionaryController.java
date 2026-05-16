package com.dianjinshou.modules.dictionary.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.dictionary.entity.Dictionary;
import com.dianjinshou.modules.dictionary.entity.DictionaryKeyword;
import com.dianjinshou.modules.dictionary.service.DictionaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public ApiResponse<List<Dictionary>> list() {
        return ApiResponse.success(dictionaryService.listDictionaries());
    }

    @PostMapping
    public ApiResponse<Dictionary> create(@RequestBody Map<String, String> body) {
        return ApiResponse.success(
                dictionaryService.createDictionary(body.get("name"), body.get("description")));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        dictionaryService.updateDictionary(id, body.get("name"), body.get("description"));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dictionaryService.deleteDictionary(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/keywords")
    public ApiResponse<List<DictionaryKeyword>> listKeywords(@PathVariable Long id) {
        return ApiResponse.success(dictionaryService.listKeywords(id));
    }

    @PostMapping("/{id}/keywords")
    public ApiResponse<DictionaryKeyword> addKeyword(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        return ApiResponse.success(
                dictionaryService.addKeyword(id,
                        body.get("category"), body.get("subCategory"),
                        body.get("keyword"), body.get("description"),
                        body.get("replacementSuggestion")));
    }

    @DeleteMapping("/keywords/{keywordId}")
    public ApiResponse<Void> deleteKeyword(@PathVariable Long keywordId) {
        dictionaryService.deleteKeyword(keywordId);
        return ApiResponse.success(null);
    }
}
