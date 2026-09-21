package com.ijhad.flightrecorder.session;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public SessionResponse create(CreateSessionRequest request) {
        TelemetrySession session =
            new TelemetrySession(request.name());

        TelemetrySession savedSession =
            sessionRepository.save(session);

        return SessionResponse.from(savedSession);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> findAll() {
        Sort newestFirst =
            Sort.by(Sort.Direction.DESC, "startedAt");

        return sessionRepository.findAll(newestFirst)
            .stream()
            .map(SessionResponse::from)
            .toList();
    }
}