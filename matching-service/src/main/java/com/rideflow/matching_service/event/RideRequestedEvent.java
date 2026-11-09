package com.rideflow.matching_service.event;

public record RideRequestedEvent(Long tripId, Long riderId,
                                 double pickupLat, double pickupLng,
                                 double dropLat, double dropLng) {}