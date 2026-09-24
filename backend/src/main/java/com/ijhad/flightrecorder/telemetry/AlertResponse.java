package com.ijhad.flightrecorder.telemetry;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID eventId,
        UUID sessionId,
        String vehicleId,
        String alertType,
        String message,
        Instant triggeredAt) {
}