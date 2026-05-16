package com.dianjinshou.modules.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianjinshou.modules.storage.entity.UploadTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UploadTaskMapper extends BaseMapper<UploadTask> {

    /** 原子自增 uploaded_parts，并把 status 设为 uploading（仅当 status 还在 init/uploading 时） */
    @Update("UPDATE upload_tasks SET uploaded_parts = uploaded_parts + 1, " +
            "status = CASE WHEN status IN ('init','uploading') THEN 'uploading' ELSE status END " +
            "WHERE id = #{uploadId}")
    int incrementUploadedParts(@Param("uploadId") Long uploadId);
}
