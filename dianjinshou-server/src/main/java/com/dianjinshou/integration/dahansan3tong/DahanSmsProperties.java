package com.dianjinshou.integration.dahansan3tong;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dianjinshou.sms")
public class DahanSmsProperties {

    private boolean enabled = false;
    private String bypassCode;
    private int codeTtlSeconds = 300;
    private int throttleSeconds = 60;
    private int phoneHourLimit = 10;
    private int phoneDayLimit = 20;
    private int ipDayLimit = 50;
    private final Dahan dahan = new Dahan();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBypassCode() { return bypassCode; }
    public void setBypassCode(String bypassCode) { this.bypassCode = bypassCode; }

    public int getCodeTtlSeconds() { return codeTtlSeconds; }
    public void setCodeTtlSeconds(int codeTtlSeconds) { this.codeTtlSeconds = codeTtlSeconds; }

    public int getThrottleSeconds() { return throttleSeconds; }
    public void setThrottleSeconds(int throttleSeconds) { this.throttleSeconds = throttleSeconds; }

    public int getPhoneHourLimit() { return phoneHourLimit; }
    public void setPhoneHourLimit(int phoneHourLimit) { this.phoneHourLimit = phoneHourLimit; }

    public int getPhoneDayLimit() { return phoneDayLimit; }
    public void setPhoneDayLimit(int phoneDayLimit) { this.phoneDayLimit = phoneDayLimit; }

    public int getIpDayLimit() { return ipDayLimit; }
    public void setIpDayLimit(int ipDayLimit) { this.ipDayLimit = ipDayLimit; }

    public Dahan getDahan() { return dahan; }

    public static class Dahan {
        private String endpoint = "https://dhst.bangkao.com/json/sms/Submit";
        private String account;
        private String password;
        private String sign = "点金手";
        private String template = "您的验证码是%s，5分钟内有效。请勿告知他人。";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 10000;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }

        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }

        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }
}
