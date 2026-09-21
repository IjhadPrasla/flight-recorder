package com.ijhad.flightrecorder.session;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class TelemetrySession {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    protected TelemetrySession() {
        // Required by JPA.
    }

    public TelemetrySession(String name) {
        this.id = UUID.randomUUID();
        this.name = validateName(name);
        this.startedAt = Instant.now();
        this.status = SessionStatus.RUNNING;
    }

    public void complete() {
        ensureRunning();
        this.status = SessionStatus.COMPLETED;
        this.endedAt = Instant.now();
    }

    public void fail() {
        ensureRunning();
        this.status = SessionStatus.FAILED;
        this.endedAt = Instant.now();
    }

    private void ensureRunning() {
        if (status != SessionStatus.RUNNING) {
            throw new IllegalStateException(
                "Only a running session can be completed or failed"
            );
        }
    }

    private static String validateName(String name) {
        String normalized = Objects.requireNonNull(
            name,
            "Session name is required"
        ).trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                "Session name cannot be blank"
            );
        }

        if (normalized.length() > 120) {
            throw new IllegalArgumentException(
                "Session name cannot exceed 120 characters"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public SessionStatus getStatus() {
        return status;
    }
}