package com.ijhad.flightrecorder.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TelemetrySessionTest {

    @Test
    void createsRunningSessionWithNormalizedName() {
        TelemetrySession session =
            new TelemetrySession("  Test Flight  ");

        assertNotNull(session.getId());
        assertEquals("Test Flight", session.getName());
        assertEquals(
            SessionStatus.RUNNING,
            session.getStatus()
        );
        assertNotNull(session.getStartedAt());
        assertNull(session.getEndedAt());
    }

    @Test
    void completesRunningSession() {
        TelemetrySession session =
            new TelemetrySession("Test Flight");

        session.complete();

        assertEquals(
            SessionStatus.COMPLETED,
            session.getStatus()
        );
        assertNotNull(session.getEndedAt());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new TelemetrySession("   ")
        );
    }

    @Test
    void cannotCompleteSessionTwice() {
        TelemetrySession session =
            new TelemetrySession("Test Flight");

        session.complete();

        assertThrows(
            IllegalStateException.class,
            session::complete
        );
    }
}