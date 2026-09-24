package com.ijhad.flightrecorder.telemetry;

import com.ijhad.flightrecorder.session.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TelemetryQueryService {

    private static final String BASE_QUERY = """
            SELECT
                event_id,
                session_id,
                vehicle_id,
                recorded_at,
                received_at,
                sequence_number,
                latitude,
                longitude,
                speed_mps,
                battery_percent,
                motor_temperature_c
            FROM telemetry_readings
            WHERE session_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SessionRepository sessionRepository;

    public TelemetryQueryService(
            JdbcTemplate jdbcTemplate,
            SessionRepository sessionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.sessionRepository = sessionRepository;
    }

    public List<TelemetryReadingResponse> findReplayReadings(
            UUID sessionId,
            String vehicleId,
            int limit) {

        if (!sessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Session not found");
        }

        String normalizedVehicleId =
                vehicleId == null || vehicleId.isBlank()
                        ? null
                        : vehicleId.trim();

        List<TelemetryReadingResponse> readings;

        if (normalizedVehicleId == null) {
            String sql = BASE_QUERY + """
                    ORDER BY recorded_at, sequence_number, event_id
                    LIMIT ?
                    """;

            readings = jdbcTemplate.query(
                    sql,
                    this::mapReading,
                    sessionId,
                    limit);
        } else {
            String sql = BASE_QUERY + """
                    AND vehicle_id = ?
                    ORDER BY recorded_at, sequence_number, event_id
                    LIMIT ?
                    """;

            readings = jdbcTemplate.query(
                    sql,
                    this::mapReading,
                    sessionId,
                    normalizedVehicleId,
                    limit);
        }

        if (readings.isEmpty()) {
            return readings;
        }

        var firstTimestamp = readings.getFirst().recordedAt();

        return readings.stream()
                .map(reading -> reading.withOffsetMillis(
                        Duration.between(
                                firstTimestamp,
                                reading.recordedAt())
                                .toMillis()))
                .toList();
    }

    private TelemetryReadingResponse mapReading(
            ResultSet resultSet,
            int rowNumber) throws SQLException {

        double speedMetersPerSecond =
                resultSet.getDouble("speed_mps");

        return new TelemetryReadingResponse(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("session_id", UUID.class),
                resultSet.getString("vehicle_id"),
                resultSet
                        .getObject("recorded_at", OffsetDateTime.class)
                        .toInstant(),
                resultSet
                        .getObject("received_at", OffsetDateTime.class)
                        .toInstant(),
                resultSet.getLong("sequence_number"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                speedMetersPerSecond * 3.6,
                resultSet.getDouble("battery_percent"),
                resultSet.getDouble("motor_temperature_c"),
                0);
    }
}