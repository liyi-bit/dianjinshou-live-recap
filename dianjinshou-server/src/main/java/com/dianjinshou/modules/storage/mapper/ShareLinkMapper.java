package com.dianjinshou.modules.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianjinshou.modules.storage.entity.ShareLink;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ShareLinkMapper extends BaseMapper<ShareLink> {

    @Update("UPDATE share_links SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE share_links SET download_count = download_count + 1 WHERE id = #{id}")
    int incrementDownloadCount(@Param("id") Long id);

    @Update("UPDATE share_links SET status = 'expired' WHERE id = #{id} AND status = 'active'")
    int markExpired(@Param("id") Long id);
}
