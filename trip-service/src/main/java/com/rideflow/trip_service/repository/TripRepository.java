package com.rideflow.trip_service.repository;

import com.rideflow.trip_service.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByRiderId(Long riderId);
}