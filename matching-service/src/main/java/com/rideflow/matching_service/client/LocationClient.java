package com.rideflow.matching_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class LocationClient {

    private final RestClient restClient;

    public LocationClient(@Value("${location-service.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public List<NearbyDriver> findNearby(double lat, double lng, double radiusKm) {
        NearbyDriver[] result = restClient.get()
                .uri(uri -> uri.path("/drivers/nearby")
                        .queryParam("lat", lat)
                        .queryParam("lng", lng)
                        .queryParam("radiusKm", radiusKm)
                        .queryParam("limit", 5)
                        .build())
                .retrieve()
                .body(NearbyDriver[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }

    public boolean reserve(Long driverId, Long tripId) {
        ReserveResponse response = restClient.post()
                .uri("/drivers/{id}/reserve", driverId)
                .body(new ReserveRequest(tripId))
                .retrieve()
                .body(ReserveResponse.class);
        return response != null && response.reserved();
    }

    public record ReserveRequest(Long tripId) {}
    public record ReserveResponse(boolean reserved) {}
    public record NearbyDriver(Long driverId, double distanceKm) {}
}