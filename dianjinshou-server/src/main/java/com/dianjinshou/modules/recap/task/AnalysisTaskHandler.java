package com.dianjinshou.modules.recap.task;

import com.dianjinshou.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class AnalysisTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskHandler.class);

    private final AnalysisTaskProcessor processor;

    public AnalysisTaskHandler(AnalysisTaskProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = RabbitMqConfig.ANALYSIS_QUEUE)
    public void handle(AnalysisTaskMessage message) {
        log.info("Received analysis task from RabbitMQ: taskId={}, type={}", message.getTaskId(), message.getType());
        processor.process(message.getTaskId());
    }
}
