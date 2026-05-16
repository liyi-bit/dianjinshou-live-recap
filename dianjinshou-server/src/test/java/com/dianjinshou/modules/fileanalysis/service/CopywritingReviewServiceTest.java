package com.dianjinshou.modules.fileanalysis.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.fileanalysis.dto.CopywritingReviewRequest;
import com.dianjinshou.modules.fileanalysis.entity.CopywritingReview;
import com.dianjinshou.modules.fileanalysis.mapper.CopywritingReviewMapper;
import com.dianjinshou.modules.fileanalysis.vo.CopywritingReviewVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CopywritingReviewServiceTest {

    @Mock
    private CopywritingReviewMapper copywritingReviewMapper;

    private CopywritingReviewService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("com.dianjinshou.modules.fileanalysis.mapper.CopywritingReviewMapper");
        TableInfoHelper.initTableInfo(assistant, CopywritingReview.class);
    }

    @BeforeEach
    void setUp() {
        service = new CopywritingReviewService(copywritingReviewMapper, new ObjectMapper());
        setCurrentUser(1L, "operator", 100L);
    }

    private void setCurrentUser(Long userId, String role, Long orgId) {
        SecurityUser user = new SecurityUser(userId, role, orgId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void submitReview_success() {
        when(copywritingReviewMapper.insert(any(CopywritingReview.class))).thenAnswer(inv -> {
            CopywritingReview r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        when(copywritingReviewMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        CopywritingReviewRequest req = new CopywritingReviewRequest();
        req.setTextContent("今天的直播间福利超级多，买到就是赚到");
        req.setCheckSensitive(true);
        req.setCheckCompliance(true);

        CopywritingReviewVO result = service.submitReview(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("completed", result.getStatus());
        assertEquals(0, result.getRiskScore()); // no sensitive words detected in placeholder
        assertNotNull(result.getResult());
    }

    @Test
    void submitReview_withIndustry() {
        when(copywritingReviewMapper.insert(any(CopywritingReview.class))).thenAnswer(inv -> {
            CopywritingReview r = inv.getArgument(0);
            r.setId(2L);
            return 1;
        });
        when(copywritingReviewMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        CopywritingReviewRequest req = new CopywritingReviewRequest();
        req.setTextContent("全网最低价");
        req.setIndustryId(5L);

        CopywritingReviewVO result = service.submitReview(req);

        assertNotNull(result);
        assertEquals("completed", result.getStatus());
    }

    @Test
    void submitReview_sensitiveOnly() {
        when(copywritingReviewMapper.insert(any(CopywritingReview.class))).thenAnswer(inv -> {
            CopywritingReview r = inv.getArgument(0);
            r.setId(3L);
            return 1;
        });
        when(copywritingReviewMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        CopywritingReviewRequest req = new CopywritingReviewRequest();
        req.setTextContent("测试文案");
        req.setCheckSensitive(true);
        req.setCheckCompliance(false);

        CopywritingReviewVO result = service.submitReview(req);

        assertNotNull(result);
        assertTrue(result.getResult().contains("sensitiveWords"));
    }

    @Test
    void listReviews_success() {
        Page<CopywritingReview> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(copywritingReviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Page<CopywritingReviewVO> result = service.listReviews(1, 20);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void submitReview_riskScoreCalcZero() {
        when(copywritingReviewMapper.insert(any(CopywritingReview.class))).thenAnswer(inv -> {
            CopywritingReview r = inv.getArgument(0);
            r.setId(4L);
            return 1;
        });
        when(copywritingReviewMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        CopywritingReviewRequest req = new CopywritingReviewRequest();
        req.setTextContent("正常文案内容");

        CopywritingReviewVO result = service.submitReview(req);

        assertEquals(0, result.getRiskScore());
    }
}
