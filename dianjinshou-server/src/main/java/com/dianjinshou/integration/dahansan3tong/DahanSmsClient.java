package com.dianjinshou.integration.dahansan3tong;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.dianjinshou.modules.admin.service.ThirdPartySettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class DahanSmsClient {

    private static final Logger log = LoggerFactory.getLogger(DahanSmsClient.class);

    private final DahanSmsProperties.Dahan cfg;
    private final ThirdPartySettings settings;
    private final ObjectMapper mapper = new ObjectMapper();

    public DahanSmsClient(DahanSmsProperties.Dahan cfg, ThirdPartySettings settings) {
        this.cfg = cfg;
        this.settings = settings;
    }

    /**
     * Sends a single SMS. Returns Dahan's msgid on success; throws on failure.
     */
    public String send(String phone, String content) {
        String endpoint = settings.getSmsEndpoint();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("account", settings.getSmsAccount());
        body.put("password", SecureUtil.md5(settings.getSmsPassword()));
        body.put("phones", phone);
        body.put("content", content);
        body.put("sign", "【" + settings.getSmsSign() + "】");
        body.put("subcode", "");
        body.put("sendtime", "");

        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Dahan SMS request", e);
        }

        log.debug("Dahan SMS request to {}: phone={} contentLen={}", endpoint, phone, content.length());

        HttpResponse resp;
        try {
            resp = HttpRequest.post(endpoint)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(json)
                    .timeout(cfg.getReadTimeoutMs())
                    .setConnectionTimeout(cfg.getConnectTimeoutMs())
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Dahan SMS HTTP call failed: " + e.getMessage(), e);
        }

        if (!resp.isOk()) {
            throw new RuntimeException("Dahan SMS HTTP " + resp.getStatus() + ": " + resp.body());
        }

        String respBody = resp.body();
        log.debug("Dahan SMS response: {}", respBody);

        JsonNode node;
        try {
            node = mapper.readTree(respBody);
        } catch (Exception e) {
            throw new RuntimeException("Dahan SMS response is not JSON: " + respBody, e);
        }

        // Dahan typically returns { result: 0, desc: "...", msgid: "..." }; result=0 means success.
        // Some accounts return code/status fields instead. Treat any of {result,code,status,errCode}=0 as success.
        int result = firstIntField(node, "result", "code", "status", "errCode", -1);
        if (result != 0) {
            String desc = firstTextField(node, "desc", "message", "msg", "errMsg", respBody);
            throw new RuntimeException("Dahan SMS rejected: result=" + result + " desc=" + desc);
        }

        return firstTextField(node, "msgid", "msgId", "messageId", "id", "");
    }

    private static int firstIntField(JsonNode node, String f1, String f2, String f3, String f4, int defVal) {
        for (String f : new String[]{f1, f2, f3, f4}) {
            JsonNode v = node.get(f);
            if (v == null || v.isNull()) continue;
            if (v.canConvertToInt()) return v.asInt();
            try { return Integer.parseInt(v.asText().trim()); } catch (NumberFormatException ignored) {}
        }
        return defVal;
    }

    private static String firstTextField(JsonNode node, String f1, String f2, String f3, String f4, String defVal) {
        for (String f : new String[]{f1, f2, f3, f4}) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull()) return v.asText();
        }
        return defVal;
    }
}
