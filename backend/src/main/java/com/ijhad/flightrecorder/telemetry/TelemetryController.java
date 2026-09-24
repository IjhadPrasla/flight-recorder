package com.ijhad.flightrecorder.telemetry;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/telemetry")
@Validated
public class TelemetryController {

    private final TelemetryQueryService queryService;

    public TelemetryController(TelemetryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/replay")
    public List<TelemetryReadingResponse> replay(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String vehicleId,
            @RequestParam(defaultValue = "1000")
            @Min(1)
            @Max(10000)
            int limit) {

        return queryService.findReplayReadings(
                sessionId,
                vehicleId,
                limit);
    }
}