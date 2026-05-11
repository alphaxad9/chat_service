// chat_service/src/main/java/com/example/chat_service/api/chat/dtos/CreateMemberRequest.java
package com.example.chat_service.api.chat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Request body for creating a new room member.
 */
public record CreateMemberRequest(

        @JsonProperty("user_id")
        UUID userId,

        String status  // Optional: "ADMIN" or "USER"; defaults to USER

) {}