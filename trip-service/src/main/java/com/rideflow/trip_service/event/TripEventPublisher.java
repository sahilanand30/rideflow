package com.rideflow.trip_service.event;

import com.rideflow.trip_service.outbox.OutboxEvent;
import com.rideflow.trip_service.outbox.OutboxRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TripEventPublisher {

    public static final String RIDE_REQUESTED_TOPIC = "ride.requested";
    public static final String TRIP_COMPLETED_TOPIC = "trip.completed";
    public static final String TRIP_CANCELLED_TOPIC = "trip.cancelled";

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public TripEventPublisher(OutboxRepository outboxRepository, JsonMapper jsonMapper) {
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    public void publishRideRequested(RideRequestedEvent event) {
        store(RIDE_REQUESTED_TOPIC, event.tripId(), event);
    }

    public void publishTripCompleted(TripCompletedEvent event) {
        store(TRIP_COMPLETED_TOPIC, event.tripId(), event);
    }

    public void publishTripCancelled(TripCancelledEvent event) {
        store(TRIP_CANCELLED_TOPIC, event.tripId(), event);
    }

    private void store(String topic, Long tripId, Object event) {
        String payload = jsonMapper.writeValueAsString(event);
        outboxRepository.save(new OutboxEvent(topic, String.valueOf(tripId), payload));
    }
}