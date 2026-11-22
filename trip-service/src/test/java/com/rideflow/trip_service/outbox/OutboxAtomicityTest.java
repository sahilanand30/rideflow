package com.rideflow.trip_service.outbox;

import com.rideflow.trip_service.client.UserClient;
import com.rideflow.trip_service.event.TripEventPublisher;
import com.rideflow.trip_service.model.Trip;
import com.rideflow.trip_service.repository.TripRepository;
import com.rideflow.trip_service.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({TripService.class, TripEventPublisher.class, OutboxAtomicityTest.TestBeans.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxAtomicityTest {

    // Same timezone problem as before: PostgreSQL rejects "Asia/Calcutta".
    // Setting UTC before any connection is made avoids it.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @TestConfiguration
    static class TestBeans {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }

    @MockitoBean
    UserClient userClient;

    @Autowired
    TripService tripService;

    @Autowired
    TripRepository tripRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTablesAndStubRider() {
        outboxRepository.deleteAll();
        tripRepository.deleteAll();
        when(userClient.getUser(1L)).thenReturn(
                new UserClient.UserResponse(1L, "Rider", "rider@test.com", "RIDER", "2026-01-01T00:00:00Z"));
    }

    @Test
    void requestingATripSavesTheTripAndItsOutboxEventTogether() {
        Trip trip = tripService.requestTrip(1L, 12.9716, 77.5946, 12.9352, 77.6245);

        assertEquals(1L, tripRepository.count());
        List<OutboxEvent> events = outboxRepository.findAll();
        assertEquals(1, events.size());

        OutboxEvent event = events.get(0);
        assertEquals("ride.requested", event.getTopic());
        assertEquals(String.valueOf(trip.getId()), event.getEventKey());
        assertTrue(event.getPayload().contains("\"tripId\":" + trip.getId()));
        assertNull(event.getPublishedAt());
    }

    @Test
    void ifTheOutboxWriteFailsTheTripIsRolledBackToo() {
        // Make every new row in outbox_events fail, using a real database rule
        jdbcTemplate.execute(
                "ALTER TABLE outbox_events ADD CONSTRAINT force_failure CHECK (false) NOT VALID");
        try {
            assertThrows(RuntimeException.class,
                    () -> tripService.requestTrip(1L, 12.9716, 77.5946, 12.9352, 77.6245));

            assertEquals(0L, tripRepository.count());
            assertEquals(0L, outboxRepository.count());
        } finally {
            jdbcTemplate.execute("ALTER TABLE outbox_events DROP CONSTRAINT force_failure");
        }
    }
}