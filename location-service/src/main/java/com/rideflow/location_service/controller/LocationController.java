package com.rideflow.location_service.controller;


import com.rideflow.location_service.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/drivers/{driverId}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@PathVariable Long driverId, @RequestBody LocationRequest request) {
        locationService.updateLocation(driverId, request.lat(), request.lng());
    }

    @GetMapping("/drivers/nearby")
    public List<LocationService.NearbyDriver> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        return locationService.findNearby(lat, lng, radiusKm, limit);
    }

    @PostMapping("/drivers/{driverId}/reserve")
    public ReserveResponse reserve(@PathVariable Long driverId, @RequestBody ReserveRequest request) {
        return new ReserveResponse(locationService.reserveDriver(driverId, request.tripId(), 7200));
    }

    public record ReserveRequest(Long tripId) {}
    public record ReserveResponse(boolean reserved) {}

    public record LocationRequest(double lat, double lng) {}
}