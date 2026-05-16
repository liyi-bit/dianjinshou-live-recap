package com.dianjinshou.modules.streamer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.common.response.PageResult;
import com.dianjinshou.modules.streamer.entity.Streamer;
import com.dianjinshou.modules.streamer.mapper.StreamerMapper;
import com.dianjinshou.modules.streamer.vo.PublicStreamerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 公开的主播浏览接口 — 不需要登录，不按 orgId 过滤，返回全表主播。
 * 字段经过 {@link PublicStreamerVO} 裁剪，不含 orgId / userId / secUid。
 */
@RestController
@RequestMapping("/api/v1/public/streamers")
public class PublicStreamerController {

    private final StreamerMapper mapper;

    public PublicStreamerController(StreamerMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<PageResult<PublicStreamerVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;

        LambdaQueryWrapper<Streamer> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.like(Streamer::getAnchorName, keyword.trim());
        }
        w.orderByDesc(Streamer::getCreatedAt);

        Page<Streamer> p = new Page<>(page, size);
        Page<Streamer> result = mapper.selectPage(p, w);

        List<PublicStreamerVO> items = new ArrayList<>();
        for (Streamer s : result.getRecords()) {
            items.add(PublicStreamerVO.fromEntity(s));
        }
        return ApiResponse.success(PageResult.of(items, result.getTotal(), page, size));
    }
}
