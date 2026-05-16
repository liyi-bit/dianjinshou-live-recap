package com.dianjinshou;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DianjinshouApplicationTest {

    @Test
    void mainClass_exists() {
        // Smoke test: main class exists and is annotated
        assertNotNull(DianjinshouApplication.class.getAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }
}
