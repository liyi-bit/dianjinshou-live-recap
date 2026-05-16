package com.dianjinshou.modules.admin.service;

import com.dianjinshou.modules.admin.entity.SystemSetting;
import com.dianjinshou.modules.admin.mapper.SystemSettingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached overrides for application.yml. DB rows in {@code system_settings} take precedence
 * over YAML defaults; clients call {@link #get(String, String)} with the YAML value as fallback.
 */
@Service
public class SystemSettingsService {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsService.class);

    private final SystemSettingMapper mapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public SystemSettingsService(SystemSettingMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void load() {
        List<SystemSetting> all = mapper.selectList(null);
        for (SystemSetting s : all) {
            if (s.getSettingValue() != null) cache.put(s.getSettingKey(), s.getSettingValue());
        }
        log.info("SettingsService loaded {} dynamic settings from DB", cache.size());
    }

    /** Returns the DB-stored value if present and non-empty, otherwise {@code fallback}. */
    public String get(String key, String fallback) {
        String v = cache.get(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    /** Snapshot of all dynamic settings (DB cache only — not merged with YAML). */
    public Map<String, String> getAll() {
        return new LinkedHashMap<>(cache);
    }

    /** Upsert a single setting, refreshing the cache atomically. {@code value} may be null/empty to delete the override. */
    public void set(String key, String value, Long updatedBy) {
        SystemSetting existing = mapper.selectById(key);
        if (value == null || value.isEmpty()) {
            cache.remove(key);
            if (existing != null) mapper.deleteById(key);
            return;
        }
        if (existing == null) {
            SystemSetting s = new SystemSetting();
            s.setSettingKey(key);
            s.setSettingValue(value);
            s.setUpdatedBy(updatedBy);
            s.setUpdatedAt(LocalDateTime.now());
            mapper.insert(s);
        } else {
            existing.setSettingValue(value);
            existing.setUpdatedBy(updatedBy);
            existing.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(existing);
        }
        cache.put(key, value);
    }

    /** Bulk update — applies all entries in a single pass. */
    public void setAll(Map<String, String> entries, Long updatedBy) {
        for (Map.Entry<String, String> e : entries.entrySet()) {
            set(e.getKey(), e.getValue(), updatedBy);
        }
    }
}
