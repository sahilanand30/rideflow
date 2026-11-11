package com.rideflow.payment_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    public enum Status { COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long tripId;

    private Long riderId;
    private Long driverId;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    public Payment() {
    }

    public Payment(Long tripId, Long riderId, Long driverId, BigDecimal amount) {
        this.tripId = tripId;
        this.riderId = riderId;
        this.driverId = driverId;
        this.amount = amount;
        this.status = Status.COMPLETED;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public Long getRiderId() { return riderId; }
    public Long getDriverId() { return driverId; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}