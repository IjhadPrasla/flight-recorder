package com.ijhad.flightrecorder.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
    @NotBlank(message = "Session name is required")
    @Size(
        max = 120,
        message = "Session name cannot exceed 120 characters"
    )
    String name
) {
}