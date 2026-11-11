package com.rideflow.notification_service.event;

public record RideMatchedEvent(Long tripId, Long driverId) {}