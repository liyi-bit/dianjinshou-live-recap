package com.dianjinshou.modules.recap.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisTaskHandlerTest {

    @Mock
    private AnalysisTaskProcessor processor;

    @InjectMocks
    private AnalysisTaskHandler handler;

    @Test
    void handle_completesSuccessfully() {
        AnalysisTaskMessage message = new AnalysisTaskMessage(1L, 10L, "full", 5);
        handler.handle(message);
        verify(processor).process(1L);
    }

    @Test
    void handle_taskNotFound() {
        doThrow(new IllegalArgumentException("Task not found"))
                .when(processor).process(999L);

        AnalysisTaskMessage message = new AnalysisTaskMessage(999L, 10L, "full", 5);
        try {
            handler.handle(message);
        } catch (IllegalArgumentException ignored) {
        }
        verify(processor).process(999L);
    }

    @Test
    void handle_clipTask() {
        AnalysisTaskMessage message = new AnalysisTaskMessage(2L, 10L, "clip", 3);
        handler.handle(message);
        verify(processor).process(2L);
    }
}
