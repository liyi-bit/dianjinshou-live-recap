package com.dianjinshou.modules.shortvideo.provider;

import com.dianjinshou.modules.shortvideo.entity.Creator;
import com.dianjinshou.modules.shortvideo.entity.CreatorVideo;

import java.util.List;

public interface CreatorDataProvider {

    List<Creator> searchCreators(String keyword, String platform, String industry,
                                 Long minFollowers, Long maxFollowers, int page, int size);

    List<CreatorVideo> getCreatorVideos(String platform, String creatorId, int limit);
}
