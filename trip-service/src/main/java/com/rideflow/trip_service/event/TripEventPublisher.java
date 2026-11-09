package com.rideflow.trip_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TripEventPublisher {

    public static final String RIDE_REQUESTED_TOPIC = "ride.requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TripEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRideRequested(RideRequestedEvent event) {
        kafkaTemplate.send(RIDE_REQUESTED_TOPIC, String.valueOf(event.tripId()), event);
    }
}