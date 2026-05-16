package com.dianjinshou.integration.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeepSeekClient implements AiAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final String apiKey;
    private final String endpoint;

    public DeepSeekClient(String apiKey, String endpoint) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public AiAnalysisResult analyze(Long userId, String asrText, String industry, String promptTemplate) {
        // TODO: Implement actual DeepSeek R1 API call with thinking field
        log.warn("DeepSeekClient not yet implemented, returning empty result");
        AiAnalysisResult result = new AiAnalysisResult();
        result.setConsumedChars(0);
        return result;
    }
}
