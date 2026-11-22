package com.rideflow.location_service.service;

import org.springframework.beans.factory.annotation.Value;
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
    private static final int SEARCH_POOL_SIZE = 50;

    private final StringRedisTemplate redis;
    private final long driverTtlSeconds;

    public LocationService(StringRedisTemplate redis,
                           @Value("${location.driver-ttl-seconds:60}") long driverTtlSeconds) {
        this.redis = redis;
        this.driverTtlSeconds = driverTtlSeconds;
    }

    public void updateLocation(Long driverId, double lat, double lng) {
        // Mark the driver online first, then record the position
        redis.opsForValue().set(onlineKey(driverId), "1", Duration.ofSeconds(driverTtlSeconds));
        // Redis expects longitude first, then latitude
        redis.opsForGeo().add(GEO_KEY, new Point(lng, lat), String.valueOf(driverId));
    }

    public List<NearbyDriver> findNearby(double lat, double lng, double radiusKm, int limit) {
        // Ask for more than we need, because some results may be filtered out as offline
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromCoordinate(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(SEARCH_POOL_SIZE));

        List<NearbyDriver> drivers = new ArrayList<>();
        if (results == null) {
            return drivers;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String driverId = result.getContent().getName();
            if (!Boolean.TRUE.equals(redis.hasKey("driver:" + driverId + ":online"))) {
                redis.opsForGeo().remove(GEO_KEY, driverId);   // clean up the stale entry
                continue;
            }
            drivers.add(new NearbyDriver(Long.valueOf(driverId), result.getDistance().getValue()));
            if (drivers.size() == limit) {
                break;
            }
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

    private String onlineKey(Long driverId) {
        return "driver:" + driverId + ":online";
    }

    public record NearbyDriver(Long driverId, double distanceKm) {}
}