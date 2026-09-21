package com.ijhad.flightrecorder.telemetry;

import com.google.protobuf.InvalidProtocolBufferException;
import com.ijhad.flightrecorder.session.SessionRepository;
import com.ijhad.flightrecorder.telemetry.proto.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class TelemetryKafkaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryKafkaConsumer.class);

    private final JdbcTemplate jdbcTemplate;
    private final SessionRepository sessionRepository;

    public TelemetryKafkaConsumer(
            JdbcTemplate jdbcTemplate,
            SessionRepository sessionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.sessionRepository = sessionRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.telemetry}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(byte[] payload) {
        TelemetryReading reading;

        try {
            reading = TelemetryReading.parseFrom(payload);
        } catch (InvalidProtocolBufferException exception) {
            log.error("Discarding malformed telemetry message", exception);
            return;
        }

        UUID eventId;
        UUID sessionId;

        try {
            eventId = UUID.fromString(reading.getReadingId());
            sessionId = UUID.fromString(reading.getSessionId());
        } catch (IllegalArgumentException exception) {
            log.warn("Discarding telemetry message with invalid identifiers");
            return;
        }

        if (!sessionRepository.existsById(sessionId)) {
            log.warn(
                    "Discarding reading {} because session {} does not exist",
                    eventId,
                    sessionId);
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO vehicles (id, display_name)
                VALUES (?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                reading.getVehicleId(),
                reading.getVehicleId());

        Instant instant = Instant.ofEpochSecond(
                reading.getRecordedAt().getSeconds(),
                reading.getRecordedAt().getNanos());

        OffsetDateTime recordedAt =
                OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);

        double speedMetersPerSecond = reading.getSpeedKph() / 3.6;

        int inserted = jdbcTemplate.update(
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
                ON CONFLICT DO NOTHING
                """,
                eventId,
                sessionId,
                reading.getVehicleId(),
                recordedAt,
                reading.getSequenceNumber(),
                reading.getLatitude(),
                reading.getLongitude(),
                speedMetersPerSecond,
                reading.getBatteryPercent(),
                reading.getMotorTemperatureCelsius());

        if (inserted == 1) {
            log.info(
                    "Persisted telemetry reading {} for vehicle {}",
                    eventId,
                    reading.getVehicleId());
        } else {
            log.debug("Ignored duplicate telemetry reading {}", eventId);
        }
    }
}