package com.example.chat_service.external.users.dtos.users.services;

import com.example.chat_service.config.AuthProperties;
import com.example.chat_service.external.users.dtos.UserResponse;
import com.example.chat_service.external.users.dtos.UserView;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Client for fetching user data from the external Auth Service.
 * 
 * Handles:
 * - Building the correct endpoint URL
 * - Adding the X-Internal-Key header for service-to-service auth
 * - Deserializing the { "user": { ... } } response envelope
 * - Logging and error handling
 * 
 * Usage:
 *   UserView user = userApiClient.getUserById(UUID.fromString("..."));
 */
@Slf4j
@Service
public class UserApiClient {

    private final RestClient restClient;
    private final AuthProperties authProperties;

    public UserApiClient(
            RestClient restClient,
            AuthProperties authProperties
    ) {
        this.restClient = restClient;
        this.authProperties = authProperties;
    }

    /**
     * Fetches a user by ID from the external Auth Service.
     * 
     * @param userId the UUID of the user to fetch
     * @return UserView containing minimal user data for display/enrichment
     * @throws RuntimeException if the response is null or user not found
     */
    public UserView getUserById(UUID userId) {

        String url = authProperties.serviceUrl()
                + "/users/users/"
                + userId
                + "/";

        log.info("Fetching user from auth service: {}", userId);

        UserResponse response = restClient.get()
                .uri(url)
                .header(
                        "X-Internal-Key",
                        authProperties.internalApiKey()
                )
                .retrieve()
                .body(UserResponse.class);

        if (response == null || response.user() == null) {
            log.error("User response was null for userId: {}", userId);
            throw new RuntimeException(
                    "User response was null for userId: " + userId
            );
        }

        return response.user();
    }
}