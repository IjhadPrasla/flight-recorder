package com.ijhad.flightrecorder.session;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(
        @Valid @RequestBody CreateSessionRequest request
    ) {
        SessionResponse response =
            sessionService.create(request);

        URI location =
            URI.create("/api/sessions/" + response.id());

        return ResponseEntity
            .created(location)
            .body(response);
    }

    @GetMapping
    public List<SessionResponse> findAll() {
        return sessionService.findAll();
    }
}