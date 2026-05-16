package com.dianjinshou.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianjinshou.modules.admin.entity.UserThirdPartySetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserThirdPartySettingMapper extends BaseMapper<UserThirdPartySetting> {

    @Select("SELECT user_id, setting_key, setting_value, updated_at " +
            "FROM user_third_party_settings WHERE user_id = #{userId}")
    List<UserThirdPartySetting> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT setting_value FROM user_third_party_settings " +
            "WHERE user_id = #{userId} AND setting_key = #{key}")
    String selectValue(@Param("userId") Long userId, @Param("key") String key);
}
