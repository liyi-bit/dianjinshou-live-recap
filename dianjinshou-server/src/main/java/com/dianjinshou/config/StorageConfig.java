package com.dianjinshou.config;

import com.dianjinshou.common.storage.MinioStorageService;
import com.dianjinshou.common.storage.StorageProperties;
import com.dianjinshou.common.storage.StorageService;
import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "minio", matchIfMissing = true)
    public StorageService storageService(StorageProperties properties, ThirdPartySettings settings) {
        return new MinioStorageService(properties, settings);
    }
}
