package com.dianjinshou.modules.client.vo;

/**
 * 客户端版本信息。客户端启动时请求此接口，若本地 version < minVersion，
 * 则显示强制升级阻塞窗，用户只能选择升级或退出。
 */
public class ClientVersionVO {
    /** 当前线上最新版本（仅展示用）。 */
    private String latestVersion;
    /** 强制升级阈值。本地版本 < 此值 → 强制升级。 */
    private String minVersion;
    /** 安装包下载地址（客户端 fallback，autoUpdater 走 publish feed）。 */
    private String downloadUrl;

    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
    public String getMinVersion() { return minVersion; }
    public void setMinVersion(String minVersion) { this.minVersion = minVersion; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
