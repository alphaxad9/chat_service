package com.example.chat_service.application.rooms.handlers;

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
 *   <li>Include unread message count for the current user in each room</li>
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
     *   <li>For each room: resolve display name, profile image, last message sender username, and unread count</li>
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

                // Fetch unread message count for current user in this room
                int unreadCount = memberQueryService.getUnreadMessageCount(userId, room.id());
                logger.debug(
                        "Unread count for user_id={} in room_id={}: {}",
                        userId, room.id(), unreadCount
                );

                // Build DTO using reusable helper (now includes unreadCount)
                MyRoomsHomePageListDto dto = buildRoomHomePageDto(
                        room,
                        lastMessage,
                        otherParticipantUser,  // For DIRECT rooms: this is the OTHER participant
                        senderUsername,
                        userId,                // currentUserId for is_mine calculation
                        unreadCount            // unread count for current user in this room
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
     *   <li>Fetching unread message count for current user in this room</li>
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
     * @param unreadCount the count of unread messages for the current user in this room
     * @return MyRoomsHomePageListDto ready for API response (with relative image paths)
     */
    private MyRoomsHomePageListDto buildRoomHomePageDto(
            Room room,
            Message lastMessage,
            UserView otherParticipantUser,
            String lastMessageSenderUsername,
            UUID currentUserId,
            int unreadCount
    ) {
        return MyRoomsHomePageListDto.fromRoomWithLastMessage(
                room,
                lastMessage,
                otherParticipantUser,  // For DIRECT: this is the OTHER participant (not current user)
                lastMessageSenderUsername,
                currentUserId,         // Used for is_mine calculation in LastMessagePreview
                unreadCount            // Unread count for current user in this room
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
}