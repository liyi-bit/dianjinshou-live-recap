package com.dianjinshou.integration.asr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AsrConfig.AsrProperties.class)
public class AsrConfig {

    @Bean
    public AsrClient disabledAsrClient() {
        return new DisabledAsrClient();
    }

    @ConfigurationProperties(prefix = "dianjinshou.asr")
    public static class AsrProperties {

        private String provider = "disabled";
        private String ffmpegPath;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFfmpegPath() {
            return ffmpegPath;
        }

        public void setFfmpegPath(String ffmpegPath) {
            this.ffmpegPath = ffmpegPath;
        }
    }
}
