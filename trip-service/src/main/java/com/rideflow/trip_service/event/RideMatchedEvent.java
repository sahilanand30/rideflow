package com.rideflow.trip_service.event;

public record RideMatchedEvent(Long tripId, Long driverId) {}