package com.rideflow.payment_service.service;

import com.rideflow.payment_service.event.PaymentCompletedEvent;
import com.rideflow.payment_service.event.TripCompletedEvent;
import com.rideflow.payment_service.model.Payment;
import com.rideflow.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final FareCalculator fareCalculator;
    private KafkaTemplate<String,Object>kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository, FareCalculator fareCalculator, KafkaTemplate<String, Object>kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.fareCalculator = fareCalculator;
        this.kafkaTemplate = kafkaTemplate;
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
        kafkaTemplate.send("payment.completed", String.valueOf(event.tripId()),
                new PaymentCompletedEvent(event.tripId(), event.riderId(), event.driverId(), amount));
        log.info("Charged {} for trip {}", amount, event.tripId());
    }
}