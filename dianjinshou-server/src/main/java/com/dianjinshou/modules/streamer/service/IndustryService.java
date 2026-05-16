package com.dianjinshou.modules.streamer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianjinshou.modules.streamer.entity.Industry;
import com.dianjinshou.modules.streamer.mapper.IndustryMapper;
import com.dianjinshou.modules.streamer.vo.IndustryTreeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndustryService {

    private final IndustryMapper industryMapper;

    public IndustryService(IndustryMapper industryMapper) {
        this.industryMapper = industryMapper;
    }

    public List<IndustryTreeVO> tree() {
        List<Industry> all = industryMapper.selectList(
                new LambdaQueryWrapper<Industry>().orderByAsc(Industry::getSortOrder, Industry::getId));

        Map<Long, IndustryTreeVO> map = new HashMap<>();
        List<IndustryTreeVO> roots = new ArrayList<>();

        for (Industry industry : all) {
            IndustryTreeVO vo = IndustryTreeVO.fromEntity(industry);
            map.put(industry.getId(), vo);

            if (industry.getParentId() == null || industry.getLevel() == 1) {
                roots.add(vo);
            }
        }

        for (Industry industry : all) {
            if (industry.getParentId() != null && industry.getLevel() != null && industry.getLevel() == 2) {
                IndustryTreeVO parent = map.get(industry.getParentId());
                if (parent != null) {
                    parent.getChildren().add(map.get(industry.getId()));
                }
            }
        }

        return roots;
    }

    public String getIndustryName(Long industryId) {
        if (industryId == null) {
            return null;
        }
        Industry industry = industryMapper.selectById(industryId);
        return industry != null ? industry.getName() : null;
    }
}
