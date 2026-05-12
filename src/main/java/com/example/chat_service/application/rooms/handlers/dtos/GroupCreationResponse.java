package com.example.chat_service.application.rooms.handlers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the response after successfully creating a GROUP room.
 * 
 * <p>Designed for immediate navigation to the newly created room view.
 * Contains only the essential display fields needed for the initial room render:
 * <ul>
 *   <li>Room identity and metadata (id, name, profile image)</li>
 *   <li>Minimal member list (usernames only, with creator shown as "You")</li>
 *   <li>Context flags (admin=true, is_group=true)</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>{@code admin} and {@code is_group} are always {@code true} for this response type.</li>
 *   <li>Member list contains only usernames to minimize payload size for initial load.</li>
 *   <li>The creator's username is replaced with "You" for personalized UX.</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output.</li>
 * </ul>
 * </p>
 * 
 * <p><strong>profileImageUrl field note:</strong> Initially contains RELATIVE path
 * from domain/DB (e.g. {@code /uploads/rooms/abc.jpg}). The controller
 * converts this to an absolute URL using {@link #withProfileImageUrl(String)}
 * before sending the HTTP response.</p>
 * 
 * <p>Usage example in service layer:
 * <pre>{@code
 *   RoomAggregate room = roomService.createGroup(...);
 *   List<MemberAggregate> members = memberService.findByRoomId(room.id());
 *   Map<UUID, String> userIdToUsername = authClient.getUsernames(
 *       members.stream().map(MemberAggregate::userId).toList()
 *   );
 *   
 *   GroupCreationResponse response = GroupCreationResponse.fromRoom(
 *       room,
 *       userIdToUsername,
 *       room.creatorId()
 *   );
 *   // Convert relative → absolute URL for frontend
 *   response = response.withProfileImageUrl("http://127.0.0.1:8005/uploads/rooms/xyz.jpg");
 *   return ResponseEntity.ok(response);
 * }</pre>
 * </p>
 */
public record GroupCreationResponse(

        @JsonProperty("room_id")
        UUID roomId,

        @JsonProperty("name")
        String name,

        @JsonProperty("profile_image_url")
        String profileImageUrl,

        @JsonProperty("members")
        List<MemberPreview> members,

        @JsonProperty("admin")
        boolean admin,

        @JsonProperty("is_group")
        boolean isGroup,

        @JsonProperty("has_profile_image")
        boolean hasProfileImage

) {

    /**
     * Minimal member representation for group creation response.
     * Contains only the username for display purposes.
     */
    public record MemberPreview(
            @JsonProperty("username")
            String username
    ) {}

    /**
     * Factory method to create a GroupCreationResponse from a RoomAggregate.
     * 
     * <p>Automatically sets admin=true and is_group=true.
     * Replaces the creator's username with "You" in the members list.
     * The profileImageUrl is stored as a relative path from the domain;
     * use {@link #withProfileImageUrl(String)} to convert to absolute URL.</p>
     * 
     * @param room the RoomAggregate containing room state
     * @param userIdToUsername map of userId to username for all room members
     * @param creatorId the UUID of the room creator (to replace with "You")
     * @return GroupCreationResponse ready for API response
     */
    public static GroupCreationResponse fromRoom(
            com.example.chat_service.domain.rooms.RoomAggregate room,
            Map<UUID, String> userIdToUsername,
            UUID creatorId
    ) {
        // Validate room type
        if (room.type() != com.example.chat_service.domain.rooms.Room.Type.GROUP) {
            throw new IllegalStateException("GroupCreationResponse can only be created for GROUP rooms");
        }
        
        String relativeImageUrl = room.profileImageUrl();
        
        // Transform userId->username map to MemberPreview list, replacing creator's username with "You"
        List<MemberPreview> previews = userIdToUsername.entrySet().stream()
                .map(entry -> {
                    String username = entry.getValue();
                    String displayUsername = entry.getKey().equals(creatorId) ? "You" : username;
                    return new MemberPreview(displayUsername);
                })
                .toList();
        
        return new GroupCreationResponse(
                room.id(),
                room.groupName(),
                relativeImageUrl,
                previews,
                true,   // admin - this response is always for the creator/admin
                true,   // is_group - this is a group room response
                relativeImageUrl != null && !relativeImageUrl.isBlank()  // hasProfileImage
        );
    }

    /**
     * Convenience factory for testing or mock scenarios.
     * 
     * @param roomId the room UUID
     * @param name the group name
     * @param memberUsernames list of usernames to display (creator should already be "You" if desired)
     * @return GroupCreationResponse for testing purposes
     */
    public static GroupCreationResponse forTesting(
            UUID roomId,
            String name,
            List<String> memberUsernames
    ) {
        return new GroupCreationResponse(
                roomId,
                name,
                null,   // profileImageUrl (relative path)
                memberUsernames.stream()
                        .map(MemberPreview::new)
                        .toList(),
                true,   // admin
                true,   // is_group
                false   // hasProfileImage
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
     * GroupCreationResponse response = GroupCreationResponse.fromRoom(room, userIdToUsername, creatorId);
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
     * @param newProfileImageUrl the absolute URL to use, or null to remove/clear image
     * @return new GroupCreationResponse instance with updated profileImageUrl and recalculated hasProfileImage
     */
    public GroupCreationResponse withProfileImageUrl(String newProfileImageUrl) {
        return new GroupCreationResponse(
                this.roomId,
                this.name,
                newProfileImageUrl,
                this.members,
                this.admin,
                this.isGroup,
                // Recalculate hasProfileImage based on new value
                newProfileImageUrl != null && !newProfileImageUrl.isBlank()
        );
    }
}