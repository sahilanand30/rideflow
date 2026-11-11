package com.rideflow.payment_service.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(Long tripId, Long riderId, Long driverId, BigDecimal amount) {}