package com.dianjinshou.config;

import com.dianjinshou.common.storage.CosProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(CosProperties.class)
public class CosConfig implements ApplicationRunner {

    private final CosProperties properties;
    private final Environment environment;

    public CosConfig(CosProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }
        if (!properties.isEnabled()
                || isBlank(properties.getSecretId())
                || isBlank(properties.getSecretKey())
                || isBlank(properties.getBucket())
                || isBlank(properties.getRegion())) {
            throw new IllegalStateException("Production profile requires tencent.cos enabled, secret-id, secret-key, bucket and region");
        }
    }

    private boolean isProdProfile() {
        String[] profiles = environment.getActiveProfiles();
        for (String profile : profiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
