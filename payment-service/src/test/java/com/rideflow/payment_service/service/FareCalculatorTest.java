package com.rideflow.payment_service.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator();

    @Test
    void zeroDistanceCostsOnlyTheBaseFare() {
        BigDecimal fare = calculator.calculate(12.9716, 77.5946, 12.9716, 77.5946);

        assertEquals(new BigDecimal("50.00"), fare);
    }

    @Test
    void oneDegreeOfLatitudeIsAboutOneHundredElevenKm() {
        // About 111.19 km, so roughly 50 + 12 * 111.19 = 1384.34
        BigDecimal fare = calculator.calculate(0.0, 77.0, 1.0, 77.0);

        assertTrue(fare.compareTo(new BigDecimal("1384.00")) > 0);
        assertTrue(fare.compareTo(new BigDecimal("1385.00")) < 0);
    }

    @Test
    void sampleBangaloreTripCostsAboutOneHundredTwelve() {
        // The trip used in our manual tests, roughly 5.2 km in a straight line
        BigDecimal fare = calculator.calculate(12.9716, 77.5946, 12.9352, 77.6245);

        assertTrue(fare.compareTo(new BigDecimal("105.00")) > 0);
        assertTrue(fare.compareTo(new BigDecimal("120.00")) < 0);
    }

    @Test
    void fareAlwaysHasTwoDecimalPlaces() {
        BigDecimal fare = calculator.calculate(12.9716, 77.5946, 12.9352, 77.6245);

        assertEquals(2, fare.scale());
    }

    @Test
    void longerTripCostsMoreThanShorterTrip() {
        BigDecimal shortTrip = calculator.calculate(12.9716, 77.5946, 12.9800, 77.6000);
        BigDecimal longTrip = calculator.calculate(12.9716, 77.5946, 13.1000, 77.7000);

        assertTrue(longTrip.compareTo(shortTrip) > 0);
    }

    @Test
    void fareIsTheSameInBothDirections() {
        BigDecimal forward = calculator.calculate(12.9716, 77.5946, 12.9352, 77.6245);
        BigDecimal backward = calculator.calculate(12.9352, 77.6245, 12.9716, 77.5946);

        assertEquals(forward, backward);
    }
}