package com.dianjinshou.modules.comparison;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.comparison.dto.CreateDraftRequest;
import com.dianjinshou.modules.comparison.dto.SelectSecondRequest;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.entity.ComparisonDraft;
import com.dianjinshou.modules.comparison.mapper.ComparisonDraftMapper;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.service.ComparisonDraftService;
import com.dianjinshou.modules.comparison.vo.ComparisonDraftVO;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonIntegrationTest {

    @Mock private ComparisonDraftMapper draftMapper;
    @Mock private ComparisonMapper comparisonMapper;
    @Mock private RecordingMapper recordingMapper;

    @InjectMocks
    private ComparisonDraftService draftService;

    @BeforeAll
    static void initCache() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, ComparisonDraft.class);
        TableInfoHelper.initTableInfo(assistant, Comparison.class);
        TableInfoHelper.initTableInfo(assistant, Recording.class);
    }

    @BeforeEach
    void setUp() {
        setSecurityContext(1L, "operator", 5L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullDraftFlow_lockFirst_selectSecond_createComparison() {
        // 1. Lock first recording
        Recording first = buildRecording(10L, 5L, 3600);
        when(recordingMapper.selectById(10L)).thenReturn(first);
        when(draftMapper.delete(any())).thenReturn(0);
        when(draftMapper.insert(any(ComparisonDraft.class))).thenReturn(1);

        CreateDraftRequest createReq = new CreateDraftRequest();
        createReq.setFirstRecordingId(10L);
        createReq.setListContext("AI_FULL_RECAP");

        ComparisonDraftVO draftVO = draftService.createDraft(createReq);

        assertNotNull(draftVO);
        assertEquals(10L, draftVO.getFirstRecordingId());

        // 2. Select second and confirm
        ComparisonDraft savedDraft = new ComparisonDraft();
        savedDraft.setId(100L);
        savedDraft.setUserId(1L);
        savedDraft.setFirstRecordingId(10L);
        savedDraft.setListContext("AI_FULL_RECAP");
        savedDraft.setExpiresAt(LocalDateTime.now().plusMinutes(25));

        Recording second = buildRecording(20L, 5L, 7200);
        when(draftMapper.selectById(100L)).thenReturn(savedDraft);
        when(recordingMapper.selectById(20L)).thenReturn(second);
        when(recordingMapper.selectById(10L)).thenReturn(first);
        when(comparisonMapper.insert(any(Comparison.class))).thenReturn(1);
        when(draftMapper.deleteById(100L)).thenReturn(1);

        SelectSecondRequest selectReq = new SelectSecondRequest();
        selectReq.setSecondRecordingId(20L);
        selectReq.setListContext("AI_FULL_RECAP");

        ComparisonVO comparisonVO = draftService.selectSecondAndConfirm(100L, selectReq);

        assertNotNull(comparisonVO);
        assertEquals("full", comparisonVO.getType());
        verify(draftMapper).deleteById(100L);
    }

    @Test
    void crossListComparison_rejected() {
        ComparisonDraft draft = new ComparisonDraft();
        draft.setId(100L);
        draft.setUserId(1L);
        draft.setFirstRecordingId(10L);
        draft.setListContext("AI_FULL_RECAP");
        draft.setExpiresAt(LocalDateTime.now().plusMinutes(25));

        when(draftMapper.selectById(100L)).thenReturn(draft);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_CLIP_RECAP"); // different context!

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.selectSecondAndConfirm(100L, req));
        assertEquals(ErrorCode.CROSS_LIST_COMPARISON.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void crossOrgComparison_rejected() {
        ComparisonDraft draft = new ComparisonDraft();
        draft.setId(100L);
        draft.setUserId(1L);
        draft.setFirstRecordingId(10L);
        draft.setListContext("AI_FULL_RECAP");
        draft.setExpiresAt(LocalDateTime.now().plusMinutes(25));

        Recording first = buildRecording(10L, 5L, 3600);
        Recording second = buildRecording(20L, 99L, 7200); // different org!

        when(draftMapper.selectById(100L)).thenReturn(draft);
        when(recordingMapper.selectById(20L)).thenReturn(second);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.selectSecondAndConfirm(100L, req));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void expiredDraft_rejected() {
        ComparisonDraft draft = new ComparisonDraft();
        draft.setId(100L);
        draft.setUserId(1L);
        draft.setFirstRecordingId(10L);
        draft.setListContext("AI_FULL_RECAP");
        draft.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // expired!

        when(draftMapper.selectById(100L)).thenReturn(draft);
        when(draftMapper.deleteById(100L)).thenReturn(1);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.selectSecondAndConfirm(100L, req));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void crossOrgRecordingAccess_rejected() {
        Recording recording = buildRecording(10L, 99L, 3600); // org 99, not 5
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateDraftRequest req = new CreateDraftRequest();
        req.setFirstRecordingId(10L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.createDraft(req));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS.getCode(), ex.getErrorCode().getCode());
    }

    @Test
    void cancelDraft_success() {
        when(draftMapper.delete(any())).thenReturn(1);
        draftService.cancelCurrent();
        verify(draftMapper).delete(any());
    }

    private void setSecurityContext(Long userId, String role, Long orgId) {
        SecurityUser su = new SecurityUser(userId, role, orgId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(su, null, Collections.emptyList()));
    }

    private Recording buildRecording(Long id, Long orgId, int duration) {
        Recording r = new Recording();
        r.setId(id);
        r.setOrgId(orgId);
        r.setDuration(duration);
        return r;
    }
}
