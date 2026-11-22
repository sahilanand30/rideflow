package com.rideflow.trip_service.service;

import com.rideflow.trip_service.client.UserClient;
import com.rideflow.trip_service.event.TripCancelledEvent;
import com.rideflow.trip_service.event.TripCompletedEvent;
import com.rideflow.trip_service.event.TripEventPublisher;
import com.rideflow.trip_service.model.Trip;
import com.rideflow.trip_service.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    TripRepository tripRepository;

    @Mock
    UserClient userClient;

    @Mock
    TripEventPublisher eventPublisher;

    @InjectMocks
    TripService tripService;

    private Trip tripWithStatus(Trip.Status status) {
        Trip trip = new Trip(1L, 12.9716, 77.5946, 12.9352, 77.6245);
        trip.setStatus(status);
        return trip;
    }

    // ---- transition ----

    @Test
    void illegalTransitionIsRejectedAndNothingIsSaved() {
        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(tripWithStatus(Trip.Status.REQUESTED)));

        assertThrows(IllegalStateException.class,
                () -> tripService.transition(1L, Trip.Status.COMPLETED));

        verify(tripRepository, never()).save(any(Trip.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void completingATripSavesItAndPublishesTripCompleted() {
        Trip trip = tripWithStatus(Trip.Status.IN_PROGRESS);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(call -> call.getArgument(0));

        Trip result = tripService.transition(1L, Trip.Status.COMPLETED);

        assertEquals(Trip.Status.COMPLETED, result.getStatus());
        verify(eventPublisher).publishTripCompleted(any(TripCompletedEvent.class));
        verify(eventPublisher, never()).publishTripCancelled(any(TripCancelledEvent.class));
    }

    @Test
    void cancellingAMatchedTripPublishesTripCancelledWithTheDriver() {
        Trip trip = tripWithStatus(Trip.Status.MATCHED);
        trip.setDriverId(2L);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(call -> call.getArgument(0));

        tripService.transition(1L, Trip.Status.CANCELLED);

        ArgumentCaptor<TripCancelledEvent> captor = ArgumentCaptor.forClass(TripCancelledEvent.class);
        verify(eventPublisher).publishTripCancelled(captor.capture());
        assertEquals(2L, captor.getValue().driverId());
    }

    @Test
    void movingToArrivingPublishesNoEvent() {
        Trip trip = tripWithStatus(Trip.Status.MATCHED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(call -> call.getArgument(0));

        tripService.transition(1L, Trip.Status.DRIVER_ARRIVING);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void transitionOnAnUnknownTripThrowsNotFound() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tripService.transition(99L, Trip.Status.CANCELLED));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ---- markMatched: handling events that may arrive twice or late ----

    @Test
    void aMatchEventAssignsTheDriverAndMovesTheTripToMatched() {
        Trip trip = tripWithStatus(Trip.Status.REQUESTED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        tripService.markMatched(1L, 2L);

        assertEquals(Trip.Status.MATCHED, trip.getStatus());
        assertEquals(2L, trip.getDriverId());
        verify(tripRepository).save(trip);
    }

    @Test
    void aDuplicateMatchEventIsIgnored() {
        Trip trip = tripWithStatus(Trip.Status.REQUESTED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        tripService.markMatched(1L, 2L);
        tripService.markMatched(1L, 3L);   // redelivery, even with a different driver

        assertEquals(2L, trip.getDriverId());
        verify(tripRepository, times(1)).save(trip);
    }

    @Test
    void aLateMatchEventDoesNotBringACancelledTripBackToLife() {
        Trip trip = tripWithStatus(Trip.Status.CANCELLED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        tripService.markMatched(1L, 2L);

        assertEquals(Trip.Status.CANCELLED, trip.getStatus());
        verify(tripRepository, never()).save(any(Trip.class));
    }
}