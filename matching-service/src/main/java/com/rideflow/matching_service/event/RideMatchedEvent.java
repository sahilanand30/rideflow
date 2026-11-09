package com.rideflow.matching_service.event;

public record RideMatchedEvent(Long tripId, Long driverId) {}