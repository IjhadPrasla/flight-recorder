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
@RequestMapping("/api/sessions/{sessionId}/alerts")
@Validated
public class AlertController {

    private final AlertQueryService queryService;

    public AlertController(AlertQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public List<AlertResponse> list(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "100")
            @Min(1)
            @Max(1000)
            int limit) {

        return queryService.findAlerts(sessionId, limit);
    }
}