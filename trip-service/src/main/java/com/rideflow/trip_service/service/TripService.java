package com.rideflow.trip_service.service;

import com.rideflow.trip_service.client.UserClient;
import com.rideflow.trip_service.model.Trip;
import com.rideflow.trip_service.repository.TripRepository;
import org.springframework.stereotype.Service;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserClient userClient;

    public TripService(TripRepository tripRepository, UserClient userClient) {
        this.tripRepository = tripRepository;
        this.userClient = userClient;
    }

    public Trip requestTrip(Long riderId, double pickupLat, double pickupLng, double dropLat, double dropLng) {
        UserClient.UserResponse rider = userClient.getUser(riderId);
        if (!"RIDER".equals(rider.role())) {
            throw new IllegalArgumentException("User " + riderId + " is not a rider");
        }

        Trip trip = new Trip(riderId, pickupLat, pickupLng, dropLat, dropLng);
        return tripRepository.save(trip);
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
}