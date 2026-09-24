package com.ijhad.flightrecorder.telemetry;

import java.time.Instant;
import java.util.UUID;

public record TelemetryReadingResponse(
        UUID eventId,
        UUID sessionId,
        String vehicleId,
        Instant recordedAt,
        Instant receivedAt,
        long sequenceNumber,
        double latitude,
        double longitude,
        double speedKph,
        double batteryPercent,
        double motorTemperatureCelsius,
        long offsetMillis) {

    TelemetryReadingResponse withOffsetMillis(long newOffsetMillis) {
        return new TelemetryReadingResponse(
                eventId,
                sessionId,
                vehicleId,
                recordedAt,
                receivedAt,
                sequenceNumber,
                latitude,
                longitude,
                speedKph,
                batteryPercent,
                motorTemperatureCelsius,
                newOffsetMillis);
    }
}