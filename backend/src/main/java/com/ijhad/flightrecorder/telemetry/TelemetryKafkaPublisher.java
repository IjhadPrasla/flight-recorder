package com.ijhad.flightrecorder.telemetry;

import com.ijhad.flightrecorder.telemetry.proto.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TelemetryKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryKafkaPublisher.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final String telemetryTopic;

    public TelemetryKafkaPublisher(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            @Value("${app.kafka.topics.telemetry}") String telemetryTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.telemetryTopic = telemetryTopic;
    }

    public void publish(TelemetryReading reading) {
        byte[] payload = reading.toByteArray();

        kafkaTemplate
                .send(telemetryTopic, reading.getVehicleId(), payload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to publish telemetry reading {}",
                                reading.getReadingId(),
                                exception);
                        return;
                    }

                    log.debug(
                            "Published reading {} to partition {} at offset {}",
                            reading.getReadingId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}