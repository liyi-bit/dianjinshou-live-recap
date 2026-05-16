package com.dianjinshou.modules.recap.task;

import com.dianjinshou.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AnalysisTaskProducer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final AnalysisTaskProcessor processor;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);

    @Autowired
    public AnalysisTaskProducer(@Autowired(required = false) RabbitTemplate rabbitTemplate,
                                AnalysisTaskProcessor processor) {
        this.rabbitTemplate = rabbitTemplate;
        this.processor = processor;
    }

    public void send(AnalysisTaskMessage message) {
        if (rabbitTemplate != null) {
            try {
                MessagePostProcessor postProcessor = msg -> {
                    msg.getMessageProperties().setPriority(
                            message.getPriority() != null ? message.getPriority() : 5);
                    return msg;
                };

                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.ANALYSIS_EXCHANGE,
                        RabbitMqConfig.ANALYSIS_ROUTING_KEY,
                        message,
                        postProcessor);
                log.info("Analysis task sent to RabbitMQ: taskId={}", message.getTaskId());
                return;
            } catch (Exception e) {
                log.warn("RabbitMQ unavailable, falling back to local processing: taskId={}, error={}",
                        message.getTaskId(), e.getMessage());
            }
        }
        // Fallback: process locally in background thread.
        // Callers wrap this in TransactionSynchronization.afterCommit() so the inserted
        // task is already visible — submit directly without re-wrapping.
        log.info("Processing task in background executor: taskId={}", message.getTaskId());
        asyncExecutor.submit(() -> {
            try {
                processor.process(message.getTaskId());
            } catch (Exception e) {
                log.error("Background processor failed: taskId={}", message.getTaskId(), e);
            }
        });
    }
}
