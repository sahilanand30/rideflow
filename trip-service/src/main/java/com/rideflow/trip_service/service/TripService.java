package com.rideflow.trip_service.service;
import org.springframework.transaction.annotation.Transactional;
import com.rideflow.trip_service.client.UserClient;
import com.rideflow.trip_service.event.RideRequestedEvent;
import com.rideflow.trip_service.event.TripEventPublisher;
import com.rideflow.trip_service.model.Trip;
import com.rideflow.trip_service.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TripService {
    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private final TripRepository tripRepository;
    private final UserClient userClient;
    private final TripEventPublisher eventPublisher;

    public TripService(TripRepository tripRepository, UserClient userClient, TripEventPublisher eventPublisher) {
        this.tripRepository = tripRepository;
        this.userClient = userClient;
        this.eventPublisher = eventPublisher;
    }

    public Trip requestTrip(Long riderId, double pickupLat, double pickupLng, double dropLat, double dropLng) {
        UserClient.UserResponse rider = userClient.getUser(riderId);
        if (!"RIDER".equals(rider.role())) {
            throw new IllegalArgumentException("User " + riderId + " is not a rider");
        }

        Trip trip = new Trip(riderId, pickupLat, pickupLng, dropLat, dropLng);
        Trip saved = tripRepository.save(trip);
        eventPublisher.publishRideRequested(new RideRequestedEvent(
                saved.getId(), saved.getRiderId(),
                saved.getPickupLat(), saved.getPickupLng(),
                saved.getDropLat(), saved.getDropLng()));
        return saved;
    }

    public Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
    }

    public Trip cancelTrip(Long id) {
        Trip trip = getTrip(id);
        if (trip.getStatus() == Trip.Status.COMPLETED || trip.getStatus() == Trip.Status.CANCELLED) {
            throw new IllegalStateException("Trip cannot be cancelled in state: " + trip.getStatus());
        }
        trip.setStatus(Trip.Status.CANCELLED);
        return tripRepository.save(trip);
    }

    @Transactional
    public void markMatched(Long tripId, Long driverId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            log.warn("Ignoring match for unknown trip {}", tripId);
            return;
        }
        if (trip.getStatus() != Trip.Status.REQUESTED) {
            log.info("Ignoring match for trip {} because it is already {}", tripId, trip.getStatus());
            return;
        }
        trip.setDriverId(driverId);
        trip.setStatus(Trip.Status.MATCHED);
        tripRepository.save(trip);
        log.info("Trip {} matched with driver {}", tripId, driverId);
    }
}