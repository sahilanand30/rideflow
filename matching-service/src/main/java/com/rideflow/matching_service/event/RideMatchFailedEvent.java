package com.rideflow.matching_service.event;

public record RideMatchFailedEvent(Long tripId, String reason) {}