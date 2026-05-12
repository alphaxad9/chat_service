// chat_service/src/main/java/com/example/chat_service/application/rooms/handlers/dtos/PrivateRoomCreationResponse.java

package com.example.chat_service.application.rooms.handlers.dtos;

import com.example.chat_service.external.users.dtos.UserView;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the response after successfully creating a PRIVATE (DIRECT) or GROUP room.
 * 
 * <p>Designed for immediate navigation to the newly created room view.
 * Contains only the essential display fields needed for the initial room render:
 * <ul>
 *   <li>Room identity and metadata (id, name, profile image)</li>
 *   <li>Minimal member list (usernames only, with creator shown as "You")</li>
 *   <li>Context flags (admin, is_group)</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>For GROUP rooms: {@code is_group=true}, {@code name=groupName}, {@code profileImageUrl} from room</li>
 *   <li>For DIRECT rooms: {@code is_group=false}, {@code name=friendUsername}, {@code profileImageUrl=friendProfilePic}</li>
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
 *   // For GROUP room:
 *   RoomAggregate room = roomService.createGroup(...);
 *   List<MemberAggregate> members = memberService.findByRoomId(room.id());
 *   Map<UUID, UserView> userIdToUser = authClient.getUsers(
 *       members.stream().map(MemberAggregate::userId).toList()
 *   );
 *   PrivateRoomCreationResponse response = PrivateRoomCreationResponse.fromRoom(
 *       room, userIdToUser, room.creatorId()
 *   );
 *   
 *   // Convert relative → absolute URL for frontend
 *   response = response.withProfileImageUrl("http://127.0.0.1:8005/uploads/rooms/xyz.jpg");
 *   return ResponseEntity.ok(response);
 * }</pre>
 * </p>
 */
public record PrivateRoomCreationResponse(

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
     * Minimal member representation for room creation response.
     * Contains only the username for display purposes.
     */
    public record MemberPreview(
            @JsonProperty("username")
            String username
    ) {}

    /**
     * Factory method to create a PrivateRoomCreationResponse from a RoomAggregate.
     * 
     * <p>Supports both GROUP and DIRECT room types:
     * <ul>
     *   <li><strong>GROUP rooms:</strong> uses {@code room.groupName()} for name,
     *       {@code room.profileImageUrl()} for profile image, sets {@code is_group=true}</li>
     *   <li><strong>DIRECT rooms:</strong> uses the OTHER participant's username for name,
     *       their profile picture for profile image, sets {@code is_group=false}</li>
     * </ul>
     * 
     * <p>Replaces the creator's username with "You" in the members list.
     * The profileImageUrl is stored as a relative path from the domain;
     * use {@link #withProfileImageUrl(String)} to convert to absolute URL.</p>
     * 
     * @param room the RoomAggregate containing room state
     * @param userIdToUser map of userId to full UserView for all room members
     * @param creatorId the UUID of the room creator (to replace with "You")
     * @return PrivateRoomCreationResponse ready for API response
     */
    public static PrivateRoomCreationResponse fromRoom(
            com.example.chat_service.domain.rooms.RoomAggregate room,
            Map<UUID, UserView> userIdToUser,
            UUID creatorId
    ) {
        String displayName = null;
        String relativeImageUrl = null;
        boolean isGroup = false;
        
        if (room.type() == com.example.chat_service.domain.rooms.Room.Type.GROUP) {
            // GROUP room: use group name and room's profile image
            displayName = room.groupName();
            relativeImageUrl = room.profileImageUrl();
            isGroup = true;
        } else {
            // DIRECT room: name = friend's username, profile image = friend's profile pic
            // Find the friend's UserView (the one that's not the creator)
            for (Map.Entry<UUID, UserView> entry : userIdToUser.entrySet()) {
                if (!entry.getKey().equals(creatorId)) {
                    UserView friendUser = entry.getValue();
                    displayName = friendUser.username();              // Friend's username as room "name"
                    relativeImageUrl = friendUser.profilePicture();   // Friend's profile image
                    break;
                }
            }
            isGroup = false;
        }
        
        // Transform userId->UserView map to MemberPreview list, replacing creator's username with "You"
        List<MemberPreview> previews = userIdToUser.entrySet().stream()
                .map(entry -> {
                    String username = entry.getValue().username();
                    String displayUsername = entry.getKey().equals(creatorId) ? "You" : username;
                    return new MemberPreview(displayUsername);
                })
                .toList();
        
        return new PrivateRoomCreationResponse(
                room.id(),
                displayName,
                relativeImageUrl,
                previews,
                true,   // admin - this response is always for the creator/admin context
                isGroup,
                relativeImageUrl != null && !relativeImageUrl.isBlank()
        );
    }

    /**
     * Convenience factory for testing or mock scenarios.
     * 
     * @param roomId the room UUID
     * @param name the display name (group name or friend username)
     * @param memberUsernames list of usernames to display (creator should already be "You" if desired)
     * @param isGroup whether this is a group room
     * @param profileImageUrl optional profile image URL (relative path)
     * @return PrivateRoomCreationResponse for testing purposes
     */
    public static PrivateRoomCreationResponse forTesting(
            UUID roomId,
            String name,
            List<String> memberUsernames,
            boolean isGroup,
            String profileImageUrl
    ) {
        return new PrivateRoomCreationResponse(
                roomId,
                name,
                profileImageUrl,
                memberUsernames.stream().map(MemberPreview::new).toList(),
                true,   // admin
                isGroup,
                profileImageUrl != null && !profileImageUrl.isBlank()
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
     * // DTO from domain has: profileImageUrl = "/uploads/users/profile/abc.jpg"
     * PrivateRoomCreationResponse response = PrivateRoomCreationResponse.fromRoom(room, userIdToUser, creatorId);
     * 
     * // Convert to absolute URL for API response
     * response = response.withProfileImageUrl("http://127.0.0.1:8005/uploads/users/profile/abc.jpg");
     * 
     * // Response JSON now contains:
     * // "profile_image_url": "http://127.0.0.1:8005/uploads/users/profile/abc.jpg",
     * // "has_profile_image": true
     * }</pre>
     * </p>
     * 
     * @param newProfileImageUrl the absolute URL to use, or null to remove/clear image
     * @return new PrivateRoomCreationResponse instance with updated profileImageUrl and recalculated hasProfileImage
     */
    public PrivateRoomCreationResponse withProfileImageUrl(String newProfileImageUrl) {
        return new PrivateRoomCreationResponse(
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