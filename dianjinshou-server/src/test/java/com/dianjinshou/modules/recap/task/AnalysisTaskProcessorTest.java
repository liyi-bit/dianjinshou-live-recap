package com.dianjinshou.modules.recap.task;

import com.dianjinshou.common.enums.AnalysisStatus;
import com.dianjinshou.common.enums.RecapType;
import com.dianjinshou.modules.auth.mapper.UserMapper;
import com.dianjinshou.modules.recap.entity.AnalysisTask;
import com.dianjinshou.modules.recap.mapper.AnalysisTaskMapper;
import com.dianjinshou.modules.recap.service.AiAnalysisService;
import com.dianjinshou.modules.recording.entity.Recording;
import com.dianjinshou.modules.recording.mapper.RecordingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisTaskProcessorTest {

    @Mock
    private AnalysisTaskMapper analysisTaskMapper;
    @Mock
    private RecordingMapper recordingMapper;
    @Mock
    private AiAnalysisService aiAnalysisService;
    @Mock
    private UserMapper userMapper;

    private AnalysisTaskProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AnalysisTaskProcessor(
                analysisTaskMapper,
                recordingMapper,
                aiAnalysisService,
                userMapper);
    }

    @Test
    void process_fullTaskSyncsRecordingStatus() {
        AnalysisTask task = buildTask(1L, RecapType.FULL.getCode());
        Recording recording = new Recording();
        recording.setId(10L);
        recording.setAnalysisStatus(AnalysisStatus.TRANSCRIBED.getCode());

        when(analysisTaskMapper.selectById(1L)).thenReturn(task);
        when(recordingMapper.selectById(10L)).thenReturn(recording);

        processor.process(1L);

        verify(recordingMapper).updateById(recording);
    }

    @Test
    void process_clipTaskDoesNotOverwriteRecordingStatus() {
        AnalysisTask task = buildTask(2L, RecapType.CLIP.getCode());
        when(analysisTaskMapper.selectById(2L)).thenReturn(task);

        processor.process(2L);

        verify(recordingMapper, never()).updateById(any(Recording.class));
    }

    @Test
    void process_failedClipTaskDoesNotMarkRecordingFailed() {
        AnalysisTask task = buildTask(3L, RecapType.CLIP.getCode());
        when(analysisTaskMapper.selectById(3L)).thenReturn(task);
        doThrow(new RuntimeException("AI error"))
                .when(aiAnalysisService).processAiAnalysis(task);

        processor.process(3L);

        verify(recordingMapper, never()).updateById(any(Recording.class));
    }

    private AnalysisTask buildTask(Long id, String type) {
        AnalysisTask task = new AnalysisTask();
        task.setId(id);
        task.setRecordingId(10L);
        task.setUserId(1L);
        task.setType(type);
        task.setStatus(AnalysisStatus.AI_PROCESSING.getCode());
        task.setConsumedChars(0L);
        return task;
    }
}
