package com.ijhad.flightrecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM telemetry_readings");
        jdbcTemplate.update("DELETE FROM vehicles");
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
                                        """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.name")
                                .value("Integration Test Flight"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.endedAt").isEmpty());

        assertEquals(1, sessionRepository.count());

        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Integration Test Flight"))
                .andExpect(
                        jsonPath("$[0].status")
                                .value("RUNNING"));
    }

    @Test
    void rejectsBlankSessionName() throws Exception {
        mockMvc.perform(
                        post("/api/sessions")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": " "
                                        }
                                        """))
                .andExpect(status().isBadRequest());

        assertEquals(0, sessionRepository.count());
    }

    @Test
    void replaysStoredTelemetryChronologicallyAndFiltersByVehicle()
            throws Exception {

        mockMvc.perform(
                        post("/api/sessions")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Replay Integration Test"
                                        }
                                        """))
                .andExpect(status().isCreated());

        UUID sessionId = sessionRepository
                .findAll()
                .getFirst()
                .getId();

        jdbcTemplate.update(
                """
                INSERT INTO vehicles (id, display_name)
                VALUES (?, ?), (?, ?)
                """,
                "vehicle-001",
                "Vehicle 001",
                "vehicle-002",
                "Vehicle 002");

        OffsetDateTime start =
                OffsetDateTime.now(ZoneOffset.UTC);

        insertReading(
                UUID.randomUUID(),
                sessionId,
                "vehicle-001",
                start.plusNanos(250_000_000),
                1,
                15.0);

        insertReading(
                UUID.randomUUID(),
                sessionId,
                "vehicle-001",
                start,
                0,
                15.0);

        insertReading(
                UUID.randomUUID(),
                sessionId,
                "vehicle-002",
                start.plusNanos(100_000_000),
                0,
                20.0);

        mockMvc.perform(
                        get("/api/sessions/{sessionId}/telemetry/replay",
                                sessionId)
                                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].vehicleId")
                        .value("vehicle-001"))
                .andExpect(jsonPath("$[0].sequenceNumber")
                        .value(0))
                .andExpect(jsonPath("$[0].offsetMillis")
                        .value(0));

        mockMvc.perform(
                        get("/api/sessions/{sessionId}/telemetry/replay",
                                sessionId)
                                .param("vehicleId", "vehicle-001")
                                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sequenceNumber")
                        .value(0))
                .andExpect(jsonPath("$[0].speedKph")
                        .value(54.0))
                .andExpect(jsonPath("$[0].offsetMillis")
                        .value(0))
                .andExpect(jsonPath("$[1].sequenceNumber")
                        .value(1))
                .andExpect(jsonPath("$[1].offsetMillis")
                        .value(250));
    }

    private void insertReading(
            UUID eventId,
            UUID sessionId,
            String vehicleId,
            OffsetDateTime recordedAt,
            long sequenceNumber,
            double speedMetersPerSecond) {

        jdbcTemplate.update(
                """
                INSERT INTO telemetry_readings (
                    event_id,
                    session_id,
                    vehicle_id,
                    recorded_at,
                    sequence_number,
                    latitude,
                    longitude,
                    speed_mps,
                    battery_percent,
                    motor_temperature_c
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventId,
                sessionId,
                vehicleId,
                recordedAt,
                sequenceNumber,
                30.2672,
                -97.7431,
                speedMetersPerSecond,
                90.0,
                60.0);
    }
}