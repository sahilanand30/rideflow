package com.rideflow.location_service.event;

import com.rideflow.location_service.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TripCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(TripCancelledListener.class);

    private final LocationService locationService;

    public TripCancelledListener(LocationService locationService) {
        this.locationService = locationService;
    }

    @KafkaListener(
            topics = "trip.cancelled",
            properties = "spring.json.value.default.type=com.rideflow.location_service.event.TripCancelledEvent")
    public void onTripCancelled(TripCancelledEvent event) {
        if (event.driverId() == null) {
            return;
        }
        locationService.releaseDriver(event.driverId(), event.tripId());
        log.info("Released driver {} after trip {} was cancelled", event.driverId(), event.tripId());
    }
}