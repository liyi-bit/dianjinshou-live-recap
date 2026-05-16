package com.dianjinshou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DianjinshouApplication {

    public static void main(String[] args) {
        SpringApplication.run(DianjinshouApplication.class, args);
    }
}
