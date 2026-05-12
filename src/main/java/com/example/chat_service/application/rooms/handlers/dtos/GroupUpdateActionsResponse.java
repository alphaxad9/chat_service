package com.example.chat_service.application.rooms.handlers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the response after successfully executing a room update action
 * (rename, update description, update images, or delete).
 * 
 * <p>Designed for immediate UI synchronization after a mutation. Contains:
 * <ul>
 *   <li>Current room state (name, description, image URLs)</li>
 *   <li>Minimal member list (usernames only, with requester shown as "You")</li>
 *   <li>Context flags (admin, is_group)</li>
 *   <li>Operation metadata (what was updated, when)</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Supported operations:</strong>
 * <ul>
 *   <li>{@code "update_name"} - room name was changed</li>
 *   <li>{@code "update_description"} - room description was changed</li>
 *   <li>{@code "update_cover_image"} - room cover image was changed</li>
 *   <li>{@code "update_profile_image"} - room profile image was changed</li>
 *   <li>{@code "delete"} - room was soft-deleted (state reflects pre-deletion snapshot)</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>{@code admin} reflects whether the requester is the room creator (extendable for multi-admin).</li>
 *   <li>Image URL fields contain RELATIVE paths from domain/DB (e.g. {@code /uploads/rooms/abc.jpg}).
 *       Use {@link #withProfileImageUrl(String)} and {@link #withCoverImageUrl(String)} 
 *       to convert to absolute URLs before sending HTTP response.</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output.</li>
 *   <li>For {@code delete} operations, frontend should use {@code operation="delete"} 
 *       to trigger appropriate UI transitions (e.g., navigation away from room).</li>
 * </ul>
 * </p>
 * 
 * <p>Usage example in service/controller layer:
 * <pre>{@code
 *   RoomAggregate room = roomCommandService.updateGroupName(roomId, newName, requesterId);
 *   List<MemberAggregate> members = memberService.findByRoomId(roomId);
 *   Map<UUID, String> userIdToUsername = authClient.getUsernames(
 *       members.stream().map(MemberAggregate::userId).toList()
 *   );
 *   
 *   GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(
 *       room,
 *       userIdToUsername,
 *       requesterId
 *   );
 *   // Convert relative → absolute URLs for frontend
 *   response = response
 *       .withProfileImageUrl("http://127.0.0.1:8005" + room.profileImageUrl())
 *       .withCoverImageUrl("http://127.0.0.1:8005" + room.coverImageUrl());
 *   return ResponseEntity.ok(response);
 * }</pre>
 * </p>
 */
public record GroupUpdateActionsResponse(

        @JsonProperty("room_id")
        UUID roomId,

        @JsonProperty("name")
        String name,

        @JsonProperty("description")
        String description,

        @JsonProperty("profile_image_url")
        String profileImageUrl,

        @JsonProperty("cover_image_url")
        String coverImageUrl,

        @JsonProperty("members")
        List<MemberPreview> members,

        @JsonProperty("admin")
        boolean admin,

        @JsonProperty("is_group")
        boolean isGroup,

        @JsonProperty("has_profile_image")
        boolean hasProfileImage,

        @JsonProperty("has_cover_image")
        boolean hasCoverImage,

        @JsonProperty("updated_at")
        String updatedAt

) {

    /**
     * Minimal member representation for update response.
     * Contains only the username for display purposes.
     */
    public record MemberPreview(
            @JsonProperty("username")
            String username
    ) {}

    /**
     * Factory method to create a GroupUpdateActionsResponse from a RoomAggregate.
     * 
     * <p>Automatically determines admin status by comparing requesterId with room creatorId.
     * Replaces the requester's username with "You" in the members list for personalized UX.
     * Image URLs and description are stored as relative paths/nullable values from the domain;
     * use {@link #withProfileImageUrl(String)} and {@link #withCoverImageUrl(String)} 
     * to convert to absolute URLs.</p>
     * 
     * @param room the RoomAggregate containing current room state
     * @param userIdToUsername map of userId to username for all room members
     * @param requesterId the UUID of the user who performed the action (for "You" substitution and admin check)
     * @return GroupUpdateActionsResponse ready for API response
     * @throws IllegalStateException if room type is not GROUP
     */
    public static GroupUpdateActionsResponse fromRoom(
            com.example.chat_service.domain.rooms.RoomAggregate room,
            Map<UUID, String> userIdToUsername,
            UUID requesterId
    ) {
        // Validate room type
        if (room.type() != com.example.chat_service.domain.rooms.Room.Type.GROUP) {
            throw new IllegalStateException("GroupUpdateActionsResponse can only be created for GROUP rooms");
        }
        
        String relativeProfileUrl = room.profileImageUrl();
        String relativeCoverUrl = room.coverImageUrl();
        String relativeDescription = room.description();
        
        // Determine admin status: creator is always admin; extendable for multi-admin support
        boolean isAdmin = room.creatorId().equals(requesterId);
        
        // Transform userId->username map to MemberPreview list, replacing requester's username with "You"
        List<MemberPreview> previews = userIdToUsername.entrySet().stream()
                .map(entry -> {
                    String username = entry.getValue();
                    String displayUsername = entry.getKey().equals(requesterId) ? "You" : username;
                    return new MemberPreview(displayUsername);
                })
                .toList();
        
        return new GroupUpdateActionsResponse(
                room.id(),
                room.groupName(),
                relativeDescription != null ? relativeDescription : "",
                relativeProfileUrl != null ? relativeProfileUrl : "",
                relativeCoverUrl != null ? relativeCoverUrl : "",
                previews,
                isAdmin,
                true,   // is_group - this response is for group rooms only
                relativeProfileUrl != null && !relativeProfileUrl.isBlank(),
                relativeCoverUrl != null && !relativeCoverUrl.isBlank(),
                Instant.now().toString()  // ISO-8601 timestamp
        );
    }

    /**
     * Convenience factory for testing or mock scenarios.
     * 
     * @param roomId the room UUID
     * @param name the group name
     * @param description the room description (can be empty)
     * @param memberUsernames list of usernames to display (requester should already be "You" if desired)
     * @return GroupUpdateActionsResponse for testing purposes
     */
    public static GroupUpdateActionsResponse forTesting(
            UUID roomId,
            String name,
            String description,
            List<String> memberUsernames
    ) {
        return new GroupUpdateActionsResponse(
                roomId,
                name,
                description != null ? description : "",
                null,   // profileImageUrl (relative path)
                null,   // coverImageUrl (relative path)
                memberUsernames.stream()
                        .map(MemberPreview::new)
                        .toList(),
                true,   // admin (for testing)
                true,   // is_group
                false,  // hasProfileImage
                false,  // hasCoverImage
                Instant.now().toString()
        );
    }

    /**
     * Create a new DTO instance with an updated profileImageUrl.
     * 
     * <p>Used to convert relative paths (from domain/DB) to absolute URLs
     * (for frontend consumption) without modifying the original immutable record.</p>
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // DTO from domain has: profileImageUrl = "/uploads/rooms/abc.jpg"
     * GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(room, userIdToUsername, requesterId);
     * 
     * // Convert to absolute URL for API response
     * response = response.withProfileImageUrl("http://127.0.0.1:8005/uploads/rooms/abc.jpg");
     * 
     * // Response JSON now contains:
     * // "profile_image_url": "http://127.0.0.1:8005/uploads/rooms/abc.jpg",
     * // "has_profile_image": true
     * }</pre>
     * </p>
     * 
     * @param newProfileImageUrl the absolute URL to use, or null/blank to clear
     * @return new GroupUpdateActionsResponse instance with updated profileImageUrl and recalculated hasProfileImage
     */
    public GroupUpdateActionsResponse withProfileImageUrl(String newProfileImageUrl) {
        return new GroupUpdateActionsResponse(
                this.roomId,
                this.name,
                this.description,
                newProfileImageUrl,
                this.coverImageUrl,
                this.members,
                this.admin,
                this.isGroup,
                // Recalculate hasProfileImage based on new value
                newProfileImageUrl != null && !newProfileImageUrl.isBlank(),
                this.hasCoverImage,
                this.updatedAt
        );
    }

    /**
     * Create a new DTO instance with an updated coverImageUrl.
     * 
     * <p>Used to convert relative paths (from domain/DB) to absolute URLs
     * (for frontend consumption) without modifying the original immutable record.</p>
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // DTO from domain has: coverImageUrl = "/uploads/rooms/covers/xyz.jpg"
     * GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(room, userIdToUsername, requesterId);
     * 
     * // Convert to absolute URL for API response
     * response = response.withCoverImageUrl("http://127.0.0.1:8005/uploads/rooms/covers/xyz.jpg");
     * 
     * // Response JSON now contains:
     * // "cover_image_url": "http://127.0.0.1:8005/uploads/rooms/covers/xyz.jpg",
     * // "has_cover_image": true
     * }</pre>
     * </p>
     * 
     * @param newCoverImageUrl the absolute URL to use, or null/blank to clear
     * @return new GroupUpdateActionsResponse instance with updated coverImageUrl and recalculated hasCoverImage
     */
    public GroupUpdateActionsResponse withCoverImageUrl(String newCoverImageUrl) {
        return new GroupUpdateActionsResponse(
                this.roomId,
                this.name,
                this.description,
                this.profileImageUrl,
                newCoverImageUrl,
                this.members,
                this.admin,
                this.isGroup,
                this.hasProfileImage,
                // Recalculate hasCoverImage based on new value
                newCoverImageUrl != null && !newCoverImageUrl.isBlank(),
                this.updatedAt
        );
    }
}