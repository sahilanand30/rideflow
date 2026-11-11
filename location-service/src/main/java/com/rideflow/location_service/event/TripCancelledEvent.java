package com.rideflow.location_service.event;

public record TripCancelledEvent(Long tripId, Long driverId) {}