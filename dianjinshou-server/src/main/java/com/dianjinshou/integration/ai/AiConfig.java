package com.dianjinshou.integration.ai;

import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiConfig.AiProperties.class)
public class AiConfig {

    @Bean
    @ConditionalOnProperty(name = "dianjinshou.ai.provider", havingValue = "mock", matchIfMissing = true)
    public AiAnalysisClient mockAiClient() {
        return new MockAiClient();
    }

    @Bean
    @ConditionalOnProperty(name = "dianjinshou.ai.provider", havingValue = "doubao")
    public AiAnalysisClient doubaoClient(AiProperties properties) {
        return new DoubaoClient(properties.getDoubaoApiKey(), properties.getDoubaoEndpoint());
    }

    @Bean
    @ConditionalOnProperty(name = "dianjinshou.ai.provider", havingValue = "deepseek")
    public AiAnalysisClient deepSeekClient(AiProperties properties) {
        return new DeepSeekClient(properties.getDeepseekApiKey(), properties.getDeepseekEndpoint());
    }

    @Bean
    @ConditionalOnProperty(name = "dianjinshou.ai.provider", havingValue = "yunwu")
    public AiAnalysisClient yunwuAiClient(ThirdPartySettings settings) {
        return new YunwuAiClient(settings);
    }

    @ConfigurationProperties(prefix = "dianjinshou.ai")
    public static class AiProperties {

        private String provider = "mock";
        private String doubaoApiKey;
        private String doubaoEndpoint;
        private String deepseekApiKey;
        private String deepseekEndpoint;
        private String yunwuApiKey;
        private String yunwuEndpoint = "https://api.yunwu.ai/v1";
        private String yunwuModel = "gpt-4o-mini";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDoubaoApiKey() {
            return doubaoApiKey;
        }

        public void setDoubaoApiKey(String doubaoApiKey) {
            this.doubaoApiKey = doubaoApiKey;
        }

        public String getDoubaoEndpoint() {
            return doubaoEndpoint;
        }

        public void setDoubaoEndpoint(String doubaoEndpoint) {
            this.doubaoEndpoint = doubaoEndpoint;
        }

        public String getDeepseekApiKey() {
            return deepseekApiKey;
        }

        public void setDeepseekApiKey(String deepseekApiKey) {
            this.deepseekApiKey = deepseekApiKey;
        }

        public String getDeepseekEndpoint() {
            return deepseekEndpoint;
        }

        public void setDeepseekEndpoint(String deepseekEndpoint) {
            this.deepseekEndpoint = deepseekEndpoint;
        }

        public String getYunwuApiKey() {
            return yunwuApiKey;
        }

        public void setYunwuApiKey(String yunwuApiKey) {
            this.yunwuApiKey = yunwuApiKey;
        }

        public String getYunwuEndpoint() {
            return yunwuEndpoint;
        }

        public void setYunwuEndpoint(String yunwuEndpoint) {
            this.yunwuEndpoint = yunwuEndpoint;
        }

        public String getYunwuModel() {
            return yunwuModel;
        }

        public void setYunwuModel(String yunwuModel) {
            this.yunwuModel = yunwuModel;
        }
    }
}
