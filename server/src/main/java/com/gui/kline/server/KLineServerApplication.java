package com.gui.kline.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for K-Line Backend Server.
 * This Spring Boot application provides REST API endpoints for the K-Line Tyre House management system.
 * 
 * Features:
 * - RESTful API endpoints for all business entities
 * - JWT-based authentication and authorization
 * - Data synchronization between client and server
 * - Caching for improved performance
 * - Scheduled tasks for maintenance
 * - Async processing for better responsiveness
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class KLineServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KLineServerApplication.class, args);
    }
}