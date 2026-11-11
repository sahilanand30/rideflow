package com.rideflow.matching_service.event;

import com.rideflow.matching_service.client.LocationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RideRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(RideRequestedListener.class);
    private static final String RIDE_MATCHED_TOPIC = "ride.matched";
    private static final String RIDE_MATCH_FAILED_TOPIC = "ride.match_failed";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LocationClient locationClient;
    private final double searchRadiusKm;

    public RideRequestedListener(KafkaTemplate<String, Object> kafkaTemplate,
                                 LocationClient locationClient,
                                 @Value("${matching.search-radius-km}") double searchRadiusKm) {
        this.kafkaTemplate = kafkaTemplate;
        this.locationClient = locationClient;
        this.searchRadiusKm = searchRadiusKm;
    }

    @KafkaListener(topics = "ride.requested")
    public void onRideRequested(RideRequestedEvent event) {
        log.info("Received ride request for trip {}", event.tripId());

        List<LocationClient.NearbyDriver> candidates =
                locationClient.findNearby(event.pickupLat(), event.pickupLng(), searchRadiusKm);

        if (candidates.isEmpty()) {
            log.warn("No drivers within {} km of trip {}", searchRadiusKm, event.tripId());
            publishFailure(event.tripId(), "NO_DRIVERS_NEARBY");
            return;
        }

        for (LocationClient.NearbyDriver candidate : candidates) {
            if (locationClient.reserve(candidate.driverId(), event.tripId())) {
                kafkaTemplate.send(RIDE_MATCHED_TOPIC, String.valueOf(event.tripId()),
                        new RideMatchedEvent(event.tripId(), candidate.driverId()));
                log.info("Matched trip {} with driver {} ({} km away)",
                        event.tripId(), candidate.driverId(), candidate.distanceKm());
                return;
            }
            log.info("Driver {} already reserved, trying next", candidate.driverId());
        }

        log.warn("All nearby drivers are busy for trip {}", event.tripId());
        publishFailure(event.tripId(), "ALL_DRIVERS_BUSY");
    }

    private void publishFailure(Long tripId, String reason) {
        kafkaTemplate.send(RIDE_MATCH_FAILED_TOPIC, String.valueOf(tripId),
                new RideMatchFailedEvent(tripId, reason));
    }
}