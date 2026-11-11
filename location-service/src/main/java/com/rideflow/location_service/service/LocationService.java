package com.rideflow.location_service.service;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {

    private static final String GEO_KEY = "drivers:geo";

    private final StringRedisTemplate redis;

    public LocationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void updateLocation(Long driverId, double lat, double lng) {
        // Redis expects longitude first, then latitude
        redis.opsForGeo().add(GEO_KEY, new Point(lng, lat), String.valueOf(driverId));
    }

    public List<NearbyDriver> findNearby(double lat, double lng, double radiusKm, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromCoordinate(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit));

        List<NearbyDriver> drivers = new ArrayList<>();
        if (results == null) {
            return drivers;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            drivers.add(new NearbyDriver(
                    Long.valueOf(result.getContent().getName()),
                    result.getDistance().getValue()));
        }
        return drivers;
    }

    public boolean reserveDriver(Long driverId, Long tripId, long ttlSeconds) {
        String key = "driver:" + driverId + ":reservation";
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(key, String.valueOf(tripId), Duration.ofSeconds(ttlSeconds));
        if (Boolean.TRUE.equals(acquired)) {
            return true;
        }
        // Already reserved: succeed only if it was reserved for this same trip (safe retry)
        return String.valueOf(tripId).equals(redis.opsForValue().get(key));
    }

    public void releaseDriver(Long driverId, Long tripId) {
        String key = "driver:" + driverId + ":reservation";
        String holder = redis.opsForValue().get(key);
        if (String.valueOf(tripId).equals(holder)) {
            redis.delete(key);
        }
    }

    public record NearbyDriver(Long driverId, double distanceKm) {}
}