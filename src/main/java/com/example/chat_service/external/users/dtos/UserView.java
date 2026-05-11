package com.example.chat_service.external.users.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * DTO representing a user fetched from the external Auth Service.
 * Maps snake_case JSON fields from Django API to Java camelCase.
 * 
 * Contains only minimal fields needed for display/enrichment.
 * Add more fields later as requirements evolve.
 */
public record UserView(

        @JsonProperty("user_id")
        UUID userId,

        String username,

        String email,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        @JsonProperty("profile_picture")
        String profilePicture

) {}