package com.dianjinshou.modules.recording.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianjinshou.modules.recording.entity.Recording;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecordingMapper extends BaseMapper<Recording> {

    /**
     * 跳过软删过滤的查询，专用于历史关联（如已软删的 recording 仍要展示主播信息）。
     * BaseMapper.selectById 自动注入 deleted=0 条件 → 软删后查不到，无法回填 anchor 快照。
     */
    @Select("SELECT * FROM recordings WHERE id = #{id} LIMIT 1")
    Recording selectByIdIncludeDeleted(@Param("id") Long id);
}
