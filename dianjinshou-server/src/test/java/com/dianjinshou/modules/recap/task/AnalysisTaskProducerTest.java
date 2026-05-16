package com.dianjinshou.modules.recap.task;

import com.dianjinshou.config.RabbitMqConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisTaskProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AnalysisTaskProducer producer;

    @Test
    void send_sendsMessageToCorrectExchange() {
        AnalysisTaskMessage message = new AnalysisTaskMessage(1L, 10L, "full", 5);

        producer.send(message);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.ANALYSIS_EXCHANGE),
                eq(RabbitMqConfig.ANALYSIS_ROUTING_KEY),
                eq(message),
                any(MessagePostProcessor.class));
    }

    @Test
    void send_withNullPriority() {
        AnalysisTaskMessage message = new AnalysisTaskMessage(2L, 20L, "clip", null);

        producer.send(message);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.ANALYSIS_EXCHANGE),
                eq(RabbitMqConfig.ANALYSIS_ROUTING_KEY),
                eq(message),
                any(MessagePostProcessor.class));
    }
}
