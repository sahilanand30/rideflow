package com.rideflow.matching_service.event;

import com.rideflow.matching_service.client.LocationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideRequestedListenerTest {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    LocationClient locationClient;

    RideRequestedListener listener;

    private final RideRequestedEvent event =
            new RideRequestedEvent(10L, 1L, 12.9716, 77.5946, 12.9352, 77.6245);

    @BeforeEach
    void setUp() {
        listener = new RideRequestedListener(kafkaTemplate, locationClient, 10.0);
    }

    private void givenCandidates(LocationClient.NearbyDriver... drivers) {
        when(locationClient.findNearby(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(drivers));
    }

    @Test
    void noDriversNearbyPublishesAFailureWithTheRightReason() {
        givenCandidates();

        listener.onRideRequested(event);

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("ride.match_failed"), eq("10"), sent.capture());
        assertEquals("NO_DRIVERS_NEARBY", ((RideMatchFailedEvent) sent.getValue()).reason());
        verify(locationClient, never()).reserve(anyLong(), anyLong());
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void theNearestFreeDriverIsMatchedAndTheOthersAreNotTouched() {
        givenCandidates(
                new LocationClient.NearbyDriver(2L, 0.1),
                new LocationClient.NearbyDriver(3L, 0.5));
        when(locationClient.reserve(2L, 10L)).thenReturn(true);

        listener.onRideRequested(event);

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("ride.matched"), eq("10"), sent.capture());
        assertEquals(2L, ((RideMatchedEvent) sent.getValue()).driverId());
        verify(locationClient, never()).reserve(eq(3L), anyLong());
    }

    @Test
    void ifTheNearestDriverIsAlreadyReservedTheNextOneIsTried() {
        givenCandidates(
                new LocationClient.NearbyDriver(2L, 0.1),
                new LocationClient.NearbyDriver(3L, 0.5));
        when(locationClient.reserve(2L, 10L)).thenReturn(false);
        when(locationClient.reserve(3L, 10L)).thenReturn(true);

        listener.onRideRequested(event);

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("ride.matched"), eq("10"), sent.capture());
        assertEquals(3L, ((RideMatchedEvent) sent.getValue()).driverId());
    }

    @Test
    void ifEveryDriverIsReservedTheFailureSaysAllDriversAreBusy() {
        givenCandidates(
                new LocationClient.NearbyDriver(2L, 0.1),
                new LocationClient.NearbyDriver(3L, 0.5));
        when(locationClient.reserve(anyLong(), anyLong())).thenReturn(false);

        listener.onRideRequested(event);

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("ride.match_failed"), eq("10"), sent.capture());
        assertEquals("ALL_DRIVERS_BUSY", ((RideMatchFailedEvent) sent.getValue()).reason());
        verify(kafkaTemplate, never()).send(eq("ride.matched"), anyString(), any());
    }
}