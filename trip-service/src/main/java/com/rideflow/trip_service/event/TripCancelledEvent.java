package com.rideflow.trip_service.event;

public record TripCancelledEvent(Long tripId, Long driverId) {}