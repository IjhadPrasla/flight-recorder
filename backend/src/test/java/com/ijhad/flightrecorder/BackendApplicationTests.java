package com.ijhad.flightrecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.ijhad.flightrecorder.session.SessionRepository;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void clearSessions() {
        sessionRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createsAndListsSession() throws Exception {
        mockMvc.perform(
                post("/api/sessions")
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {
                          "name": "Integration Test Flight"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(
                jsonPath("$.name")
                    .value("Integration Test Flight")
            )
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.endedAt").isEmpty());

        assertEquals(1, sessionRepository.count());

        mockMvc.perform(get("/api/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(
                jsonPath("$[0].name")
                    .value("Integration Test Flight")
            )
            .andExpect(
                jsonPath("$[0].status")
                    .value("RUNNING")
            );
    }

    @Test
    void rejectsBlankSessionName() throws Exception {
        mockMvc.perform(
                post("/api/sessions")
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {
                          "name": "   "
                        }
                        """)
            )
            .andExpect(status().isBadRequest());

        assertEquals(0, sessionRepository.count());
    }
}