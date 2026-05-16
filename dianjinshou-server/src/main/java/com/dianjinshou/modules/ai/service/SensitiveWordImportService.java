package com.dianjinshou.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.modules.ai.entity.SensitiveWordLibrary;
import com.dianjinshou.modules.ai.mapper.SensitiveWordLibraryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Service
public class SensitiveWordImportService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordImportService.class);
    private static final int MAX_IMPORT_COUNT = 100000;

    private final SensitiveWordLibraryMapper sensitiveWordLibraryMapper;
    private final SensitiveWordEngine sensitiveWordEngine;

    public SensitiveWordImportService(SensitiveWordLibraryMapper sensitiveWordLibraryMapper,
                                       SensitiveWordEngine sensitiveWordEngine) {
        this.sensitiveWordLibraryMapper = sensitiveWordLibraryMapper;
        this.sensitiveWordEngine = sensitiveWordEngine;
    }

    public ImportResult importCsv(InputStream inputStream) {
        int imported = 0;
        int skipped = 0;
        int total = 0;

        Set<String> existingWords = loadExistingWords();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    if (line.startsWith("word") || line.startsWith("Word") || line.startsWith("\uFEFF")) {
                        continue;
                    }
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                total++;
                if (total > MAX_IMPORT_COUNT) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "导入数量不能超过" + MAX_IMPORT_COUNT + "条");
                }

                String[] parts = line.split(",", -1);
                String word = parts[0].trim();
                if (word.isEmpty()) continue;

                if (existingWords.contains(word)) {
                    skipped++;
                    continue;
                }

                String category = parts.length > 1 && !parts[1].trim().isEmpty() ? parts[1].trim() : "其他";
                int riskLevel = 1;
                if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                    try {
                        riskLevel = Integer.parseInt(parts[2].trim());
                        if (riskLevel < 1) riskLevel = 1;
                        if (riskLevel > 3) riskLevel = 3;
                    } catch (NumberFormatException ignored) { }
                }

                SensitiveWordLibrary sw = new SensitiveWordLibrary();
                sw.setWord(word);
                sw.setCategory(category);
                sw.setRiskLevel(riskLevel);
                sw.setPlatform("all");
                sw.setSource("system");
                sw.setIsActive(1);
                sensitiveWordLibraryMapper.insert(sw);
                existingWords.add(word);
                imported++;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV import failed", e);
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "导入失败: " + e.getMessage());
        }

        sensitiveWordEngine.reload();
        log.info("CSV import done: total={}, imported={}, skipped={}", total, imported, skipped);

        ImportResult result = new ImportResult();
        result.setTotal(total);
        result.setImported(imported);
        result.setSkipped(skipped);
        return result;
    }

    public ImportResult importTxt(InputStream inputStream, String defaultCategory) {
        int imported = 0;
        int skipped = 0;
        int total = 0;

        if (defaultCategory == null || defaultCategory.isEmpty()) {
            defaultCategory = "其他";
        }

        Set<String> existingWords = loadExistingWords();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim();
                }

                total++;
                if (total > MAX_IMPORT_COUNT) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "导入数量不能超过" + MAX_IMPORT_COUNT + "条");
                }

                if (existingWords.contains(line)) {
                    skipped++;
                    continue;
                }

                SensitiveWordLibrary sw = new SensitiveWordLibrary();
                sw.setWord(line);
                sw.setCategory(defaultCategory);
                sw.setRiskLevel(1);
                sw.setPlatform("all");
                sw.setSource("system");
                sw.setIsActive(1);
                sensitiveWordLibraryMapper.insert(sw);
                existingWords.add(line);
                imported++;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TXT import failed", e);
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "导入失败: " + e.getMessage());
        }

        sensitiveWordEngine.reload();
        log.info("TXT import done: total={}, imported={}, skipped={}", total, imported, skipped);

        ImportResult result = new ImportResult();
        result.setTotal(total);
        result.setImported(imported);
        result.setSkipped(skipped);
        return result;
    }

    private Set<String> loadExistingWords() {
        LambdaQueryWrapper<SensitiveWordLibrary> query = new LambdaQueryWrapper<>();
        query.select(SensitiveWordLibrary::getWord);
        Set<String> words = new HashSet<String>();
        for (SensitiveWordLibrary w : sensitiveWordLibraryMapper.selectList(query)) {
            words.add(w.getWord());
        }
        return words;
    }

    public static class ImportResult {
        private int total;
        private int imported;
        private int skipped;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getImported() { return imported; }
        public void setImported(int imported) { this.imported = imported; }

        public int getSkipped() { return skipped; }
        public void setSkipped(int skipped) { this.skipped = skipped; }
    }
}
