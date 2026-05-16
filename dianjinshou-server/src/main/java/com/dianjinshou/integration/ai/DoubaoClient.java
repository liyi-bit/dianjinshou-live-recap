package com.dianjinshou.integration.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubaoClient implements AiAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(DoubaoClient.class);

    private final String apiKey;
    private final String endpoint;

    public DoubaoClient(String apiKey, String endpoint) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public AiAnalysisResult analyze(Long userId, String asrText, String industry, String promptTemplate) {
        // TODO: Implement actual Doubao API call with SSE streaming
        log.warn("DoubaoClient not yet implemented, returning empty result");
        AiAnalysisResult result = new AiAnalysisResult();
        result.setConsumedChars(0);
        return result;
    }
}
