package com.ijhad.flightrecorder.session;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository
    extends JpaRepository<TelemetrySession, UUID> {
}