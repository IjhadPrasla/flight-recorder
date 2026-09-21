package com.ijhad.flightrecorder;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@Testcontainers
@SpringBootTest
class BackendApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void contextLoads() {
    }
}