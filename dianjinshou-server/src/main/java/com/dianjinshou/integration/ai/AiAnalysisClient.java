package com.dianjinshou.integration.ai;

/**
 * AI 分析客户端接口。所有方法都需要 {@code userId}，用于在实现层从
 * {@code ThirdPartySettings} 读取该用户的云雾 API Key / Endpoint / Model 等配置。
 * 调用方必须能明确知道当前是哪个用户发起的请求（从 SecurityContext 或 task 对象拿）。
 */
public interface AiAnalysisClient {

    AiAnalysisResult analyze(Long userId, String asrText, String industry, String promptTemplate);

    /**
     * Optimize ASR transcription text: fix punctuation, remove filler words,
     * improve sentence structure while preserving original meaning.
     */
    default String optimizeText(Long userId, String asrText) {
        // Default implementation returns original text
        return asrText;
    }

    /**
     * Classify each paragraph into a script category.
     * @param userId the user whose API key to use
     * @param paragraphTexts list of paragraph texts (index-aligned)
     * @return list of category names (same size, index-aligned), one of:
     *   话题话术, 行动指令, 留人话术, 塑品话术, 互动话术, 促单话术, 逼单话术, 福利话术, 开场话术, 人设话术
     */
    default java.util.List<String> classifyParagraphs(Long userId, java.util.List<String> paragraphTexts) {
        // Default: return null to indicate not supported
        return null;
    }

    class AiAnalysisResult {

        private String aiResult;
        private String aiDiagnosis;
        private String keywordSummary;
        private String contentCompass;
        private String summary;
        private long consumedChars;

        public String getAiResult() {
            return aiResult;
        }

        public void setAiResult(String aiResult) {
            this.aiResult = aiResult;
        }

        public String getAiDiagnosis() {
            return aiDiagnosis;
        }

        public void setAiDiagnosis(String aiDiagnosis) {
            this.aiDiagnosis = aiDiagnosis;
        }

        public String getKeywordSummary() {
            return keywordSummary;
        }

        public void setKeywordSummary(String keywordSummary) {
            this.keywordSummary = keywordSummary;
        }

        public String getContentCompass() {
            return contentCompass;
        }

        public void setContentCompass(String contentCompass) {
            this.contentCompass = contentCompass;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public long getConsumedChars() {
            return consumedChars;
        }

        public void setConsumedChars(long consumedChars) {
            this.consumedChars = consumedChars;
        }
    }
}
