package com.rideflow.trip_service.controller;

import com.rideflow.trip_service.model.Trip;
import com.rideflow.trip_service.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Trip requestTrip(@RequestBody RequestTripRequest request) {
        return tripService.requestTrip(
                request.riderId(),
                request.pickupLat(), request.pickupLng(),
                request.dropLat(), request.dropLng());
    }

    @GetMapping("/{id}")
    public Trip getTrip(@PathVariable Long id) {
        return tripService.getTrip(id);
    }

    @PostMapping("/{id}/cancel")
    public Trip cancelTrip(@PathVariable Long id) {
        return tripService.cancelTrip(id);
    }

    public record RequestTripRequest(Long riderId,
                                     double pickupLat, double pickupLng,
                                     double dropLat, double dropLng) {}
}