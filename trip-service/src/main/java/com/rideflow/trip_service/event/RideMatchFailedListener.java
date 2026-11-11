package com.rideflow.trip_service.event;

import com.rideflow.trip_service.service.TripService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RideMatchFailedListener {

    private final TripService tripService;

    public RideMatchFailedListener(TripService tripService) {
        this.tripService = tripService;
    }

    @KafkaListener(
            topics = "ride.match_failed",
            properties = "spring.json.value.default.type=com.rideflow.trip_service.event.RideMatchFailedEvent")
    public void onRideMatchFailed(RideMatchFailedEvent event) {
        tripService.markMatchFailed(event.tripId(), event.reason());
    }
}