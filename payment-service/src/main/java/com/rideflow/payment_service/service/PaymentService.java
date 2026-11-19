package com.rideflow.payment_service.service;

import com.rideflow.payment_service.event.PaymentCompletedEvent;
import com.rideflow.payment_service.event.TripCompletedEvent;
import com.rideflow.payment_service.model.Payment;
import com.rideflow.payment_service.outbox.OutboxEvent;
import com.rideflow.payment_service.outbox.OutboxRepository;
import com.rideflow.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";

    private final PaymentRepository paymentRepository;
    private final FareCalculator fareCalculator;
    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          FareCalculator fareCalculator,
                          OutboxRepository outboxRepository,
                          JsonMapper jsonMapper) {
        this.paymentRepository = paymentRepository;
        this.fareCalculator = fareCalculator;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void processTripCompleted(TripCompletedEvent event) {
        if (paymentRepository.existsByTripId(event.tripId())) {
            log.info("Payment for trip {} already exists, skipping", event.tripId());
            return;
        }

        BigDecimal amount = fareCalculator.calculate(
                event.pickupLat(), event.pickupLng(), event.dropLat(), event.dropLng());
        paymentRepository.save(new Payment(event.tripId(), event.riderId(), event.driverId(), amount));

        PaymentCompletedEvent completed = new PaymentCompletedEvent(
                event.tripId(), event.riderId(), event.driverId(), amount);
        String payload = jsonMapper.writeValueAsString(completed);
        outboxRepository.save(new OutboxEvent(
                PAYMENT_COMPLETED_TOPIC, String.valueOf(event.tripId()), payload));

        log.info("Charged {} for trip {}", amount, event.tripId());
    }
}