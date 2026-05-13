in (// chat_service/src/main/java/com/example/chat_service/domain/members/Member.java
package com.example.chat_service.domain.members;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing a Group Member aggregate.
 * Manages membership state, role status, and unread message tracking for a user in a room.
 * 
 * <p>Constructor is public to allow infrastructure mapping from persistence layer.
 * Validation is enforced in constructor, so instantiation is always safe.</p>
 */
public final class Member {

    private final UUID id;           // Group member ID (primary key)
    private final UUID userId;       // Reference to the user
    private final UUID roomId;       // Reference to the room/group
    private final Status status;     // Member role: ADMIN or USER
    private final int unreadMessages; // Count of unread messages
    
    // Metadata
    private final LocalDateTime joinedAt;
    private final LocalDateTime updatedAt;
    private final boolean isLeft;    // Soft delete flag for members who left

    // ── Status Enum ────────────────────────────────────────────────
    public enum Status {
        USER,
        ADMIN
    }

    // ── Constructor with validation ──────────────────────────────────
    /**
     * Public constructor for domain creation and infrastructure mapping.
     * All arguments are validated to ensure domain invariants.
     */
    public Member(UUID id, UUID userId, UUID roomId, Status status, int unreadMessages,
                  LocalDateTime joinedAt, LocalDateTime updatedAt, boolean isLeft) {
        
        // Validate required fields
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        if (roomId == null) throw new IllegalArgumentException("roomId cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (unreadMessages < 0) throw new IllegalArgumentException("unreadMessages cannot be negative");
        if (joinedAt == null) throw new IllegalArgumentException("joinedAt cannot be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");

        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.status = status;
        this.unreadMessages = unreadMessages;
        this.joinedAt = joinedAt;
        this.updatedAt = updatedAt;
        this.isLeft = isLeft;
    }

    // ── Factory Methods ─────────────────────────────────────────────
    
    /**
     * Create a new Member with explicit status.
     * Use this when the initial role matters (e.g., group creator becomes ADMIN).
     */
    public static Member create(UUID id, UUID userId, UUID roomId, Status initialStatus) {
        if (initialStatus == null) {
            throw new IllegalArgumentException("initialStatus cannot be null");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Member(id, userId, roomId, initialStatus, 0, now, now, false);
    }

    /**
     * Create a new Member with USER status (convenience method).
     * Use for inviting regular participants to a room.
     */
    public static Member createAsUser(UUID id, UUID userId, UUID roomId) {
        return create(id, userId, roomId, Status.USER);
    }

    /**
     * Create a new Member with ADMIN status (convenience method).
     * Use for the room creator or when explicitly assigning admin rights at creation.
     */
    public static Member createAsAdmin(UUID id, UUID userId, UUID roomId) {
        return create(id, userId, roomId, Status.ADMIN);
    }

    // ── Getters (no setters - immutable) ───────────────────────────
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID roomId() { return roomId; }
    public Status status() { return status; }
    public int unreadMessages() { return unreadMessages; }
    public LocalDateTime joinedAt() { return joinedAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public boolean isLeft() { return isLeft; }

    // ── State Queries ──────────────────────────────────────────────
    public boolean isActive() {
        return !isLeft;
    }

    public boolean isAdmin() {
        return status == Status.ADMIN;
    }

    public boolean hasUnreadMessages() {
        return unreadMessages > 0;
    }

    // ── Role Management (return new instance) ──────────────────────
    /**
     * Promote this member to ADMIN status.
     * @return new Member instance with updated status and timestamp
     */
    public Member promote() {
        if (isAdmin()) {
            return this; // Already admin, no change needed
        }
        return new Member(this.id, this.userId, this.roomId, Status.ADMIN, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    /**
     * Demote this member to USER status.
     * @return new Member instance with updated status and timestamp
     */
    public Member demote() {
        if (!isAdmin()) {
            return this; // Already user, no change needed
        }
        return new Member(this.id, this.userId, this.roomId, Status.USER, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    // ── Unread Messages Management (return new instance) ───────────
    /**
     * Increment unread messages count by the specified amount.
     * @param amount positive value to add to unread count
     * @return new Member instance with updated unread count and timestamp
     */
    public Member incrementUnreadMessages(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("increment amount cannot be negative");
        }
        int newCount = this.unreadMessages + amount;
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         newCount, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    /**
     * Reset unread messages count to zero.
     * @return new Member instance with zero unread count and updated timestamp
     */
    public Member markAllRead() {
        if (this.unreadMessages == 0) {
            return this; // Already zero, no change needed
        }
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         0, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    // ── Membership State Transformers ──────────────────────────────
    /**
     * Mark this member as having left the room voluntarily.
     * @return new Member instance with isLeft flag set to true
     */
    public Member leave() {
        if (isLeft) {
            return this;
        }
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), true);
    }

    /**
     * Remove this member from the room (admin/system-initiated).
     * Semantically distinct from leave() for audit/event purposes.
     * @return new Member instance with isLeft flag set to true
     */
    public Member remove() {
        if (isLeft) {
            return this;
        }
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), true);
    }

    public Member touch() {
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    // ── Standard Object Methods ────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member member)) return false;
        return Objects.equals(id, member.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", userId=" + userId +
                ", roomId=" + roomId +
                ", status=" + status +
                ", unreadMessages=" + unreadMessages +
                ", isActive=" + isActive() +
                ", joinedAt=" + joinedAt +
                '}';
    }
}) you see i have unreadMessages now if you query the member that field is available so and i added it in (package com.example.chat_service.application.rooms.handlers.dtos;

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
 *   <li>Unread message count for the current user ({@code my_unread_messages_in_room})</li>
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
 *   // Fetch unread counts per room for current user
 *   Map<UUID, Integer> roomIdToUnreadCount = messageQueryRepository.countUnreadByRoomAndUser(
 *       roomData.stream().map(rd -> rd.room().id()).toList(),
 *       userId
 *   );
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
 *           int unreadCount = roomIdToUnreadCount.getOrDefault(room.id(), 0);
 *           
 *           return MyRoomsHomePageListDto.fromRoomWithLastMessage(
 *               room,
 *               lastMsg,
 *               friendUser,
 *               senderUser.username(),
 *               userId,  // current user's ID for is_mine calculation
 *               unreadCount
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
        LastMessagePreview lastMessage,

        @JsonProperty("my_unread_messages_in_room")
        int myUnreadMessagesInRoom

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
     * @param unreadCount the count of unread messages for the current user in this room
     * @return MyRoomsHomePageListDto ready for API response
     * @throws IllegalArgumentException if friendUser is null for a DIRECT room
     */
    public static MyRoomsHomePageListDto fromRoomWithLastMessage(
            Room room,
            com.example.chat_service.domain.messages.Message lastMessage,
            UserView friendUser,
            String lastMessageSenderUsername,
            UUID currentUserId,
            int unreadCount
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
                lastMessagePreview,
                unreadCount
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
     * @param unreadCount the unread message count for testing purposes
     * @return MyRoomsHomePageListDto for testing purposes
     */
    public static MyRoomsHomePageListDto forTesting(
            UUID roomId,
            String name,
            boolean isGroup,
            String profileImageUrl,
            String lastActivityAt,
            LastMessagePreview lastMessage,
            int unreadCount
    ) {
        return new MyRoomsHomePageListDto(
                roomId,
                name,
                profileImageUrl,
                profileImageUrl != null && !profileImageUrl.isBlank(),
                isGroup,
                lastActivityAt,
                false,  // isDeleted
                lastMessage,
                unreadCount
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
                this.lastMessage,  // Keep existing lastMessage unchanged
                this.myUnreadMessagesInRoom
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
                updatedPreview,
                this.myUnreadMessagesInRoom
        );
    }
}) so you can used this function (
    @Override
    public int getUnreadMessageCount(UUID userId, UUID roomId) {
        try {
            Optional<Member> member = memberQueryRepository.findByUserIdAndRoomId(userId, roomId);
            
            if (member.isPresent()) {
                int unreadCount = member.get().unreadMessages();
                logger.debug(
                    "Retrieved unread count: user_id={}, room_id={}, unread={}",
                    userId,
                    roomId,
                    unreadCount
                );
                return unreadCount;
            } else {
                logger.debug(
                    "No active member found for unread count: user_id={}, room_id={}, returning 0",
                    userId,
                    roomId
                );
                return 0;
            }

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving unread count (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving unread count: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }
} from // chat_service/src/main/java/com/example/chat_service/application/members/services/impl/MemberQueryServiceImpl.java

package com.example.chat_service.application.members.services.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.members.services.MemberQueryServiceInterface;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.members.exceptions.MemberDomainError;
import com.example.chat_service.domain.members.exceptions.MemberNotFoundError;
import com.example.chat_service.domain.members.repositories.MemberQueryRepository;

/**
 * Application-layer implementation of {@link MemberQueryServiceInterface}.
 *
 * <p>Orchestrates member query (read) operations by coordinating domain entities
 * with infrastructure query repositories. All methods run within a read-only
 * transaction boundary to optimize database access and ensure consistency.</p>
 *
 * <p><strong>Query pattern:</strong> All query methods accept IDs or filters as parameters,
 * delegate to {@link MemberQueryRepository}, apply read-side business logic if needed,
 * and return domain entities or projections. No state mutations occur.</p>
 *
 * <p><strong>CQRS read-side:</strong> This implementation focuses purely on read operations.
 * All queries automatically exclude members where {@code isLeft = true} unless explicitly
 * documented, ensuring only active participants are returned.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Query methods accept IDs/filters — service delegates to repository, applies read logic</li>
 *   <li>All queries exclude {@code isLeft = true} members by default (active members only)</li>
 *   <li>Read-side projections and DTOs are handled via repository or mapped here</li>
 *   <li>No state mutations — this service is strictly for read operations</li>
 *   <li>All public methods are {@code @Transactional(readOnly = true)} for optimization</li>
 *   <li>Logging at DEBUG level for queries, WARN for not-found scenarios</li>
 * </ul></p>
 */
@Service
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MemberQueryServiceImpl.class);

    private final MemberQueryRepository memberQueryRepository;

    public MemberQueryServiceImpl(MemberQueryRepository memberQueryRepository) {
        this.memberQueryRepository = memberQueryRepository;
    }
) to add the value in the dto, for the requested user. give me full code file for (package com.example.chat_service.application.rooms.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.chat_service.application.members.services.MemberQueryServiceInterface;
import com.example.chat_service.application.messages.services.MessageQueryServiceInterface;
import com.example.chat_service.application.rooms.handlers.dtos.MyRoomsHomePageListDto;
import com.example.chat_service.application.rooms.services.RoomQueryServiceInterface;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.dtos.users.services.UserApiClient;

/**
 * Application-layer orchestrator for room query operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Query user's active memberships via {@link MemberQueryServiceInterface}</li>
 *   <li>Fetch rooms and latest messages via {@link RoomQueryServiceInterface} and {@link MessageQueryServiceInterface}</li>
 *   <li>Enrich room data with external user info via {@link UserApiClient} (for DIRECT rooms and message senders)</li>
 *   <li>Build {@link MyRoomsHomePageListDto} for WhatsApp-style room list display</li>
 *   <li>Apply image-over-text priority and "You" personalization logic in DTO construction</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to domain entities)</li>
 *   <li>Directly access database (delegated to Repositories via Services)</li>
 *   <li>Build absolute URLs — that's handled at the controller layer via MediaUrlService</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler returns DTOs with RELATIVE image paths
 * (as stored in the domain/database). The controller layer is responsible for converting
 * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}.
 * This mirrors the pattern used in {@code RoomCommandHandler} and {@code MessageQueryHandler}.</p>
 *
 * <p><strong>Room type handling:</strong>
 * <ul>
 *   <li><strong>GROUP rooms:</strong> name = {@code room.groupName()}, profile image = {@code room.profileImageUrl()}</li>
 *   <li><strong>DIRECT rooms:</strong> name = OTHER participant's username (not current user), 
 *       profile image = OTHER participant's profile picture (fetched from Auth Service)</li>
 * </ul>
 * </p>
 *
 * <p><strong>Last message preview logic:</strong>
 * <ul>
 *   <li>If message has image: {@code content} is {@code null}, {@code image_url} is populated</li>
 *   <li>If message has no image: {@code content} is populated, {@code image_url} is {@code null}</li>
 *   <li>When requester is the sender: {@code sender_username} is set to "You" for personalized UX</li>
 *   <li><strong>CRITICAL:</strong> {@code is_mine} is calculated by comparing {@code message.senderId()} 
 *       with {@code currentUserId} — only true when they match exactly</li>
 * </ul>
 * </p>
 *
 * <p><strong>Backend invariant:</strong> Only rooms that have at least one active message
 * are included in the returned list. Rooms without messages are filtered out during processing.</p>
 */
@Component
public class RoomQueryHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(RoomQueryHandler.class);

    private final MemberQueryServiceInterface memberQueryService;
    private final RoomQueryServiceInterface roomQueryService;
    private final MessageQueryServiceInterface messageQueryService;
    private final UserApiClient userApiClient;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param memberQueryService handles read operations for Member entities
     * @param roomQueryService handles read operations for Room entities
     * @param messageQueryService handles read operations for Message entities
     * @param userApiClient fetches user data from external Auth Service
     */
    public RoomQueryHandler(
            MemberQueryServiceInterface memberQueryService,
            RoomQueryServiceInterface roomQueryService,
            MessageQueryServiceInterface messageQueryService,
            UserApiClient userApiClient
    ) {
        this.memberQueryService = memberQueryService;
        this.roomQueryService = roomQueryService;
        this.messageQueryService = messageQueryService;
        this.userApiClient = userApiClient;
    }

    // ─────────────────────────────────────────────────────────────────
    // MAIN QUERY: Get rooms for user's home page
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve all rooms a user participates in, enriched for home page display.
     *
     * <p>Flow:
     * <ol>
     *   <li>Fetch user's active memberships via memberQueryService</li>
     *   <li>Extract room IDs from memberships</li>
     *   <li>Fetch active rooms via roomQueryService (bulk lookup)</li>
     *   <li>Fetch latest active messages for each room via messageQueryService (bulk lookup)</li>
     *   <li>For DIRECT rooms: find the OTHER participant (not current user) and fetch their UserView</li>
     *   <li>For each room: resolve display name, profile image, and last message sender username</li>
     *   <li>Build MyRoomsHomePageListDto for each room using reusable helper</li>
     *   <li>Return list of DTOs ready for HTTP response (with relative image paths)</li>
     * </ol>
     * </p>
     *
     * <p><strong>Image URL note:</strong> The returned DTOs contain RELATIVE image paths
     * (e.g., {@code /uploads/rooms/abc.jpg} or {@code /uploads/users/xyz.jpg}). The controller
     * layer should convert these to absolute URLs using
     * {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)} before sending
     * the HTTP response.</p>
     *
     * <p><strong>Performance note:</strong> Uses bulk repository methods
     * ({@code findActiveByIds}, {@code findLatestActiveByRoomIds}) to minimize database round-trips.
     * External Auth Service calls are batched where possible.</p>
     *
     * <p><strong>Backend invariant:</strong> Only rooms with at least one active message
     * are included in the result. Rooms without messages are explicitly filtered out.</p>
     *
     * @param userId the authenticated user requesting their room list
     * @return List of MyRoomsHomePageListDto ready for HTTP response (with relative image paths)
     */
    public List<MyRoomsHomePageListDto> getRoomsForUserHomePage(UUID userId) {
        logger.info("Fetching rooms for user home page: user_id={}", userId);

        // ─────────────────────────────────────────────
        // 1. Fetch user's active memberships
        // ─────────────────────────────────────────────
        List<Member> memberships = memberQueryService.getAllActiveMembershipsByUserId(userId);
        logger.debug(
                "Retrieved {} active memberships for user: user_id={}",
                memberships.size(), userId
        );

        if (memberships.isEmpty()) {
            logger.debug("No active memberships found for user: user_id={}", userId);
            return List.of();
        }

        // ─────────────────────────────────────────────
        // 2. Extract room IDs from memberships
        // ─────────────────────────────────────────────
        List<UUID> roomIds = memberships.stream()
                .map(Member::roomId)
                .distinct()
                .collect(Collectors.toList());

        logger.debug(
                "Extracted {} unique room IDs from memberships: user_id={}",
                roomIds.size(), userId
        );

        // ─────────────────────────────────────────────
        // 3. Fetch active rooms by IDs (bulk lookup)
        // ─────────────────────────────────────────────
        List<Room> rooms = roomQueryService.getActiveRoomsByIds(roomIds);
        logger.debug(
                "Retrieved {} active rooms for {} requested IDs: user_id={}",
                rooms.size(), roomIds.size(), userId
        );

        if (rooms.isEmpty()) {
            logger.debug("No active rooms found for user memberships: user_id={}", userId);
            return List.of();
        }

        // ─────────────────────────────────────────────
        // 4. Fetch latest active messages for each room (bulk lookup)
        // ─────────────────────────────────────────────
        List<UUID> roomIdsForMessages = rooms.stream()
                .map(Room::id)
                .collect(Collectors.toList());
        
        Map<UUID, Message> roomToLatestMessage = messageQueryService
                .getLatestActiveMessagesByRoomIds(roomIdsForMessages);
        
        logger.debug(
                "Retrieved latest messages for {} rooms: user_id={}",
                roomToLatestMessage.size(), userId
        );

        // ─────────────────────────────────────────────
        // 5. For DIRECT rooms: find the OTHER participant (not current user)
        //    and collect their user IDs for fetching UserViews
        // ─────────────────────────────────────────────
        List<UUID> directRoomOtherParticipantIds = new ArrayList<>();
        
        for (Room room : rooms) {
            if (room.type() == Room.Type.DIRECT) {
                // Find the OTHER participant in this DIRECT room (not the current user)
                UUID otherParticipantId = findOtherParticipantInDirectRoom(room.id(), userId);
                if (otherParticipantId != null) {
                    directRoomOtherParticipantIds.add(otherParticipantId);
                }
            }
        }
        
        Map<UUID, UserView> otherParticipantIdToUserView = fetchUserViews(directRoomOtherParticipantIds);
        logger.debug(
                "Fetched {} UserViews for DIRECT room other participants: user_id={}",
                otherParticipantIdToUserView.size(), userId
        );

        // ─────────────────────────────────────────────
        // 6. Collect sender IDs from latest messages (for fetching sender usernames)
        // ─────────────────────────────────────────────
        List<UUID> senderIds = roomToLatestMessage.values().stream()
                .map(Message::senderId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, UserView> senderIdToUserView = fetchUserViews(senderIds);
        logger.debug(
                "Fetched {} UserViews for message senders: user_id={}",
                senderIdToUserView.size(), userId
        );

        // ─────────────────────────────────────────────
        // 7. Build DTOs for each room using reusable helper
        //    NOTE: Rooms without a last message are explicitly excluded
        // ─────────────────────────────────────────────
        List<MyRoomsHomePageListDto> dtos = new ArrayList<>(rooms.size());

        for (Room room : rooms) {
            try {
                Message lastMessage = roomToLatestMessage.get(room.id());
                
                // ─────────────────────────────────────────
                // BACKEND INVARIANT: Skip rooms with no messages
                // Only rooms that have at least one active message are included
                // ─────────────────────────────────────────
                if (lastMessage == null) {
                    logger.debug("Skipping room with no messages (backend invariant): room_id={}", room.id());
                    continue;
                }

                // Resolve the OTHER participant's UserView for DIRECT rooms
                UserView otherParticipantUser = null;
                if (room.type() == Room.Type.DIRECT) {
                    UUID otherParticipantId = findOtherParticipantInDirectRoom(room.id(), userId);
                    if (otherParticipantId != null) {
                        otherParticipantUser = otherParticipantIdToUserView.get(otherParticipantId);
                        if (otherParticipantUser == null) {
                            logger.warn(
                                    "Other participant UserView not found for DIRECT room: room_id={}, other_participant_id={}",
                                    room.id(), otherParticipantId
                            );
                            continue; // Skip if we can't resolve the other participant's display data
                        }
                    } else {
                        logger.warn(
                                "Could not find other participant in DIRECT room: room_id={}, current_user_id={}",
                                room.id(), userId
                        );
                        continue;
                    }
                }

                // Resolve last message sender username
                String senderUsername = resolveLastMessageSenderUsername(lastMessage, senderIdToUserView);

                // Build DTO using reusable helper
                MyRoomsHomePageListDto dto = buildRoomHomePageDto(
                        room,
                        lastMessage,
                        otherParticipantUser,  // For DIRECT rooms: this is the OTHER participant
                        senderUsername,
                        userId  // currentUserId for is_mine calculation
                );

                dtos.add(dto);

            } catch (Exception e) {
                // Log but continue processing other rooms
                // A single room enrichment failure shouldn't break the entire list
                logger.warn(
                        "Failed to build DTO for room: room_id={}, user_id={}, error={}",
                        room.id(), userId, e.getMessage(), e
                );
            }
        }

        logger.info(
                "Successfully built {} room DTOs for user home page: user_id={}",
                dtos.size(), userId
        );

        return dtos;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Find the OTHER participant in a DIRECT room (not current user)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Find the other participant in a DIRECT room by querying active members
     * and filtering out the current user.
     *
     * <p>For DIRECT rooms, there are exactly 2 participants. This method returns
     * the UUID of the participant who is NOT the current user.</p>
     *
     * @param roomId the room to query
     * @param currentUserId the authenticated user requesting the data
     * @return the UUID of the other participant, or null if not found
     */
    private UUID findOtherParticipantInDirectRoom(UUID roomId, UUID currentUserId) {
        try {
            List<Member> members = memberQueryService.getAllActiveMembersByRoomId(roomId);
            
            // Filter out the current user and return the other participant's userId
            return members.stream()
                    .map(Member::userId)
                    .filter(uid -> !uid.equals(currentUserId))
                    .findFirst()
                    .orElse(null);
                    
        } catch (Exception e) {
            logger.warn(
                    "Failed to find other participant in DIRECT room: room_id={}, current_user_id={}, error={}",
                    roomId, currentUserId, e.getMessage()
            );
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // REUSABLE HELPER: Build MyRoomsHomePageListDto from domain objects
    // ─────────────────────────────────────────────────────────────────

    /**
     * Centralized helper to construct a MyRoomsHomePageListDto from a Room,
     * its last message, and enriched user data.
     *
     * <p>This method encapsulates the common pattern of:
     * <ol>
     *   <li>Resolving display name and profile image based on room type (GROUP vs DIRECT)</li>
     *   <li>Building LastMessagePreview with image-over-text priority</li>
     *   <li>Applying "You" personalization when requester is the message sender</li>
     *   <li>Formatting timestamps as ISO-8601 strings</li>
     * </ol>
     * </p>
     *
     * <p>Used by all query methods that return room list data to ensure:
     * <ul>
     *   <li>Consistent enrichment logic across endpoints</li>
     *   <li>Single point of change if DTO structure evolves</li>
     *   <li>Clear separation: handler orchestrates, domain holds state, DTO represents</li>
     * </ul>
     * </p>
     *
     * <p><strong>Note:</strong> The returned DTO contains RELATIVE image paths.
     * Use {@link MyRoomsHomePageListDto#withProfileImageUrl(String)} and
     * {@link MyRoomsHomePageListDto.LastMessagePreview#withImageUrl(String)}
     * to convert to absolute URLs at the controller layer.</p>
     *
     * @param room the Room domain object containing room state
     * @param lastMessage the Message domain object representing the room's last message
     * @param otherParticipantUser the UserView of the OTHER participant in a DIRECT room 
     *        (required for DIRECT rooms, null for GROUP)
     * @param lastMessageSenderUsername the resolved username of the last message's sender
     * @param currentUserId the UUID of the current user (for is_mine calculation in last message)
     * @return MyRoomsHomePageListDto ready for API response (with relative image paths)
     */
    private MyRoomsHomePageListDto buildRoomHomePageDto(
            Room room,
            Message lastMessage,
            UserView otherParticipantUser,
            String lastMessageSenderUsername,
            UUID currentUserId
    ) {
        return MyRoomsHomePageListDto.fromRoomWithLastMessage(
                room,
                lastMessage,
                otherParticipantUser,  // For DIRECT: this is the OTHER participant (not current user)
                lastMessageSenderUsername,
                currentUserId  // Used for is_mine calculation in LastMessagePreview
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Bulk fetch UserViews from external Auth Service
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fetch UserView objects for a collection of user IDs from external Auth Service.
     *
     * <p>Uses batch fetching where possible to minimize external API calls.
     * Falls back to individual fetches if batch endpoint is unavailable.</p>
     *
     * @param userIds collection of user IDs to fetch
     * @return Map of userId -> UserView (may contain fallback entries for failures)
     */
    private Map<UUID, UserView> fetchUserViews(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UserView> result = new HashMap<>(userIds.size());

        for (UUID uid : userIds) {
            try {
                UserView user = userApiClient.getUserById(uid);
                result.put(uid, user);
            } catch (Exception e) {
                logger.warn("Failed to fetch UserView for user_id={}: {}", uid, e.getMessage());
                // Fallback: create minimal UserView with just username
                result.put(uid, new UserView(
                        uid,
                        "user_" + uid.toString().substring(0, 8),
                        null, null, null, null
                ));
            }
        }

        return result;
    }

    /**
     * Fetch a single UserView for a user ID from external Auth Service.
     *
     * <p>Convenience wrapper around {@link #fetchUserViews(Collection)} for single lookups.</p>
     *
     * @param userId the user ID to fetch
     * @return UserView with username, profilePicture, etc. (may be fallback on failure)
     */
    private UserView fetchUserView(UUID userId) {
        return fetchUserViews(List.of(userId)).get(userId);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Resolve last message sender username
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resolve the username for a message's sender from a pre-fetched UserView map.
     *
     * <p>Falls back to a placeholder username if the UserView is not found.</p>
     *
     * @param message the Message domain object
     * @param senderIdToUserView map of sender UUID -> UserView
     * @return the sender's username, or a placeholder if not found
     */
    private String resolveLastMessageSenderUsername(
            Message message,
            Map<UUID, UserView> senderIdToUserView
    ) {
        if (message == null || message.senderId() == null) {
            return "Unknown";
        }

        UserView sender = senderIdToUserView.get(message.senderId());
        return (sender != null && sender.username() != null) 
                ? sender.username() 
                : "user_" + message.senderId().toString().substring(0, 8);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER: Resolve display name and profile image for a room
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resolve the display name and profile image URL for a room based on its type.
     *
     * <p>For GROUP rooms: uses room's groupName and profileImageUrl.
     * For DIRECT rooms: uses OTHER participant's username and profilePicture from UserView.</p>
     *
     * <p><strong>Note:</strong> Returns RELATIVE image paths as stored in domain/DB.
     * Convert to absolute URLs at the controller layer.</p>
     *
     * @param room the Room domain object
     * @param otherParticipantUser the UserView of the OTHER participant (required for DIRECT rooms, null for GROUP)
     * @return DisplayNameAndImage record containing resolved name and relative image URL
     * @throws IllegalArgumentException if otherParticipantUser is null for a DIRECT room
     */
    private DisplayNameAndImage resolveDisplayNameAndImage(Room room, UserView otherParticipantUser) {
        if (room.type() == Room.Type.GROUP) {
            return new DisplayNameAndImage(
                    room.groupName(),
                    room.profileImageUrl()
            );
        } else {
            // DIRECT room: use the OTHER participant's data (not current user)
            if (otherParticipantUser == null) {
                throw new IllegalArgumentException(
                        "otherParticipantUser is required for DIRECT rooms when resolving display data"
                );
            }
            return new DisplayNameAndImage(
                    otherParticipantUser.username(),
                    otherParticipantUser.profilePicture()
            );
        }
    }

    /**
     * Simple record to hold resolved display name and image URL for a room.
     *
     * @param displayName the name to display for the room
     * @param relativeImageUrl the RELATIVE image URL path (to be converted to absolute at controller)
     */
    private record DisplayNameAndImage(String displayName, String relativeImageUrl) {}
}) no any other change requred because everything else is 100% correct 