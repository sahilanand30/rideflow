package com.rideflow.payment_service.repository;

import com.rideflow.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByTripId(Long tripId);
}