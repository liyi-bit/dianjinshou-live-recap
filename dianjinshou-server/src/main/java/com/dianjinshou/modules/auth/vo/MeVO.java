package com.dianjinshou.modules.auth.vo;

public class MeVO {

    private UserVO user;
    private OrgInfo org;
    private VipInfo vipInfo;
    private QuotaInfo quotaInfo;

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public OrgInfo getOrg() {
        return org;
    }

    public void setOrg(OrgInfo org) {
        this.org = org;
    }

    public VipInfo getVipInfo() {
        return vipInfo;
    }

    public void setVipInfo(VipInfo vipInfo) {
        this.vipInfo = vipInfo;
    }

    public QuotaInfo getQuotaInfo() {
        return quotaInfo;
    }

    public void setQuotaInfo(QuotaInfo quotaInfo) {
        this.quotaInfo = quotaInfo;
    }

    public static class OrgInfo {
        private Long id;
        private String name;
        private Integer maxMembers;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getMaxMembers() {
            return maxMembers;
        }

        public void setMaxMembers(Integer maxMembers) {
            this.maxMembers = maxMembers;
        }
    }

    public static class VipInfo {
        private Integer level;
        private String name;
        private String expireAt;

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(String expireAt) {
            this.expireAt = expireAt;
        }
    }

    public static class QuotaInfo {
        private Long aiQuotaTotal;
        private Long aiQuotaUsed;
        private Long durationQuotaTotal;
        private Long durationQuotaUsed;

        public Long getAiQuotaTotal() {
            return aiQuotaTotal;
        }

        public void setAiQuotaTotal(Long aiQuotaTotal) {
            this.aiQuotaTotal = aiQuotaTotal;
        }

        public Long getAiQuotaUsed() {
            return aiQuotaUsed;
        }

        public void setAiQuotaUsed(Long aiQuotaUsed) {
            this.aiQuotaUsed = aiQuotaUsed;
        }

        public Long getDurationQuotaTotal() {
            return durationQuotaTotal;
        }

        public void setDurationQuotaTotal(Long durationQuotaTotal) {
            this.durationQuotaTotal = durationQuotaTotal;
        }

        public Long getDurationQuotaUsed() {
            return durationQuotaUsed;
        }

        public void setDurationQuotaUsed(Long durationQuotaUsed) {
            this.durationQuotaUsed = durationQuotaUsed;
        }
    }
}
