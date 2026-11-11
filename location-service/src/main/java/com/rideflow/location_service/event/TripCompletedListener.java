package com.rideflow.location_service.event;

import com.rideflow.location_service.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TripCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(TripCompletedListener.class);

    private final LocationService locationService;

    public TripCompletedListener(LocationService locationService) {
        this.locationService = locationService;
    }

    @KafkaListener(topics = "trip.completed")
    public void onTripCompleted(TripCompletedEvent event) {
        if (event.driverId() == null) {
            return;
        }
        locationService.releaseDriver(event.driverId(), event.tripId());
        log.info("Released driver {} after trip {} completed", event.driverId(), event.tripId());
    }
}