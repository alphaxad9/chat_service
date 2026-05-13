package com.example.chat_service.application.rooms.handlers.dtos;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.external.users.dtos.UserView;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO representing a room's full details for the "Get Room by ID" query endpoint.
 * 
 * <p>Designed for displaying room settings, member management, and detailed room information.
 * Contains all essential fields needed for rendering a complete room detail view:
 * <ul>
 *   <li>Room identity and type (id, type as "GROUP" or "DIRECT")</li>
 *   <li>Display name (group name for GROUP rooms, friend username for DIRECT rooms)</li>
 *   <li>Profile and cover image URLs for visual identification (GROUP rooms only have cover image)</li>
 *   <li>Group-specific metadata (description) for GROUP rooms</li>
 *   <li>Context flags (is_admin, is_owner, has_profile_image, has_cover_image)</li>
 *   <li>Timestamps for audit and ordering purposes</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>For GROUP rooms: {@code name=groupName}, {@code profileImageUrl} and {@code coverImageUrl} from room metadata, {@code description} included</li>
 *   <li>For DIRECT rooms: {@code name=friendUsername}, {@code profileImageUrl=friendProfilePicture}, {@code coverImageUrl=null}, {@code description=null}</li>
 *   <li>{@code is_admin} is computed from the requesting user's membership status in the room</li>
 *   <li>{@code is_owner} is computed by comparing {@code creatorId} with the requesting user's ID</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output</li>
 *   <li>Immutable record pattern ensures thread-safety and predictable state</li>
 * </ul>
 * </p>
 * 
 * <p><strong>URL field notes:</strong> {@code profileImageUrl} and {@code coverImageUrl} initially 
 * contain RELATIVE paths from domain/DB (e.g. {@code /uploads/rooms/abc.jpg} or 
 * {@code /uploads/users/xyz.jpg}). The controller converts these to absolute URLs using 
 * {@link #withProfileImageUrl(String)} and {@link #withCoverImageUrl(String)} before sending 
 * the HTTP response.</p>
 * 
 * <p>Usage example in service/controller layer:
 * <pre>{@code
 *   // Fetch room from repository
 *   Room room = roomRepository.findById(roomId)
 *       .orElseThrow(() -> new RoomNotFoundException(roomId));
 *   
 *   // Fetch membership status for current user
 *   Member member = memberRepository.findByRoomIdAndUserId(roomId, currentUserId)
 *       .orElseThrow(() -> new AccessDeniedException("Not a member of this room"));
 *   
 *   // For DIRECT rooms, fetch friend's UserView from auth service
 *   UserView friendUser = null;
 *   if (room.isDirect()) {
 *       friendUser = authClient.getUserView(room.friendId());
 *   }
 *   
 *   // Build response DTO
 *   GetRoomByIdDTO response = GetRoomByIdDTO.fromRoom(
 *       room,
 *       friendUser,
 *       member.isAdmin(),  // is_admin from membership status
 *       currentUserId      // for is_owner calculation
 *   );
 *   
 *   // Convert relative → absolute URLs for frontend
 *   String mediaBaseUrl = "http://127.0.0.1:8005";
 *   if (response.profileImageUrl() != null) {
 *       response = response.withProfileImageUrl(mediaBaseUrl + response.profileImageUrl());
 *   }
 *   if (response.coverImageUrl() != null) {
 *       response = response.withCoverImageUrl(mediaBaseUrl + response.coverImageUrl());
 *   }
 *   
 *   return ResponseEntity.ok(response);
 * }</pre>
 * </p>
 */
public record GetRoomByIdDTO(

        @JsonProperty("room_id")
        UUID roomId,

        @JsonProperty("name")
        String name,

        @JsonProperty("profile_image_url")
        String profileImageUrl,

        @JsonProperty("cover_image_url")
        String coverImageUrl,

        @JsonProperty("has_profile_image")
        boolean hasProfileImage,

        @JsonProperty("has_cover_image")
        boolean hasCoverImage,

        @JsonProperty("is_group")
        boolean isGroup,

        @JsonProperty("type")
        String type,

        @JsonProperty("description")
        String description,

        @JsonProperty("creator_id")
        UUID creatorId,

        @JsonProperty("is_admin")
        boolean isAdmin,

        @JsonProperty("is_owner")
        boolean isOwner,

        @JsonProperty("last_activity_at")
        String lastActivityAt,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("updated_at")
        String updatedAt,

        @JsonProperty("is_deleted")
        boolean isDeleted

) {

    /**
     * Factory method to create a GetRoomByIdDTO from a Room domain model.
     * 
     * <p>Supports both GROUP and DIRECT room types:
     * <ul>
     *   <li><strong>GROUP rooms:</strong> uses {@code room.groupName()} for name,
     *       {@code room.profileImageUrl()} and {@code room.coverImageUrl()} for images,
     *       {@code room.description()} for description, sets {@code is_group=true}, {@code type="GROUP"}</li>
     *   <li><strong>DIRECT rooms:</strong> uses the friend's {@code UserView.username} for name,
     *       {@code UserView.profilePicture} for profile image, {@code coverImageUrl=null},
     *       {@code description=null}, sets {@code is_group=false}, {@code type="DIRECT"}</li>
     * </ul>
     * 
     * <p>Computes {@code is_owner} by comparing {@code room.creatorId()} with {@code currentUserId}.
     * The {@code isAdmin} flag is passed in from the membership lookup.</p>
     * 
     * <p>Image URLs are stored as relative paths; use {@link #withProfileImageUrl(String)} and 
     * {@link #withCoverImageUrl(String)} to convert to absolute URLs before sending to frontend.</p>
     * 
     * @param room the Room domain object containing room state
     * @param friendUser the UserView of the friend participant (required for DIRECT rooms, null for GROUP)
     * @param isAdmin whether the current user has admin status in this room (from membership lookup)
     * @param currentUserId the UUID of the current user (for is_owner calculation)
     * @return GetRoomByIdDTO ready for API response
     * @throws IllegalArgumentException if friendUser is null for a DIRECT room
     */
    public static GetRoomByIdDTO fromRoom(
            Room room,
            UserView friendUser,
            boolean isAdmin,
            UUID currentUserId
    ) {
        String displayName = null;
        String relativeProfileImageUrl = null;
        String relativeCoverImageUrl = null;
        String descriptionValue = null;
        boolean isGroup = false;
        String typeValue = null;
        
        if (room.type() == Room.Type.GROUP) {
            // GROUP room: use group metadata
            displayName = room.groupName();
            relativeProfileImageUrl = room.profileImageUrl();
            relativeCoverImageUrl = room.coverImageUrl();
            descriptionValue = room.description();
            isGroup = true;
            typeValue = "GROUP";
        } else {
            // DIRECT room: name = friend's username, profile image = friend's profile pic
            if (friendUser == null) {
                throw new IllegalArgumentException(
                        "friendUser is required for DIRECT rooms when creating GetRoomByIdDTO"
                );
            }
            displayName = friendUser.username();
            relativeProfileImageUrl = friendUser.profilePicture();
            relativeCoverImageUrl = null;  // DIRECT rooms have no cover image
            descriptionValue = null;       // DIRECT rooms have no description
            isGroup = false;
            typeValue = "DIRECT";
        }
        
        // Format timestamps as ISO-8601 strings
        String lastActivityAtStr = room.lastActivityAt() != null 
            ? room.lastActivityAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        String createdAtStr = room.createdAt() != null 
            ? room.createdAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        String updatedAtStr = room.updatedAt() != null 
            ? room.updatedAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        
        // Compute is_owner by comparing creatorId with current user
        boolean isOwner = room.creatorId().equals(currentUserId);
        
        return new GetRoomByIdDTO(
                room.id(),
                displayName,
                relativeProfileImageUrl,
                relativeCoverImageUrl,
                relativeProfileImageUrl != null && !relativeProfileImageUrl.isBlank(),
                relativeCoverImageUrl != null && !relativeCoverImageUrl.isBlank(),
                isGroup,
                typeValue,
                descriptionValue,
                room.creatorId(),
                isAdmin,
                isOwner,
                lastActivityAtStr,
                createdAtStr,
                updatedAtStr,
                room.isDeleted()
        );
    }

    /**
     * Convenience factory for testing or mock scenarios.
     * 
     * @param roomId the room UUID
     * @param name the display name (group name or friend username)
     * @param isGroup whether this is a group room
     * @param type the room type string ("GROUP" or "DIRECT")
     * @param profileImageUrl optional profile image URL (relative path)
     * @param coverImageUrl optional cover image URL (relative path, null for DIRECT)
     * @param description optional description (null for DIRECT)
     * @param creatorId the UUID of the room creator
     * @param isAdmin whether current user is admin
     * @param isOwner whether current user is owner
     * @param lastActivityAt the last activity timestamp (ISO-8601 string)
     * @return GetRoomByIdDTO for testing purposes
     */
    public static GetRoomByIdDTO forTesting(
            UUID roomId,
            String name,
            boolean isGroup,
            String type,
            String profileImageUrl,
            String coverImageUrl,
            String description,
            UUID creatorId,
            boolean isAdmin,
            boolean isOwner,
            String lastActivityAt
    ) {
        String now = java.time.Instant.now().toString();
        return new GetRoomByIdDTO(
                roomId,
                name,
                profileImageUrl,
                coverImageUrl,
                profileImageUrl != null && !profileImageUrl.isBlank(),
                coverImageUrl != null && !coverImageUrl.isBlank(),
                isGroup,
                type,
                description,
                creatorId,
                isAdmin,
                isOwner,
                lastActivityAt,
                now,
                now,
                false  // isDeleted
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
     * GetRoomByIdDTO dto = GetRoomByIdDTO.fromRoom(room, friendUser, isAdmin, currentUserId);
     * 
     * // Convert to absolute URL for API response
     * dto = dto.withProfileImageUrl("http://127.0.0.1:8005/uploads/rooms/abc.jpg");
     * 
     * // Response JSON now contains:
     * // "profile_image_url": "http://127.0.0.1:8005/uploads/rooms/abc.jpg",
     * // "has_profile_image": true
     * }</pre>
     * </p>
     * 
     * @param newProfileImageUrl the absolute URL to use, or null to remove/clear image
     * @return new GetRoomByIdDTO instance with updated profileImageUrl and recalculated hasProfileImage
     */
    public GetRoomByIdDTO withProfileImageUrl(String newProfileImageUrl) {
        return new GetRoomByIdDTO(
                this.roomId,
                this.name,
                newProfileImageUrl,
                this.coverImageUrl,
                // Recalculate hasProfileImage based on new value
                newProfileImageUrl != null && !newProfileImageUrl.isBlank(),
                this.hasCoverImage,
                this.isGroup,
                this.type,
                this.description,
                this.creatorId,
                this.isAdmin,
                this.isOwner,
                this.lastActivityAt,
                this.createdAt,
                this.updatedAt,
                this.isDeleted
        );
    }

    /**
     * Create a new DTO instance with an updated coverImageUrl.
     * 
     * <p>Used to convert relative paths (from domain/DB) to absolute URLs
     * (for frontend consumption) without modifying the original immutable record.
     * Note: For DIRECT rooms, this will typically remain null.</p>
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // DTO from domain has: coverImageUrl = "/uploads/rooms/cover/xyz.jpg"
     * GetRoomByIdDTO dto = GetRoomByIdDTO.fromRoom(room, friendUser, isAdmin, currentUserId);
     * 
     * // Convert to absolute URL for API response (GROUP rooms only)
     * dto = dto.withCoverImageUrl("http://127.0.0.1:8005/uploads/rooms/cover/xyz.jpg");
     * 
     * // Response JSON now contains:
     * // "cover_image_url": "http://127.0.0.1:8005/uploads/rooms/cover/xyz.jpg",
     * // "has_cover_image": true
     * }</pre>
     * </p>
     * 
     * @param newCoverImageUrl the absolute URL to use, or null to remove/clear image
     * @return new GetRoomByIdDTO instance with updated coverImageUrl and recalculated hasCoverImage
     */
    public GetRoomByIdDTO withCoverImageUrl(String newCoverImageUrl) {
        return new GetRoomByIdDTO(
                this.roomId,
                this.name,
                this.profileImageUrl,
                newCoverImageUrl,
                this.hasProfileImage,
                // Recalculate hasCoverImage based on new value
                newCoverImageUrl != null && !newCoverImageUrl.isBlank(),
                this.isGroup,
                this.type,
                this.description,
                this.creatorId,
                this.isAdmin,
                this.isOwner,
                this.lastActivityAt,
                this.createdAt,
                this.updatedAt,
                this.isDeleted
        );
    }
}