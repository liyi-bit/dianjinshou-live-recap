package com.dianjinshou.modules.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianjinshou.modules.storage.entity.CloudFile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CloudFileMapper extends BaseMapper<CloudFile> {

    /**
     * 用户已占空间统计（含 active + queued + uploading，避免上传中文件超配）
     * deleted/failed/quota_exceeded 不计入。
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM cloud_files " +
            "WHERE user_id = #{userId} AND deleted = 0 " +
            "AND status IN ('active', 'queued', 'uploading')")
    long sumUsedBytesByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM cloud_files " +
            "WHERE user_id = #{userId} AND deleted = 0 AND status = 'active'")
    long countActiveByUser(@Param("userId") Long userId);
}
