package com.dianjinshou.common.storage;

import java.io.InputStream;

public interface StorageService {

    /**
     * 上传文件到对象存储
     *
     * @param bucket      桶名称
     * @param key         对象键
     * @param inputStream 文件输入流
     * @param contentType MIME 类型
     * @return 存储的对象键
     */
    String upload(String bucket, String key, InputStream inputStream, String contentType);

    /**
     * 上传已知大小的文件
     */
    String upload(String bucket, String key, InputStream inputStream, long size, String contentType);

    /**
     * 下载文件
     *
     * @return 文件输入流（调用方负责关闭）
     */
    InputStream download(String bucket, String key);

    /**
     * 删除文件
     */
    void delete(String bucket, String key);

    /**
     * 获取预签名下载 URL
     *
     * @param expireSeconds 过期时间（秒）
     */
    String getPresignedUrl(String bucket, String key, int expireSeconds);

    /**
     * 检查文件是否存在
     */
    boolean exists(String bucket, String key);

    /**
     * 确保桶存在，不存在则创建
     */
    void ensureBucketExists(String bucket);

    /**
     * 服务端合并多个对象为一个（用于分块上传 complete）
     * MinIO 原生 composeObject，无需下载到本地。
     *
     * @param bucket     桶名称
     * @param targetKey  最终对象键
     * @param sourceKeys 源对象键列表（按顺序）
     */
    void composeObject(String bucket, String targetKey, java.util.List<String> sourceKeys);
}
