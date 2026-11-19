package com.rideflow.trip_service.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public UserClient(@Value("${user-service.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(6)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
        this.circuitBreaker = CircuitBreaker.of("user-service", config);
        this.circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker user-service: {}", event.getStateTransition()));
    }

    public UserResponse getUser(Long userId) {
        try {
            return circuitBreaker.executeSupplier(() -> fetchUser(userId));
        } catch (CallNotPermittedException e) {
            throw new IllegalStateException("User service is unavailable (circuit open)");
        }
    }

    private UserResponse fetchUser(Long userId) {
        try {
            return restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(UserResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 400) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
            throw new IllegalStateException("User service error: " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("User service is unavailable");
        }
    }

    public record UserResponse(Long id, String name, String email, String role, String createdAt) {}
}