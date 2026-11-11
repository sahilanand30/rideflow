package com.rideflow.trip_service.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trips")
public class Trip {

    public enum Status {
        REQUESTED, MATCHED, DRIVER_ARRIVING, IN_PROGRESS, COMPLETED, CANCELLED, NO_DRIVERS_AVAILABLE;

        public boolean canTransitionTo(Status next) {
            return switch (this) {
                case REQUESTED -> next == MATCHED || next == CANCELLED || next == NO_DRIVERS_AVAILABLE;
                case MATCHED -> next == DRIVER_ARRIVING || next == CANCELLED;
                case DRIVER_ARRIVING -> next == IN_PROGRESS || next == CANCELLED;
                case IN_PROGRESS -> next == COMPLETED;
                case COMPLETED, CANCELLED, NO_DRIVERS_AVAILABLE -> false;
            };
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long riderId;

    private Long driverId;

    private double pickupLat;
    private double pickupLng;
    private double dropLat;
    private double dropLng;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    public Trip() {
    }

    public Trip(Long riderId, double pickupLat, double pickupLng, double dropLat, double dropLng) {
        this.riderId = riderId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropLat = dropLat;
        this.dropLng = dropLng;
        this.status = Status.REQUESTED;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getRiderId() { return riderId; }
    public Long getDriverId() { return driverId; }
    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public double getDropLat() { return dropLat; }
    public double getDropLng() { return dropLng; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public void setStatus(Status status) { this.status = status; }
}