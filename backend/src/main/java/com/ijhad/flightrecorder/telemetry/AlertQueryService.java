package com.ijhad.flightrecorder.telemetry;

import com.ijhad.flightrecorder.session.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AlertQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final SessionRepository sessionRepository;

    public AlertQueryService(
            JdbcTemplate jdbcTemplate,
            SessionRepository sessionRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.sessionRepository = sessionRepository;
    }

    public List<AlertResponse> findAlerts(
            UUID sessionId,
            int limit) {

        if (!sessionRepository.existsById(sessionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Session not found");
        }

        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    event_id,
                    session_id,
                    vehicle_id,
                    alert_type,
                    message,
                    triggered_at
                FROM alerts
                WHERE session_id = ?
                ORDER BY triggered_at DESC, id DESC
                LIMIT ?
                """,
                this::mapAlert,
                sessionId,
                limit);
    }

    private AlertResponse mapAlert(
            ResultSet resultSet,
            int rowNumber) throws SQLException {

        return new AlertResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("session_id", UUID.class),
                resultSet.getString("vehicle_id"),
                resultSet.getString("alert_type"),
                resultSet.getString("message"),
                resultSet
                        .getObject("triggered_at", OffsetDateTime.class)
                        .toInstant());
    }
}