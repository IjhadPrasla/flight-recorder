package com.ijhad.flightrecorder.session;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    String name,
    Instant startedAt,
    Instant endedAt,
    SessionStatus status
) {

    public static SessionResponse from(TelemetrySession session) {
        return new SessionResponse(
            session.getId(),
            session.getName(),
            session.getStartedAt(),
            session.getEndedAt(),
            session.getStatus()
        );
    }
}