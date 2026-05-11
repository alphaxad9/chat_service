// chat_service/src/main/java/com/example/chat_service/application/members/handlers/dtos/MemberQueryResponseDTO.java
package com.example.chat_service.application.members.handlers.dtos;

import com.example.chat_service.domain.members.Member;
import com.example.chat_service.external.users.dtos.UserView;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a Group Member for query/list API responses.
 * 
 * <p>Combines:
 * <ul>
 *   <li>{@link Member} (raw domain model with membership state and metadata)</li>
 *   <li>{@link UserView} (enriched user data from external Auth Service)</li>
 * </ul>
 * </p>
 * 
 * <p>This DTO is optimized for <strong>read/query scenarios</strong> (e.g., listing room members, 
 * fetching member details) and includes additional fields that were excluded from command/response 
 * DTOs to keep mutation APIs minimal:</p>
 * <ul>
 *   <li>{@code unread_messages} - count of unread messages for this member</li>
 *   <li>{@code joined_at} - timestamp when member joined the room</li>
 *   <li>{@code updated_at} - last activity/update timestamp</li>
 *   <li>{@code is_active} - whether member has left the room (soft-delete flag)</li>
 * </ul>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>Uses raw {@link Member} domain model directly — no aggregate logic required for queries.</li>
 *   <li>{@code isAdmin} is computed from {@code Member.status()} for frontend convenience.</li>
 *   <li>{@code status} is serialized as String ("ADMIN" or "USER") for API clarity.</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output.</li>
 *   <li>Immutable record pattern ensures thread-safety and predictable serialization.</li>
 * </ul>
 * </p>
 * 
 * <p><strong>When to use:</strong>
 * <pre>{@code
 *   // ✅ Use for: GET /rooms/{id}/members, GET /members/{id}, search results
 *   List<Member> members = memberRepository.findByRoomId(roomId);
 *   List<MemberQueryResponseDTO> dtos = members.stream()
 *       .map(m -> {
 *           UserView user = authClient.getUser(m.userId());
 *           return MemberQueryResponseDTO.fromMember(m, user);
 *       })
 *       .toList();
 * 
 *   // Avoid for: POST/PUT commands — use MemberResponseDTO or command-specific DTOs instead
 * }</pre>
 * </p>
 */
public record MemberQueryResponseDTO(

        @JsonProperty("member_id")
        UUID memberId,

        @JsonProperty("user")
        UserView user,

        @JsonProperty("room_id")
        UUID roomId,

        String status,

        @JsonProperty("is_admin")
        boolean isAdmin,

        @JsonProperty("unread_messages")
        int unreadMessages,

        @JsonProperty("joined_at")
        LocalDateTime joinedAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("is_active")
        boolean isActive

) {

    /**
     * Factory method to create a MemberQueryResponseDTO by combining:
     * - Member (raw domain model with full metadata)
     * - UserView (resolved user data from external Auth Service)
     * 
     * <p>Pre-computes {@code isAdmin} and {@code isActive} from domain state 
     * for convenient frontend consumption without leaking domain internals.</p>
     * 
     * @param member the Member domain object containing full membership state
     * @param user the resolved UserView from Auth Service
     * @return enriched MemberQueryResponseDTO ready for query API response
     */
    public static MemberQueryResponseDTO fromMember(Member member, UserView user) {
        return new MemberQueryResponseDTO(
                member.id(),
                user,
                member.roomId(),
                member.status().name(),
                member.isAdmin(),
                member.unreadMessages(),
                member.joinedAt(),
                member.updatedAt(),
                member.isActive()
        );
    }

    /**
     * Convenience factory for testing or when user data is not yet resolved.
     * Creates a placeholder UserView with minimal data.
     * 
     * <p>⚠️ Use only for internal/testing scenarios — prefer {@link #fromMember(Member, UserView)} 
     * in production to ensure enriched user data.</p>
     * 
     * @param member the Member domain object containing full membership state
     * @return MemberQueryResponseDTO with placeholder user data
     */
    public static MemberQueryResponseDTO fromMemberWithPlaceholderUser(Member member) {
        UserView placeholderUser = new UserView(
                member.userId(),
                "user_" + member.userId().toString().substring(0, 8),
                null,   // email
                null,   // firstName
                null,   // lastName
                null    // profilePicture
        );
        
        return fromMember(member, placeholderUser);
    }

    /**
     * Create a new DTO instance with an updated unread message count.
     * 
     * <p>Used to reflect real-time unread updates without modifying 
     * the original immutable record. Useful for WebSocket push payloads.</p>
     * 
     * @param newUnreadCount the new unread messages count
     * @return new MemberQueryResponseDTO instance with updated unread_messages
     */
    public MemberQueryResponseDTO withUnreadMessages(int newUnreadCount) {
        if (newUnreadCount < 0) {
            throw new IllegalArgumentException("unread count cannot be negative");
        }
        return new MemberQueryResponseDTO(
                this.memberId,
                this.user,
                this.roomId,
                this.status,
                this.isAdmin,
                newUnreadCount,
                this.joinedAt,
                this.updatedAt,
                this.isActive
        );
    }

    /**
     * Create a new DTO instance with an updated active status.
     * 
     * <p>Used when member leaves/joins without re-fetching from domain.</p>
     * 
     * @param newActive the new active status
     * @return new MemberQueryResponseDTO instance with updated is_active
     */
    public MemberQueryResponseDTO withActiveStatus(boolean newActive) {
        return new MemberQueryResponseDTO(
                this.memberId,
                this.user,
                this.roomId,
                this.status,
                this.isAdmin,
                this.unreadMessages,
                this.joinedAt,
                this.updatedAt,
                newActive
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
                "is_admin", isAdmin,
                "unread_messages", unreadMessages,
                "joined_at", joinedAt.toString(),
                "updated_at", updatedAt.toString(),
                "is_active", isActive
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