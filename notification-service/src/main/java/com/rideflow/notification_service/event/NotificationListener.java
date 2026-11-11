package com.rideflow.notification_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(
            topics = "ride.matched",
            properties = "spring.json.value.default.type=com.rideflow.notification_service.event.RideMatchedEvent")
    public void onRideMatched(RideMatchedEvent event) {
        log.info("NOTIFY: Your ride (trip {}) is confirmed. Driver {} is on the way.",
                event.tripId(), event.driverId());
    }

    @KafkaListener(
            topics = "payment.completed",
            properties = "spring.json.value.default.type=com.rideflow.notification_service.event.PaymentCompletedEvent")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("NOTIFY: Trip {} is complete. You were charged {}.",
                event.tripId(), event.amount());
    }
}