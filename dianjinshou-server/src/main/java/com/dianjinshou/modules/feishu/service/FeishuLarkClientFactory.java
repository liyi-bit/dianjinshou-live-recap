package com.dianjinshou.modules.feishu.service;

import com.lark.oapi.Client;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches one com.lark.oapi.Client (REST) per Feishu bot (app_id).
 * The SDK client internally caches tenant_access_token so sharing it across requests is intentional.
 */
@Component
public class FeishuLarkClientFactory {

    private final Map<String, Client> cache = new ConcurrentHashMap<>();

    public Client getOrCreate(String appId, String appSecret) {
        return cache.computeIfAbsent(appId, k -> Client.newBuilder(appId, appSecret).build());
    }

    public Client get(String appId) {
        return cache.get(appId);
    }

    public void evict(String appId) {
        cache.remove(appId);
    }
}
