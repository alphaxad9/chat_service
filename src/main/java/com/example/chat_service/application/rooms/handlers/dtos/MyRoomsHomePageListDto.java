package com.example.chat_service.application.rooms.handlers.dtos;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.external.users.dtos.UserView;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO representing a room item in the user's home page room list.
 * 
 * <p>Designed for efficient display of rooms in a scrollable list view (WhatsApp-style).
 * Contains only the essential fields needed for rendering room previews:
 * <ul>
 *   <li>Room identity and type (id, is_group)</li>
 *   <li>Display name (group name for GROUP rooms, friend username for DIRECT rooms)</li>
 *   <li>Profile/cover image URL for visual identification</li>
 *   <li>Last message preview with content/image, timestamp, sender, and status</li>
 *   <li>Context flags (has_profile_image) for conditional UI rendering</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>For GROUP rooms: {@code name=groupName}, {@code profileImageUrl} from room metadata</li>
 *   <li>For DIRECT rooms: {@code name=friendUsername}, {@code profileImageUrl=friendProfilePicture}</li>
 *   <li><strong>Backend invariant:</strong> Only rooms with at least one message are included in this list</li>
 *   <li>Last message preview follows "image-over-text" priority: if message has image, {@code content} is {@code null} and {@code image_url} is populated</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output</li>
 *   <li>Immutable record pattern ensures thread-safety and predictable state</li>
 * </ul>
 * </p>
 * 
 * <p><strong>profileImageUrl field note:</strong> Initially contains RELATIVE path
 * from domain/DB (e.g. {@code /uploads/rooms/abc.jpg} or {@code /uploads/users/xyz.jpg}).
 * The controller converts this to an absolute URL using {@link #withProfileImageUrl(String)}
 * before sending the HTTP response.</p>
 * 
 * <p><strong>lastMessage.imageUrl field note:</strong> Also contains RELATIVE path.
 * Use {@link LastMessagePreview#withImageUrl(String)} to convert to absolute URL.</p>
 * 
 * <p>Usage example in service layer:
 * <pre>{@code
 *   // Fetch rooms with their last message from repository
 *   List<RoomWithLastMessage> roomData = roomQueryRepository.findRoomsWithLastMessageByUserId(userId);
 *   
 *   // For DIRECT rooms, fetch friend's UserView from auth service
 *   Map<UUID, UserView> friendIdToUser = authClient.getUsers(
 *       roomData.stream()
 *           .filter(rd -> rd.room().isDirect())
 *           .map(rd -> rd.room().friendId())
 *           .toList()
 *   );
 *   
 *   // Transform to DTOs
 *   List<MyRoomsHomePageListDto> dtos = roomData.stream()
 *       .map(rd -> {
 *           Room room = rd.room();
 *           Message lastMsg = rd.lastMessage();
 *           UserView friendUser = room.isDirect() ? friendIdToUser.get(room.friendId()) : null;
 *           UserView senderUser = authClient.getUserView(lastMsg.senderId());
 *           
 *           return MyRoomsHomePageListDto.fromRoomWithLastMessage(
 *               room,
 *               lastMsg,
 *               friendUser,
 *               senderUser.username(),
 *               userId  // current user's ID for is_mine calculation
 *           );
 *       })
 *       .toList();
 *   
 *   // Convert relative → absolute URLs for frontend
 *   String mediaBaseUrl = "http://127.0.0.1:8005";
 *   dtos = dtos.stream()
 *       .map(dto -> {
 *           dto = dto.withProfileImageUrl(
 *               dto.profileImageUrl() != null ? mediaBaseUrl + dto.profileImageUrl() : null
 *           );
 *           if (dto.lastMessage().imageUrl() != null) {
 *               dto = dto.withLastMessageImageUrl(mediaBaseUrl + dto.lastMessage().imageUrl());
 *           }
 *           return dto;
 *       })
 *       .toList();
 *   
 *   return ResponseEntity.ok(dtos);
 * }</pre>
 * </p>
 */
public record MyRoomsHomePageListDto(

        @JsonProperty("room_id")
        UUID roomId,

        @JsonProperty("name")
        String name,

        @JsonProperty("profile_image_url")
        String profileImageUrl,

        @JsonProperty("has_profile_image")
        boolean hasProfileImage,

        @JsonProperty("is_group")
        boolean isGroup,

        @JsonProperty("last_activity_at")
        String lastActivityAt,

        @JsonProperty("is_deleted")
        boolean isDeleted,

        @JsonProperty("last_message")
        LastMessagePreview lastMessage

) {

    /**
     * Minimal preview representation of the last message in a room.
     * 
     * <p>Follows "image-over-text" priority for preview display:
     * <ul>
     *   <li>If message has image: {@code image_url} is set, {@code content} is {@code null}</li>
     *   <li>If message has no image: {@code content} is set, {@code image_url} is {@code null}</li>
     * </ul>
     * 
     * <p><strong>Personalization:</strong> When {@code is_mine=true}, {@code sender_username}
     * is set to "You" for consistent UX across the application.</p>
     * 
     * <p><strong>URL handling:</strong> {@code image_url} contains RELATIVE path from domain/DB.
     * Use {@link #withImageUrl(String)} to convert to absolute URL before HTTP response.</p>
     */
    public record LastMessagePreview(

            @JsonProperty("id")
            UUID id,

            @JsonProperty("room_id")
            UUID roomId,

            @JsonProperty("content")
            String content,

            @JsonProperty("image_url")
            String imageUrl,

            @JsonProperty("created_at")
            String createdAt,

            @JsonProperty("is_mine")
            boolean isMine,

            @JsonProperty("status")
            String status,

            @JsonProperty("sender_username")
            String senderUsername,

            @JsonProperty("has_image")
            boolean hasImage

    ) {

        /**
         * Factory method to create LastMessagePreview from a Message domain object.
         * 
         * <p>Applies image-over-text priority logic and personalizes sender username.
         * Timestamps are formatted as ISO-8601 strings for API consistency.</p>
         * 
         * @param message the Message domain object containing last message state
         * @param senderUsername the resolved username of the message sender
         * @param requesterId the UUID of the current user (for is_mine calculation)
         * @return LastMessagePreview ready for embedding in room list DTO
         */
        public static LastMessagePreview fromMessage(
                com.example.chat_service.domain.messages.Message message,
                String senderUsername,
                UUID requesterId
        ) {
            boolean messageHasImage = message.hasImage();
            boolean isMine = message.senderId().equals(requesterId);
            
            // Format timestamp as ISO-8601 string
            String createdAtStr = message.createdAt() != null 
                ? message.createdAt().format(DateTimeFormatter.ISO_DATE_TIME) 
                : null;
            
            // Personalize sender username: show "You" when the requester is the sender
            String displaySenderUsername = isMine ? "You" : (senderUsername != null ? senderUsername : "");
            
            return new LastMessagePreview(
                    message.id(),
                    message.roomId(),
                    // Image-over-text priority: content is null if message has image
                    messageHasImage ? null : message.content(),
                    // Image-over-text priority: image_url is null if message has no image
                    messageHasImage ? message.imageUrl() : null,
                    createdAtStr,
                    isMine,
                    message.status().name(),  // "SENT", "RECEIVED", or "SEEN"
                    displaySenderUsername,
                    messageHasImage
            );
        }

        /**
         * Create a new LastMessagePreview instance with an updated imageUrl.
         * 
         * <p>Used to convert relative paths (from domain/DB) to absolute URLs
         * (for frontend consumption) without modifying the original immutable record.</p>
         * 
         * <p><strong>Example:</strong>
         * <pre>{@code
         * // DTO from domain has: imageUrl = "/uploads/messages/abc.jpg"
         * LastMessagePreview preview = LastMessagePreview.fromMessage(message, username, userId);
         * 
         * // Convert to absolute URL for API response
         * preview = preview.withImageUrl("http://127.0.0.1:8005/uploads/messages/abc.jpg");
         * 
         * // Response JSON now contains:
         * // "image_url": "http://127.0.0.1:8005/uploads/messages/abc.jpg",
         * // "has_image": true
         * }</pre>
         * </p>
         * 
         * @param newImageUrl the absolute URL to use, or null/blank to clear
         * @return new LastMessagePreview instance with updated imageUrl and recalculated hasImage
         */
        public LastMessagePreview withImageUrl(String newImageUrl) {
            return new LastMessagePreview(
                    this.id,
                    this.roomId,
                    this.content,  // Keep existing content (already follows image-over-text logic)
                    newImageUrl,
                    this.createdAt,
                    this.isMine,
                    this.status,
                    this.senderUsername,  // Keep existing senderUsername (already personalized)
                    // Recalculate hasImage based on new value
                    newImageUrl != null && !newImageUrl.isBlank()
            );
        }
    }

    /**
     * Factory method to create a MyRoomsHomePageListDto from a Room domain model
     * with its associated last message.
     * 
     * <p>Supports both GROUP and DIRECT room types:
     * <ul>
     *   <li><strong>GROUP rooms:</strong> uses {@code room.groupName()} for name,
     *       {@code room.profileImageUrl()} for profile image, sets {@code is_group=true}</li>
     *   <li><strong>DIRECT rooms:</strong> uses the friend's {@code UserView.username} for name,
     *       {@code UserView.profilePicture} for profile image, sets {@code is_group=false}</li>
     * </ul>
     * 
     * <p>The profileImageUrl and lastMessage.imageUrl are stored as relative paths;
     * use {@link #withProfileImageUrl(String)} and 
     * {@link LastMessagePreview#withImageUrl(String)} to convert to absolute URLs.</p>
     * 
     * @param room the Room domain object containing room state
     * @param lastMessage the Message domain object representing the room's last message
     * @param friendUser the UserView of the friend participant (required for DIRECT rooms, null for GROUP)
     * @param lastMessageSenderUsername the resolved username of the last message's sender
     * @param currentUserId the UUID of the current user (for is_mine calculation in last message)
     * @return MyRoomsHomePageListDto ready for API response
     * @throws IllegalArgumentException if friendUser is null for a DIRECT room
     */
    public static MyRoomsHomePageListDto fromRoomWithLastMessage(
            Room room,
            com.example.chat_service.domain.messages.Message lastMessage,
            UserView friendUser,
            String lastMessageSenderUsername,
            UUID currentUserId
    ) {
        String displayName = null;
        String relativeImageUrl = null;
        boolean isGroup = false;
        
        if (room.type() == Room.Type.GROUP) {
            // GROUP room: use group name and room's profile image
            displayName = room.groupName();
            relativeImageUrl = room.profileImageUrl();
            isGroup = true;
        } else {
            // DIRECT room: name = friend's username, profile image = friend's profile pic
            if (friendUser == null) {
                throw new IllegalArgumentException(
                        "friendUser is required for DIRECT rooms when creating MyRoomsHomePageListDto"
                );
            }
            displayName = friendUser.username();
            relativeImageUrl = friendUser.profilePicture();
            isGroup = false;
        }
        
        // Format last activity timestamp as ISO-8601 string
        String lastActivityAtStr = room.lastActivityAt() != null 
            ? room.lastActivityAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        
        // Build last message preview
        LastMessagePreview lastMessagePreview = LastMessagePreview.fromMessage(
                lastMessage,
                lastMessageSenderUsername,
                currentUserId
        );
        
        return new MyRoomsHomePageListDto(
                room.id(),
                displayName,
                relativeImageUrl,
                relativeImageUrl != null && !relativeImageUrl.isBlank(),
                isGroup,
                lastActivityAtStr,
                room.isDeleted(),
                lastMessagePreview
        );
    }

    /**
     * Convenience factory for testing or mock scenarios.
     * 
     * @param roomId the room UUID
     * @param name the display name (group name or friend username)
     * @param isGroup whether this is a group room
     * @param profileImageUrl optional profile image URL (relative path)
     * @param lastActivityAt the last activity timestamp (ISO-8601 string)
     * @param lastMessage the LastMessagePreview for this room
     * @return MyRoomsHomePageListDto for testing purposes
     */
    public static MyRoomsHomePageListDto forTesting(
            UUID roomId,
            String name,
            boolean isGroup,
            String profileImageUrl,
            String lastActivityAt,
            LastMessagePreview lastMessage
    ) {
        return new MyRoomsHomePageListDto(
                roomId,
                name,
                profileImageUrl,
                profileImageUrl != null && !profileImageUrl.isBlank(),
                isGroup,
                lastActivityAt,
                false,  // isDeleted
                lastMessage
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
     * MyRoomsHomePageListDto dto = MyRoomsHomePageListDto.fromRoomWithLastMessage(...);
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
     * @return new MyRoomsHomePageListDto instance with updated profileImageUrl and recalculated hasProfileImage
     */
    public MyRoomsHomePageListDto withProfileImageUrl(String newProfileImageUrl) {
        return new MyRoomsHomePageListDto(
                this.roomId,
                this.name,
                newProfileImageUrl,
                // Recalculate hasProfileImage based on new value
                newProfileImageUrl != null && !newProfileImageUrl.isBlank(),
                this.isGroup,
                this.lastActivityAt,
                this.isDeleted,
                this.lastMessage  // Keep existing lastMessage unchanged
        );
    }

    /**
     * Create a new DTO instance with an updated last message image URL.
     * 
     * <p>Used to convert the last message's relative image path to an absolute URL
     * for frontend consumption without modifying the original immutable record.</p>
     * 
     * @param newLastMessageImageUrl the absolute URL for the last message image, or null to clear
     * @return new MyRoomsHomePageListDto instance with updated lastMessage.imageUrl
     */
    public MyRoomsHomePageListDto withLastMessageImageUrl(String newLastMessageImageUrl) {
        if (this.lastMessage == null) {
            return this; // No last message to update
        }
        LastMessagePreview updatedPreview = this.lastMessage.withImageUrl(newLastMessageImageUrl);
        return new MyRoomsHomePageListDto(
                this.roomId,
                this.name,
                this.profileImageUrl,
                this.hasProfileImage,
                this.isGroup,
                this.lastActivityAt,
                this.isDeleted,
                updatedPreview
        );
    }
}