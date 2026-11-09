package com.rideflow.trip_service.event;

import com.rideflow.trip_service.service.TripService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RideMatchedListener {

    private final TripService tripService;

    public RideMatchedListener(TripService tripService) {
        this.tripService = tripService;
    }

    @KafkaListener(topics = "ride.matched")
    public void onRideMatched(RideMatchedEvent event) {
        tripService.markMatched(event.tripId(), event.driverId());
    }
}