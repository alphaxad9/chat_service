// chat_service/src/main/java/com/example/chat_service/application/members/handlers/dtos/MemberResponseDTO.java
package com.example.chat_service.application.members.handlers.dtos;

import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.external.users.dtos.UserView;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Data Transfer Object representing a Group Member for API/UI consumption.
 * 
 * <p>Combines:
 * <ul>
 *   <li>MemberAggregate (domain logic + membership state)</li>
 *   <li>UserView (enriched user data from external Auth Service)</li>
 * </ul>
 * </p>
 * 
 * <p>This decouples the internal domain model from external representation
 * and allows attaching resolved user data without leaking domain internals.
 * The raw {@code userId} from the domain is replaced with a full {@link UserView}
 * object containing username, profile picture, and other display fields.</p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>{@code isAdmin} is pre-computed from domain logic for convenient frontend consumption.</li>
 *   <li>{@code status} is serialized as a String ("ADMIN" or "USER") for API clarity.</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output.</li>
 *   <li>Internal tracking fields (timestamps, unread counts, activity flags) are excluded 
 *       from command/response scenarios to keep the API surface minimal and focused.</li>
 * </ul>
 * </p>
 * 
 * <p>Usage:
 * <pre>{@code
 *   // In application service layer:
 *   MemberAggregate memberAggregate = memberRepository.findById(memberId);
 *   UserView userView = authClient.getUser(memberAggregate.userId());
 *   
 *   MemberResponseDTO dto = MemberResponseDTO.fromAggregate(memberAggregate, userView);
 *   return ResponseEntity.ok(dto); // Jackson auto-serializes to JSON
 * }</pre>
 * </p>
 */
public record MemberResponseDTO(

        @JsonProperty("member_id")
        UUID memberId,

        @JsonProperty("user")
        UserView user,

        @JsonProperty("room_id")
        UUID roomId,

        String status,

        @JsonProperty("is_admin")
        boolean isAdmin

) {

    /**
     * Factory method to create a MemberResponseDTO by combining:
     * - MemberAggregate (domain state)
     * - UserView (resolved user data from external Auth Service)
     * 
     * <p>Pre-computes {@code isAdmin} from domain logic for frontend convenience.</p>
     * 
     * @param aggregate the MemberAggregate containing domain state
     * @param user the resolved UserView from Auth Service
     * @return enriched MemberResponseDTO ready for API response
     */
    public static MemberResponseDTO fromAggregate(MemberAggregate aggregate, UserView user) {
        Member member = aggregate.member();
        
        return new MemberResponseDTO(
                member.id(),
                user,
                member.roomId(),
                member.status().name(),
                member.isAdmin()
        );
    }

    /**
     * Convenience factory for testing or when user data is not yet resolved.
     * Creates a placeholder UserView with minimal data.
     * 
     * <p>⚠️ Use only for internal/testing scenarios — prefer {@link #fromAggregate(MemberAggregate, UserView)} 
     * in production to ensure enriched user data.</p>
     * 
     * @param aggregate the MemberAggregate containing domain state
     * @return MemberResponseDTO with placeholder user data
     */
    public static MemberResponseDTO fromAggregateWithPlaceholderUser(MemberAggregate aggregate) {
        Member member = aggregate.member();
        
        UserView placeholderUser = new UserView(
                member.userId(),
                "user_" + member.userId().toString().substring(0, 8),
                null,   // email
                null,   // firstName
                null,   // lastName
                null    // profilePicture
        );
        
        return fromAggregate(aggregate, placeholderUser);
    }

    /**
     * Create a new DTO instance with an updated status.
     * 
     * <p>Used to reflect role changes (promote/demote) without modifying 
     * the original immutable record. Automatically recalculates {@code isAdmin}.</p>
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     *   // After promoting a member in the domain:
     *   memberAggregate.promote();
     *   
     *   // Update DTO for response:
     *   dto = dto.withStatus("ADMIN");
     *   // JSON now contains: "status": "ADMIN", "is_admin": true
     * }</pre>
     * </p>
     * 
     * @param newStatus the new status value ("ADMIN" or "USER")
     * @return new MemberResponseDTO instance with updated status and recalculated isAdmin
     */
    public MemberResponseDTO withStatus(String newStatus) {
        return new MemberResponseDTO(
                this.memberId,
                this.user,
                this.roomId,
                newStatus,
                "ADMIN".equalsIgnoreCase(newStatus)
        );
    }

    /**
     * Convert to Map for logging, testing, or manual serialization.
     * 
     * <p>Not required for Spring MVC responses (Jackson handles records automatically),
     * but useful for debugging, audit logging, or non-JSON use cases.</p>
     * 
     * @return immutable Map representation of this DTO
     */
    public java.util.Map<String, Object> toMap() {
        return java.util.Map.of(
                "member_id", memberId.toString(),
                "user", userViewToMap(user),
                "room_id", roomId.toString(),
                "status", status,
                "is_admin", isAdmin
        );
    }

    /**
     * Nested helper to convert UserView to Map (since UserView is a record).
     * 
     * <p>Attempts to call UserView.toMap() via reflection if available,
     * otherwise falls back to manual field mapping.</p>
     * 
     * @param user the UserView to convert
     * @return Map representation or null if user is null
     */
    private static java.util.Map<String, Object> userViewToMap(UserView user) {
        if (user == null) return null;
        
        // If UserView has a toMap() method, use it via reflection
        try {
            var method = UserView.class.getMethod("toMap");
            return (java.util.Map<String, Object>) method.invoke(user);
        } catch (Exception e) {
            // Fallback manual mapping
            return java.util.Map.of(
                    "user_id", user.userId().toString(),
                    "username", user.username(),
                    "email", user.email(),
                    "first_name", user.firstName(),
                    "last_name", user.lastName(),
                    "profile_picture", user.profilePicture()
            );
        }
    }
}