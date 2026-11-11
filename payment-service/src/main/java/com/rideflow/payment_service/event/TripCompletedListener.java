package com.rideflow.payment_service.event;

import com.rideflow.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TripCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(TripCompletedListener.class);
    private PaymentService paymentService;

    public TripCompletedListener(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "trip.completed")
    public void onTripCompleted(TripCompletedEvent event) {
        paymentService.processTripCompleted(event);
    }
}