package com.dianjinshou.modules.recap.service;

import com.dianjinshou.integration.asr.AsrClient;
import com.dianjinshou.integration.asr.AsrClient.AsrSegmentResult;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.entity.AsrParagraph;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.mapper.AsrParagraphMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AsrService {

    private static final Logger log = LoggerFactory.getLogger(AsrService.class);
    private static final int MAX_RETRIES = 2;

    private final AsrClient asrClient;
    private final AsrParagraphMapper asrParagraphMapper;
    private final AnalysisTaskMapper analysisTaskMapper;

    public AsrService(AsrClient asrClient,
                      AsrParagraphMapper asrParagraphMapper,
                      AnalysisTaskMapper analysisTaskMapper) {
        this.asrClient = asrClient;
        this.asrParagraphMapper = asrParagraphMapper;
        this.analysisTaskMapper = analysisTaskMapper;
    }

    @Transactional
    public List<AsrParagraph> processAsr(AnalysisTask task, String audioFilePath) {
        List<AsrSegmentResult> segments = transcribeWithRetry(task.getUserId(), audioFilePath);

        // For clip tasks WITHOUT a dedicated clip file, filter segments to clipStart~clipEnd time range
        // If clipFilePath exists, the file is already trimmed — no filtering needed
        boolean isClip = "clip".equals(task.getType());
        boolean needsTimeFilter = isClip && (task.getClipFilePath() == null || task.getClipFilePath().isEmpty());
        Integer clipStartSec = task.getClipStart();
        Integer clipEndSec = task.getClipEnd();

        List<AsrParagraph> paragraphs = new ArrayList<>();
        int totalWordCount = 0;
        int paragraphIndex = 0;

        for (AsrSegmentResult segment : segments) {
            // If clip task, skip segments outside the clip time range
            if (needsTimeFilter && clipStartSec != null && clipEndSec != null) {
                // segment times are in "HH:mm:ss" or "mm:ss" format, convert to seconds
                int segStartSec = parseTimeToSeconds(segment.getStartTime());
                int segEndSec = parseTimeToSeconds(segment.getEndTime());
                // Skip if segment is completely outside clip range
                if (segEndSec <= clipStartSec || segStartSec >= clipEndSec) {
                    continue;
                }
            }

            AsrParagraph paragraph = new AsrParagraph();
            paragraph.setTaskId(task.getId());
            paragraph.setParagraphIndex(paragraphIndex++);
            paragraph.setStartTime(segment.getStartTime());
            paragraph.setEndTime(segment.getEndTime());
            paragraph.setTextContent(segment.getText());

            int wordCount = segment.getText().length();
            paragraph.setWordCount(wordCount);
            paragraph.setWordsPerMin(wordCount); // 1-minute segments
            paragraph.setIsHighlighted(0);

            asrParagraphMapper.insert(paragraph);
            paragraphs.add(paragraph);
            totalWordCount += wordCount;
        }

        // Update task with ASR results
        StringBuilder fullText = new StringBuilder();
        for (AsrParagraph p : paragraphs) {
            fullText.append(p.getTextContent()).append("\n");
        }
        task.setAsrText(fullText.toString().trim());
        task.setAsrWordCount(totalWordCount);
        analysisTaskMapper.updateById(task);

        log.info("ASR completed for taskId={}, type={}, paragraphs={}, totalWords={}",
                task.getId(), task.getType(), paragraphs.size(), totalWordCount);

        return paragraphs;
    }

    /**
     * Parse time string (HH:mm:ss or mm:ss) to total seconds.
     */
    private int parseTimeToSeconds(String time) {
        if (time == null || time.isEmpty()) return 0;
        String[] parts = time.split(":");
        try {
            if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse time '{}': {}", time, e.getMessage());
        }
        return 0;
    }

    private List<AsrSegmentResult> transcribeWithRetry(Long userId, String audioFilePath) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return asrClient.transcribe(userId, audioFilePath);
            } catch (Exception e) {
                lastException = e;
                log.warn("ASR attempt {} failed for {}: {}", attempt + 1, audioFilePath, e.getMessage());
            }
        }
        log.error("ASR failed after {} retries for {}", MAX_RETRIES + 1, audioFilePath, lastException);
        throw new RuntimeException("ASR转写失败，已重试" + MAX_RETRIES + "次: " +
                (lastException != null ? lastException.getMessage() : "未知错误"));
    }
}
