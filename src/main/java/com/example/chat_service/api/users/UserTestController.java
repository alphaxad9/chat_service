package com.example.chat_service.api.users;

import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.services.UserApiClient;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Test controller for fetching user data from the external Auth Service.
 * 
 * Endpoint: GET /api/users/{userId}
 * 
 * Usage:
 *   curl http://127.0.0.1:8005/api/users/012a181c-0c14-4aba-b868-4329555c3540
 * 
 * Note: This is for development/testing. In production, user fetching
 * would typically be internal to service logic, not exposed via public API.
 */
@RestController
@RequestMapping("/api/users")
public class UserTestController {

    private final UserApiClient userApiClient;

    public UserTestController(UserApiClient userApiClient) {
        this.userApiClient = userApiClient;
    }

    /**
     * Fetches a user by ID from the external Auth Service.
     * 
     * @param userId the UUID of the user to fetch (from path variable)
     * @return UserView containing minimal user data
     */
    @GetMapping("/{userId}")
    public UserView getUser(@PathVariable UUID userId) {
        return userApiClient.getUserById(userId);
    }
}