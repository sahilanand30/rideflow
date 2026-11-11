package com.rideflow.trip_service.event;

public record RideMatchFailedEvent(Long tripId, String reason) {}