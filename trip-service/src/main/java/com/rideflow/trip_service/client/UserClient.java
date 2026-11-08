package com.rideflow.trip_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(@Value("${user-service.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public UserResponse getUser(Long userId) {
        try {
            return restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(UserResponse.class);
        } catch (RestClientResponseException e) {
            throw new IllegalArgumentException("User not found: " + userId);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("User service is unavailable");
        }
    }

    public record UserResponse(Long id, String name, String email, String role, String createdAt) {}
}