package com.rideflow.matching_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RideRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(RideRequestedListener.class);
    private static final String RIDE_MATCHED_TOPIC = "ride.matched";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Long tempDriverId;

    public RideRequestedListener(KafkaTemplate<String, Object> kafkaTemplate, @Value("${matching.temp-driver-id}") Long tempDriverId) {
        this.kafkaTemplate = kafkaTemplate;
        this.tempDriverId = tempDriverId;
    }

    @KafkaListener(topics = "ride.requested")
    public void onRideRequested(RideRequestedEvent event) {
        log.info("Received ride request for trip {}", event.tripId());

        RideMatchedEvent matched = new RideMatchedEvent(event.tripId(), tempDriverId);
        kafkaTemplate.send(RIDE_MATCHED_TOPIC, String.valueOf(event.tripId()), matched);

        log.info("Matched trip {} with driver {}", event.tripId(), tempDriverId);
    }
}