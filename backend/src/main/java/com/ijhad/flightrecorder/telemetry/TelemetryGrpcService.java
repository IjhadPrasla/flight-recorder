package com.ijhad.flightrecorder.telemetry;

import com.ijhad.flightrecorder.telemetry.proto.IngestionSummary;
import com.ijhad.flightrecorder.telemetry.proto.TelemetryIngestionServiceGrpc;
import com.ijhad.flightrecorder.telemetry.proto.TelemetryReading;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TelemetryGrpcService
        extends TelemetryIngestionServiceGrpc.TelemetryIngestionServiceImplBase {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryGrpcService.class);

    private final TelemetryKafkaPublisher publisher;

    public TelemetryGrpcService(TelemetryKafkaPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public StreamObserver<TelemetryReading> streamReadings(
            StreamObserver<IngestionSummary> responseObserver) {

        return new StreamObserver<>() {

            private long acceptedCount;
            private long rejectedCount;

            @Override
            public void onNext(TelemetryReading reading) {
                if (!isValid(reading)) {
                    rejectedCount++;
                    log.warn(
                            "Rejected invalid telemetry reading {}",
                            reading.getReadingId());
                    return;
                }

                try {
                    publisher.publish(reading);
                    acceptedCount++;
                } catch (RuntimeException exception) {
                    rejectedCount++;
                    log.error(
                            "Could not enqueue telemetry reading {}",
                            reading.getReadingId(),
                            exception);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn(
                        "Telemetry stream ended unexpectedly after accepting {} readings",
                        acceptedCount,
                        throwable);
            }

            @Override
            public void onCompleted() {
                IngestionSummary summary = IngestionSummary.newBuilder()
                        .setAcceptedCount(acceptedCount)
                        .setRejectedCount(rejectedCount)
                        .build();

                responseObserver.onNext(summary);
                responseObserver.onCompleted();

                log.info(
                        "Telemetry stream completed: {} accepted, {} rejected",
                        acceptedCount,
                        rejectedCount);
            }
        };
    }

    private boolean isValid(TelemetryReading reading) {
        try {
            UUID.fromString(reading.getReadingId());
            UUID.fromString(reading.getSessionId());
            UUID.fromString(reading.getVehicleId());
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return reading.hasRecordedAt()
                && reading.getLatitude() >= -90
                && reading.getLatitude() <= 90
                && reading.getLongitude() >= -180
                && reading.getLongitude() <= 180
                && reading.getSpeedKph() >= 0
                && reading.getBatteryPercent() >= 0
                && reading.getBatteryPercent() <= 100
                && reading.getMotorTemperatureCelsius() >= -100
                && reading.getMotorTemperatureCelsius() <= 300;
    }
}