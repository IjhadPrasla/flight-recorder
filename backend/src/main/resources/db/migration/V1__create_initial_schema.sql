CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_sessions_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_sessions_time_order
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE TABLE vehicles (
    id VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL
);

CREATE TABLE telemetry_readings (
    event_id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    vehicle_id VARCHAR(100) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sequence_number BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed_mps DOUBLE PRECISION NOT NULL,
    battery_percent DOUBLE PRECISION NOT NULL,
    motor_temperature_c DOUBLE PRECISION NOT NULL,

    CONSTRAINT fk_readings_session
        FOREIGN KEY (session_id) REFERENCES sessions(id),

    CONSTRAINT fk_readings_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),

    CONSTRAINT uq_readings_sequence
        UNIQUE (session_id, vehicle_id, sequence_number),

    CONSTRAINT chk_readings_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_readings_longitude
        CHECK (longitude BETWEEN -180 AND 180),

    CONSTRAINT chk_readings_speed
        CHECK (speed_mps >= 0),

    CONSTRAINT chk_readings_battery
        CHECK (battery_percent BETWEEN 0 AND 100),

    CONSTRAINT chk_readings_sequence
        CHECK (sequence_number >= 0)
);

CREATE INDEX idx_readings_session_vehicle_time
    ON telemetry_readings (session_id, vehicle_id, recorded_at, event_id);

CREATE INDEX idx_readings_received_at
    ON telemetry_readings (received_at);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    session_id UUID NOT NULL,
    vehicle_id VARCHAR(100) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_alerts_reading
        FOREIGN KEY (event_id) REFERENCES telemetry_readings(event_id),

    CONSTRAINT fk_alerts_session
        FOREIGN KEY (session_id) REFERENCES sessions(id),

    CONSTRAINT fk_alerts_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),

    CONSTRAINT uq_alerts_event_type
        UNIQUE (event_id, alert_type)
);

CREATE INDEX idx_alerts_session_time
    ON alerts (session_id, triggered_at);