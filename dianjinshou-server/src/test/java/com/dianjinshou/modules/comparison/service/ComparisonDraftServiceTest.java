package com.dianjinshou.modules.comparison.service;

import com.dianjinshou.common.exception.BusinessException;
import com.dianjinshou.common.response.ErrorCode;
import com.dianjinshou.common.security.SecurityUser;
import com.dianjinshou.modules.comparison.dto.CreateDraftRequest;
import com.dianjinshou.modules.comparison.dto.SelectSecondRequest;
import com.dianjinshou.modules.comparison.entity.Comparison;
import com.dianjinshou.modules.comparison.entity.ComparisonDraft;
import com.dianjinshou.modules.comparison.mapper.ComparisonDraftMapper;
import com.dianjinshou.modules.comparison.mapper.ComparisonMapper;
import com.dianjinshou.modules.comparison.vo.ComparisonDraftVO;
import com.dianjinshou.modules.comparison.vo.ComparisonVO;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.junit.jupiter.api.AfterEach;
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
class ComparisonDraftServiceTest {

    @Mock
    private ComparisonDraftMapper draftMapper;
    @Mock
    private ComparisonMapper comparisonMapper;
    @Mock
    private RecordingMapper recordingMapper;

    @InjectMocks
    private ComparisonDraftService draftService;

    @BeforeEach
    void setUp() {
        SecurityUser user = new SecurityUser(1L, "admin", 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDraft_success() {
        Recording recording = buildRecording(10L, 5L);
        when(recordingMapper.selectById(10L)).thenReturn(recording);
        when(draftMapper.delete(any())).thenReturn(0);
        when(draftMapper.insert(any(ComparisonDraft.class))).thenReturn(1);

        CreateDraftRequest req = new CreateDraftRequest();
        req.setFirstRecordingId(10L);
        req.setListContext("AI_FULL_RECAP");

        ComparisonDraftVO result = draftService.createDraft(req);

        assertNotNull(result);
        assertEquals(10L, result.getFirstRecordingId());
        assertEquals("AI_FULL_RECAP", result.getListContext());
        verify(draftMapper).insert(any(ComparisonDraft.class));
    }

    @Test
    void createDraft_recordingNotFound() {
        when(recordingMapper.selectById(10L)).thenReturn(null);

        CreateDraftRequest req = new CreateDraftRequest();
        req.setFirstRecordingId(10L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class, () -> draftService.createDraft(req));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createDraft_crossOrgAccess() {
        Recording recording = buildRecording(10L, 99L); // different org
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        CreateDraftRequest req = new CreateDraftRequest();
        req.setFirstRecordingId(10L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class, () -> draftService.createDraft(req));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void selectSecond_crossListRejected() {
        ComparisonDraft draft = buildDraft(1L, 1L, 10L, "AI_FULL_RECAP");
        when(draftMapper.selectById(1L)).thenReturn(draft);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_CLIP");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.selectSecondAndConfirm(1L, req));
        assertEquals(ErrorCode.CROSS_LIST_COMPARISON, ex.getErrorCode());
    }

    @Test
    void selectSecond_draftExpired() {
        ComparisonDraft draft = buildDraft(1L, 1L, 10L, "AI_FULL_RECAP");
        draft.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        when(draftMapper.selectById(1L)).thenReturn(draft);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.selectSecondAndConfirm(1L, req));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATION, ex.getErrorCode());
        verify(draftMapper).deleteById(1L);
    }

    @Test
    void selectSecond_success_createsComparison() {
        ComparisonDraft draft = buildDraft(1L, 1L, 10L, "AI_FULL_RECAP");
        when(draftMapper.selectById(1L)).thenReturn(draft);

        Recording first = buildRecording(10L, 5L);
        first.setDuration(3600);
        Recording second = buildRecording(20L, 5L);
        second.setDuration(7200); // higher duration = reference

        when(recordingMapper.selectById(20L)).thenReturn(second);
        when(recordingMapper.selectById(10L)).thenReturn(first);
        when(comparisonMapper.insert(any(Comparison.class))).thenReturn(1);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_FULL_RECAP");

        ComparisonVO result = draftService.selectSecondAndConfirm(1L, req);

        assertNotNull(result);
        assertEquals("full", result.getType());
        assertEquals("pending", result.getStatus());
        verify(comparisonMapper).insert(any(Comparison.class));
        verify(draftMapper).deleteById(1L);
    }

    @Test
    void selectSecond_crossOrgBetweenRecordings() {
        ComparisonDraft draft = buildDraft(1L, 1L, 10L, "AI_FULL_RECAP");
        when(draftMapper.selectById(1L)).thenReturn(draft);

        Recording second = buildRecording(20L, 5L);
        when(recordingMapper.selectById(20L)).thenReturn(second);

        Recording first = buildRecording(10L, 99L); // different org
        when(recordingMapper.selectById(10L)).thenReturn(first);

        SelectSecondRequest req = new SelectSecondRequest();
        req.setSecondRecordingId(20L);
        req.setListContext("AI_FULL_RECAP");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> draftService.selectSecondAndConfirm(1L, req));
        assertEquals(ErrorCode.CROSS_ORG_ACCESS, ex.getErrorCode());
    }

    @Test
    void cancelCurrent_deletesUserDraft() {
        when(draftMapper.delete(any())).thenReturn(1);
        draftService.cancelCurrent();
        verify(draftMapper).delete(any());
    }

    @Test
    void getCurrent_returnsNullWhenNoDraft() {
        when(draftMapper.selectOne(any())).thenReturn(null);
        ComparisonDraftVO result = draftService.getCurrent();
        assertNull(result);
    }

    @Test
    void getCurrent_returnsNullWhenExpired() {
        ComparisonDraft draft = buildDraft(1L, 1L, 10L, "AI_FULL_RECAP");
        draft.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(draftMapper.selectOne(any())).thenReturn(draft);

        ComparisonDraftVO result = draftService.getCurrent();
        assertNull(result);
        verify(draftMapper).deleteById(1L);
    }

    // --- Helpers ---

    private Recording buildRecording(Long id, Long orgId) {
        Recording r = new Recording();
        r.setId(id);
        r.setUserId(1L);
        r.setOrgId(orgId);
        r.setDuration(3600);
        return r;
    }

    private ComparisonDraft buildDraft(Long id, Long userId, Long firstRecordingId, String listContext) {
        ComparisonDraft draft = new ComparisonDraft();
        draft.setId(id);
        draft.setUserId(userId);
        draft.setFirstRecordingId(firstRecordingId);
        draft.setListContext(listContext);
        draft.setExpiresAt(LocalDateTime.now().plusMinutes(25));
        return draft;
    }
}
