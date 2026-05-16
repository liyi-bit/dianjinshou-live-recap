package com.dianjinshou.integration.dahansan3tong;

import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DahanSmsProperties.class)
public class DahanSmsConfig {

    @Bean
    public DahanSmsClient dahanSmsClient(DahanSmsProperties props, ThirdPartySettings settings) {
        return new DahanSmsClient(props.getDahan(), settings);
    }
}
