package com.dianjinshou.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("T01: HealthController 健康检查")
class HealthControllerTest {

    @InjectMocks
    private HealthController controller;

    @Mock
    private DataSource dataSource;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Connection dbConnection;

    @BeforeEach
    void setUp() throws Exception {
        // 默认 mock: DB UP
        when(dataSource.getConnection()).thenReturn(dbConnection);
        when(dbConnection.isValid(2)).thenReturn(true);

        // 默认 mock: Redis UP
        when(redisTemplate.getConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
    }

    @Test
    @DisplayName("TC-01-01-A: 所有组件 UP -> status=UP")
    void allComponentsUp() throws Exception {
        when(rabbitTemplate.execute(any())).thenReturn(null);

        Map<String, Object> result = controller.health();

        assertEquals("UP", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("UP", components.get("db"));
        assertEquals("UP", components.get("redis"));
        assertEquals("UP", components.get("rabbitmq"));
    }

    @Test
    @DisplayName("TC-01-01-B: 数据库 DOWN -> status=DEGRADED")
    void dbDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB fail"));
        when(rabbitTemplate.execute(any())).thenReturn(null);

        Map<String, Object> result = controller.health();

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("DOWN", components.get("db"));
    }

    @Test
    @DisplayName("TC-01-01-C: Redis DOWN -> status=DEGRADED")
    void redisDown() throws Exception {
        when(redisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("Redis fail"));
        when(rabbitTemplate.execute(any())).thenReturn(null);

        Map<String, Object> result = controller.health();

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("DOWN", components.get("redis"));
    }

    @Test
    @DisplayName("TC-01-01-D: RabbitMQ 未配置 -> NOT_CONFIGURED，不影响整体")
    void rabbitNotConfigured() throws Exception {
        Field f = HealthController.class.getDeclaredField("rabbitTemplate");
        f.setAccessible(true);
        f.set(controller, null);

        Map<String, Object> result = controller.health();

        assertEquals("UP", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("NOT_CONFIGURED", components.get("rabbitmq"));
    }

    @Test
    @DisplayName("TC-01-01-E: RabbitMQ 检查抛异常 -> DOWN, DEGRADED")
    void rabbitDown() throws Exception {
        when(rabbitTemplate.execute(any())).thenThrow(new RuntimeException("RabbitMQ fail"));

        Map<String, Object> result = controller.health();

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("DOWN", components.get("rabbitmq"));
    }

    @Test
    @DisplayName("TC-01-01-F: 所有组件 DOWN -> status=DEGRADED")
    void allDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB fail"));
        when(redisTemplate.getConnectionFactory()).thenThrow(new RuntimeException("Redis fail"));
        when(rabbitTemplate.execute(any())).thenThrow(new RuntimeException("RabbitMQ fail"));

        Map<String, Object> result = controller.health();

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("DOWN", components.get("db"));
        assertEquals("DOWN", components.get("redis"));
        assertEquals("DOWN", components.get("rabbitmq"));
    }

    @Test
    @DisplayName("TC-01-01-G: DB 连接有效但 isValid 返回 false -> DOWN")
    void dbInvalidConnection() throws Exception {
        when(dbConnection.isValid(2)).thenReturn(false);
        when(rabbitTemplate.execute(any())).thenReturn(null);

        Map<String, Object> result = controller.health();

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("DOWN", components.get("db"));
    }

    @Test
    @DisplayName("TC-01-01-H: Redis ping 返回非 PONG -> DOWN")
    void redisBadPing() throws Exception {
        when(redisConnection.ping()).thenReturn("ERROR");
        when(rabbitTemplate.execute(any())).thenReturn(null);

        Map<String, Object> result = controller.health();

        assertEquals("DEGRADED", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) result.get("components");
        assertEquals("DOWN", components.get("redis"));
    }

    @Test
    @DisplayName("TC-01-02-A: DianjinshouApplication 标注 @SpringBootApplication")
    void applicationAnnotation() {
        assertNotNull(
            com.dianjinshou.DianjinshouApplication.class
                .getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class),
            "应标注 @SpringBootApplication"
        );
    }
}
