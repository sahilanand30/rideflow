package com.rideflow.payment_service.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareCalculator {

    private static final double BASE_FARE = 50.0;
    private static final double RATE_PER_KM = 12.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    public BigDecimal calculate(double pickupLat, double pickupLng, double dropLat, double dropLng) {
        double km = distanceKm(pickupLat, pickupLng, dropLat, dropLng);
        return BigDecimal.valueOf(BASE_FARE + RATE_PER_KM * km).setScale(2, RoundingMode.HALF_UP);
    }

    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}