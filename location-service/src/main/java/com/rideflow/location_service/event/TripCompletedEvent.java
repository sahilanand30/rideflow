package com.rideflow.location_service.event;

public record TripCompletedEvent(Long tripId, Long riderId, Long driverId,
                                 double pickupLat, double pickupLng,
                                 double dropLat, double dropLng) {}