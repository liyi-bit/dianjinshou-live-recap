package com.dianjinshou.modules.streamer.controller;

import com.dianjinshou.common.response.ApiResponse;
import com.dianjinshou.modules.streamer.service.IndustryService;
import com.dianjinshou.modules.streamer.vo.IndustryTreeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/industries")
public class IndustryController {

    private final IndustryService industryService;

    public IndustryController(IndustryService industryService) {
        this.industryService = industryService;
    }

    @GetMapping
    public ApiResponse<List<IndustryTreeVO>> tree() {
        return ApiResponse.success(industryService.tree());
    }
}
