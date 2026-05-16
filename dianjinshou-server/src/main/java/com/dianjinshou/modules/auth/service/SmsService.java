package com.dianjinshou.modules.auth.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.integration.dahansan3tong.DahanSmsClient;
import com.dianjinshou.integration.dahansan3tong.DahanSmsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final StringRedisTemplate redis;
    private final DahanSmsClient client;
    private final DahanSmsProperties props;

    public SmsService(StringRedisTemplate redis, DahanSmsClient client, DahanSmsProperties props) {
        this.redis = redis;
        this.client = client;
        this.props = props;
    }

    public int sendVerifyCode(String phone, String type, String clientIp) {
        // Track keys we incremented so we can roll back on failure (limit hit downstream or Dahan failure).
        List<String> incrementedCounters = new ArrayList<>();
        String throttleKey = throttleKey(phone, type);

        Boolean firstSend = redis.opsForValue().setIfAbsent(throttleKey, "1",
                Duration.ofSeconds(props.getThrottleSeconds()));
        if (Boolean.FALSE.equals(firstSend)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "请求过于频繁，请 " + props.getThrottleSeconds() + " 秒后再试");
        }

        try {
            checkAndIncrLimit(phoneHourKey(phone), props.getPhoneHourLimit(), 3600,
                    "该手机号本小时发送次数已达上限", incrementedCounters);
            checkAndIncrLimit(phoneDayKey(phone), props.getPhoneDayLimit(), 86400,
                    "该手机号今日发送次数已达上限", incrementedCounters);
            if (clientIp != null && !clientIp.isEmpty()) {
                checkAndIncrLimit(ipDayKey(clientIp), props.getIpDayLimit(), 86400,
                        "您所在网络今日发送次数已达上限", incrementedCounters);
            }
        } catch (BusinessException e) {
            redis.delete(throttleKey);
            rollback(incrementedCounters);
            throw e;
        }

        String code = String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
        String content = String.format(props.getDahan().getTemplate(), code);

        if (props.isEnabled()) {
            try {
                String msgId = client.send(phone, content);
                log.info("SMS sent to {} (type={}, ip={}, msgId={})",
                        maskPhone(phone), type, clientIp, msgId);
            } catch (Exception e) {
                redis.delete(throttleKey);
                rollback(incrementedCounters);
                log.error("SMS send failed for {} (type={}): {}", maskPhone(phone), type, e.getMessage());
                throw new BusinessException(ErrorCode.PARAM_ERROR, "短信发送失败，请稍后重试");
            }
        } else {
            log.warn("SMS provider disabled — DEV CODE for {} ({}) = {}", maskPhone(phone), type, code);
        }

        redis.opsForValue().set(codeKey(phone, type), code,
                Duration.ofSeconds(props.getCodeTtlSeconds()));
        return props.getCodeTtlSeconds();
    }

    private void checkAndIncrLimit(String key, int limit, int ttlSeconds, String overMsg,
                                   List<String> incremented) {
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            // Redis returned nothing — treat as failed limit check, fail safe by allowing.
            return;
        }
        incremented.add(key);
        if (count == 1L) {
            redis.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        if (count > limit) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, overMsg);
        }
    }

    private void rollback(List<String> keys) {
        for (String k : keys) {
            try {
                redis.opsForValue().decrement(k);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Verifies and consumes the code. Throws BusinessException if invalid.
     * Bypass code (if configured) is always accepted and does NOT consume any Redis state.
     */
    public void verifyCode(String phone, String type, String code) {
        if (code == null || code.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }
        if (props.getBypassCode() != null && props.getBypassCode().equals(code)) {
            return;
        }
        String key = codeKey(phone, type);
        String stored = redis.opsForValue().get(key);
        if (stored == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码已失效，请重新获取");
        }
        if (!stored.equals(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误");
        }
        redis.delete(key);
    }

    private String codeKey(String phone, String type) {
        return "sms:code:" + type + ":" + phone;
    }

    private String throttleKey(String phone, String type) {
        return "sms:throttle:" + type + ":" + phone;
    }

    private String phoneHourKey(String phone) {
        return "sms:phone:hour:" + phone;
    }

    private String phoneDayKey(String phone) {
        return "sms:phone:day:" + phone;
    }

    private String ipDayKey(String ip) {
        return "sms:ip:day:" + ip;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
