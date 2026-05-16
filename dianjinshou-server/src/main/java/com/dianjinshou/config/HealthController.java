package com.dianjinshou.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> components = new HashMap<>();

        // Check MySQL
        try (Connection conn = dataSource.getConnection()) {
            components.put("db", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            components.put("db", "DOWN");
        }

        // Check Redis
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            components.put("redis", "PONG".equals(pong) ? "UP" : "DOWN");
        } catch (Exception e) {
            components.put("redis", "DOWN");
        }

        // Check RabbitMQ
        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.execute(channel -> {
                    channel.queueDeclarePassive("dianjinshou.analysis.queue");
                    return null;
                });
                components.put("rabbitmq", "UP");
            } catch (Exception e) {
                components.put("rabbitmq", "DOWN");
            }
        } else {
            components.put("rabbitmq", "NOT_CONFIGURED");
        }

        boolean allUp = components.values().stream().allMatch("UP"::equals);
        boolean anyDown = components.values().stream().anyMatch("DOWN"::equals);
        result.put("status", anyDown ? "DEGRADED" : "UP");
        result.put("components", components);

        return result;
    }
}
