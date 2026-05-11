// chat_service/src/main/java/com/example/chat_service/external/users/dtos/UserResponse.java

package com.example.chat_service.external.users.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper DTO for the Django Auth Service response envelope.
 * 
 * The API returns: { "user": { ... } }
 * This class maps that outer structure so Jackson can deserialize
 * the inner UserView object correctly.
 */
public record UserResponse(

        @JsonProperty("user")
        UserView user

) {}