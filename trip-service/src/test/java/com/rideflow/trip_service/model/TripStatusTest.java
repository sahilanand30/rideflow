package com.rideflow.trip_service.model;

import org.junit.jupiter.api.Test;

import static com.rideflow.trip_service.model.Trip.Status.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripStatusTest {

    @Test
    void happyPathIsAllowedStepByStep() {
        assertTrue(REQUESTED.canTransitionTo(MATCHED));
        assertTrue(MATCHED.canTransitionTo(DRIVER_ARRIVING));
        assertTrue(DRIVER_ARRIVING.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(COMPLETED));
    }

    @Test
    void requestedTripCanFailToFindADriver() {
        assertTrue(REQUESTED.canTransitionTo(NO_DRIVERS_AVAILABLE));
    }

    @Test
    void tripCanBeCancelledOnlyBeforeItStarts() {
        assertTrue(REQUESTED.canTransitionTo(CANCELLED));
        assertTrue(MATCHED.canTransitionTo(CANCELLED));
        assertTrue(DRIVER_ARRIVING.canTransitionTo(CANCELLED));
        assertFalse(IN_PROGRESS.canTransitionTo(CANCELLED));
    }

    @Test
    void stepsCannotBeSkipped() {
        assertFalse(REQUESTED.canTransitionTo(IN_PROGRESS));
        assertFalse(REQUESTED.canTransitionTo(COMPLETED));
        assertFalse(MATCHED.canTransitionTo(COMPLETED));
    }

    @Test
    void tripsCannotMoveBackwards() {
        assertFalse(MATCHED.canTransitionTo(REQUESTED));
        assertFalse(IN_PROGRESS.canTransitionTo(MATCHED));
    }

    @Test
    void finalStatesCannotMoveAnywhere() {
        Trip.Status[] finalStates = {COMPLETED, CANCELLED, NO_DRIVERS_AVAILABLE};
        for (Trip.Status from : finalStates) {
            for (Trip.Status to : Trip.Status.values()) {
                assertFalse(from.canTransitionTo(to), from + " should not move to " + to);
            }
        }
    }
}