package com.example.chat_service.application.rooms.handlers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.chat_service.application.members.services.MemberQueryServiceInterface;
import com.example.chat_service.application.messages.services.MessageCommandServiceInterface;
import com.example.chat_service.application.messages.services.MessageQueryServiceInterface;
import com.example.chat_service.application.rooms.handlers.dtos.GetRoomByIdDTO;
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
 *   <li>Build {@link GetRoomByIdDTO} for detailed room view (settings, member management)</li>
 *   <li>Provide list of users for "start new conversation" UI (including friends from empty rooms)</li>
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
 * <p><strong>Backend invariant:</strong> 
 * <ul>
 *   <li><strong>DIRECT rooms:</strong> Only rooms with at least one active message are included in home page list</li>
 *   <li><strong>GROUP rooms:</strong> Included even if empty (no messages yet) so users can start conversations</li>
 * </ul>
 * </p>
 */
@Component
public class RoomQueryHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(RoomQueryHandler.class);

    private final MemberQueryServiceInterface memberQueryService;
    private final RoomQueryServiceInterface roomQueryService;
    private final MessageQueryServiceInterface messageQueryService;
    private final MessageCommandServiceInterface messageCommandService;
    private final UserApiClient userApiClient;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param memberQueryService handles read operations for Member entities
     * @param roomQueryService handles read operations for Room entities
     * @param messageQueryService handles read operations for Message entities
     * @param messageCommandService handles write operations for Message entities (e.g., markAsReceived)
     * @param userApiClient fetches user data from external Auth Service
     */
    public RoomQueryHandler(
            MemberQueryServiceInterface memberQueryService,
            RoomQueryServiceInterface roomQueryService,
            MessageQueryServiceInterface messageQueryService,
            MessageCommandServiceInterface messageCommandService,
            UserApiClient userApiClient
    ) {
        this.memberQueryService = memberQueryService;
        this.roomQueryService = roomQueryService;
        this.messageQueryService = messageQueryService;
        this.messageCommandService = messageCommandService;
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
     *   <li><strong>For each room: mark all messages as RECEIVED via bulkMarkAsReceivedInRoom</strong></li>
     *   <li><strong>For each room with a latest message: also call markAsReceived on that specific message</strong></li>
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
     * <p><strong>Backend invariant:</strong> 
     * <ul>
     *   <li><strong>DIRECT rooms:</strong> Only rooms with at least one active message are included</li>
     *   <li><strong>GROUP rooms:</strong> Included even if empty (no messages) so users can start conversations</li>
     * </ul>
     * </p>
     *
     * <p><strong>Read receipt note:</strong> This method automatically marks all messages as RECEIVED
     * for the current user before returning rooms. Both bulk operation ({@code bulkMarkAsReceivedInRoom})
     * and individual message operation ({@code markAsReceived} for the latest message) are performed.
     * If any read receipt operation fails, the error is logged but the room query continues (fail-soft).</p>
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
        // 5. MARK ALL MESSAGES AS RECEIVED (Read Receipt Automation)
        //    - For each room: call bulkMarkAsReceivedInRoom to mark all active messages
        //    - For each room with a latest message: also call markAsReceived on that specific message
        //    - Fail-soft: log errors but continue with room query
        // ─────────────────────────────────────────────
        for (Room room : rooms) {
            try {
                // Bulk mark all active messages in room as RECEIVED
                int markedCount = messageCommandService.bulkMarkAsReceivedInRoom(room.id(), userId);
                logger.trace(
                        "Bulk marked {} messages as RECEIVED in room: room_id={}, user_id={}",
                        markedCount, room.id(), userId
                );

                // Also explicitly mark the latest/top message as RECEIVED (double-check)
                Message latestMessage = roomToLatestMessage.get(room.id());
                if (latestMessage != null) {
                    try {
                        messageCommandService.markAsReceived(latestMessage.id(), userId);
                        logger.trace(
                                "Explicitly marked latest message as RECEIVED: message_id={}, room_id={}, user_id={}",
                                latestMessage.id(), room.id(), userId
                        );
                    } catch (Exception e) {
                        // Log but continue - bulk operation already handled most cases
                        logger.trace(
                                "Failed to mark latest message individually (non-blocking): message_id={}, room_id={}, user_id={}, error={}",
                                latestMessage.id(), room.id(), userId, e.getMessage()
                        );
                    }
                }

            } catch (Exception e) {
                // Log but continue - room query should not fail if read receipt fails
                logger.warn(
                        "Failed to mark messages as received in room (non-blocking): room_id={}, user_id={}, error={}",
                        room.id(), userId, e.getMessage()
                );
            }
        }

        // ─────────────────────────────────────────────
        // 6. For DIRECT rooms: find the OTHER participant (not current user)
        //    and collect their user IDs for fetching UserViews
        // ─────────────────────────────────────────────
        List<UUID> directRoomOtherParticipantIds = new ArrayList<>();
        
        for (Room room : rooms) {
            if (room.type() == Room.Type.DIRECT) {
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
        // 7. Collect sender IDs from latest messages (for fetching sender usernames)
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
        // 8. Build DTOs for each room using reusable helper
        // ─────────────────────────────────────────────
        List<MyRoomsHomePageListDto> dtos = new ArrayList<>(rooms.size());

        for (Room room : rooms) {
            try {
                Message lastMessage = roomToLatestMessage.get(room.id());
                
                // Skip empty DIRECT rooms; include empty GROUP rooms
                // if (lastMessage == null && room.type() == Room.Type.DIRECT) {
                //     logger.debug("Skipping empty DIRECT room (appears in conversation starters): room_id={}", room.id());
                //     continue;
                // }
                
                // if (lastMessage == null && room.type() == Room.Type.GROUP) {
                //     logger.debug("Including empty GROUP room on home page: room_id={}", room.id());
                // }

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
                            continue;
                        }
                    } else {
                        logger.warn(
                                "Could not find other participant in DIRECT room: room_id={}, current_user_id={}",
                                room.id(), userId
                        );
                        continue;
                    }
                }

                String senderUsername = (lastMessage != null) 
                        ? resolveLastMessageSenderUsername(lastMessage, senderIdToUserView) 
                        : null;

                int unreadCount = memberQueryService.getUnreadMessageCount(userId, room.id());
                logger.debug(
                        "Unread count for user_id={} in room_id={}: {}",
                        userId, room.id(), unreadCount
                );

                MyRoomsHomePageListDto dto = buildRoomHomePageDto(
                        room,
                        lastMessage,
                        otherParticipantUser,
                        senderUsername,
                        userId,
                        unreadCount
                );

                dtos.add(dto);

            } catch (Exception e) {
                logger.warn(
                        "Failed to build DTO for room: room_id={}, user_id={}, error={}",
                        room.id(), userId, e.getMessage(), e
                );
            }
        }

        // ─────────────────────────────────────────────
        // 9. SORT: Order rooms by last_activity_at descending (most recent first)
        //    - Rooms with null last_activity_at go to the end
        //    - Uses stable sort to preserve relative order for equal timestamps
        // ─────────────────────────────────────────────
        dtos.sort((dto1, dto2) -> {
            String ts1 = dto1.lastActivityAt();
            String ts2 = dto2.lastActivityAt();
            
            // Handle nulls: put them at the end
            if (ts1 == null && ts2 == null) return 0;
            if (ts1 == null) return 1;   // dto1 goes after dto2
            if (ts2 == null) return -1;  // dto1 goes before dto2
            
            // Parse ISO-8601 strings and compare (descending order)
            try {
                LocalDateTime dt1 = LocalDateTime.parse(ts1);
                LocalDateTime dt2 = LocalDateTime.parse(ts2);
                return dt2.compareTo(dt1);  // descending: newer first
            } catch (Exception e) {
                // Fallback: string comparison (lexicographic works for ISO-8601)
                return ts2.compareTo(ts1);
            }
        });

        logger.info(
                "Successfully built and sorted {} room DTOs for user home page: user_id={}",
                dtos.size(), userId
        );

        return dtos;
    }

    // ─────────────────────────────────────────────────────────────────
    // NEW: Get users for "start new conversation" UI (includes friends from empty rooms)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve a list of users available to start a new conversation with.
     *
     * <p>This endpoint combines two data sources:
     * <ol>
     *   <li>Users from the external Auth Service (via {@code getListUsers})</li>
     *   <li>Friends from the user's empty DIRECT rooms (rooms with no messages yet)</li>
     * </ol>
     * </p>
     *
     * <p><strong>Why include friends from empty rooms?</strong>
     * <ul>
     *   <li>Home page ({@code /api/query/rooms/home}) only shows DIRECT rooms WITH messages</li>
     *   <li>But users may have created DIRECT rooms that have no messages yet (empty conversations)</li>
     *   <li>These "empty rooms" should still appear in the "start conversation" UI so users can resume chatting</li>
     *   <li>We fetch the OTHER participant from each empty DIRECT room and include them in the results</li>
     * </ul>
     * </p>
     *
     * <p><strong>Deduplication & Filtering:</strong>
     * <ul>
     *   <li>Excludes the current user ({@code userId}) from results</li>
     *   <li>Removes duplicate UserViews (by {@code userId}) using a {@code Set}</li>
     *   <li>Preserves order: Auth Service users first, then empty-room friends appended</li>
     * </ul>
     * </p>
     *
     * <p><strong>Performance note:</strong> Uses bulk fetching for UserViews where possible.
     * Empty room friend IDs are fetched in a single query per room, then batch-fetched from Auth Service.</p>
     *
     * @param userId the authenticated user requesting the conversation starter list
     * @param limit maximum number of users to return from Auth Service (passed through to {@code getListUsers})
     * @param offset offset for pagination from Auth Service (passed through to {@code getListUsers})
     * @param includeDeleted whether to include deleted users from Auth Service (passed through to {@code getListUsers})
     * @return List of UserView ready for "start conversation" UI display
     */
    public List<UserView> getUsersForNewConversation(
            UUID userId,
            int limit,
            int offset,
            boolean includeDeleted
    ) {
        logger.info(
                "Fetching users for new conversation: user_id={}, limit={}, offset={}, include_deleted={}",
                userId, limit, offset, includeDeleted
        );

        // ─────────────────────────────────────────────
        // 1. Fetch users from external Auth Service
        // ─────────────────────────────────────────────
        List<UserView> authServiceUsers = userApiClient.getListUsers(limit, offset, includeDeleted);
        logger.debug(
                "Retrieved {} users from Auth Service for conversation starters",
                authServiceUsers.size()
        );

        // ─────────────────────────────────────────────
        // 2. Find user's empty DIRECT rooms (rooms with no messages)
        // ─────────────────────────────────────────────
        List<Member> memberships = memberQueryService.getAllActiveMembershipsByUserId(userId);
        List<UUID> membershipRoomIds = memberships.stream()
                .map(Member::roomId)
                .distinct()
                .collect(Collectors.toList());

        if (membershipRoomIds.isEmpty()) {
            logger.debug("No memberships found for user: user_id={}", userId);
            // Return only Auth Service users, filtered
            return filterAndDeduplicateUsers(authServiceUsers, userId);
        }

        // Fetch active rooms for these memberships
        List<Room> userRooms = roomQueryService.getActiveRoomsByIds(membershipRoomIds);
        logger.debug(
                "Retrieved {} active rooms for user memberships: user_id={}",
                userRooms.size(), userId
        );

        // ─────────────────────────────────────────────
        // 3. Filter to DIRECT rooms with NO messages (empty rooms)
        // ─────────────────────────────────────────────
        List<UUID> emptyDirectRoomIds = new ArrayList<>();
        for (Room room : userRooms) {
            if (room.type() == Room.Type.DIRECT) {
                // Check if this room has any messages
                Optional<Message> latestMsg = messageQueryService.getLatestActiveMessageByRoomId(room.id());
                if (latestMsg.isEmpty()) {
                    // This is an empty DIRECT room - include it
                    emptyDirectRoomIds.add(room.id());
                    logger.debug(
                            "Found empty DIRECT room for conversation starters: room_id={}, user_id={}",
                            room.id(), userId
                    );
                }
            }
        }

        if (emptyDirectRoomIds.isEmpty()) {
            logger.debug("No empty DIRECT rooms found for user: user_id={}", userId);
            // Return only Auth Service users, filtered
            return filterAndDeduplicateUsers(authServiceUsers, userId);
        }

        // ─────────────────────────────────────────────
        // 4. Find the OTHER participant in each empty DIRECT room
        // ─────────────────────────────────────────────
        List<UUID> emptyRoomFriendIds = new ArrayList<>();
        for (UUID roomId : emptyDirectRoomIds) {
            UUID otherParticipantId = findOtherParticipantInDirectRoom(roomId, userId);
            if (otherParticipantId != null && !otherParticipantId.equals(userId)) {
                emptyRoomFriendIds.add(otherParticipantId);
            }
        }

        if (emptyRoomFriendIds.isEmpty()) {
            logger.debug("No other participants found in empty rooms for user: user_id={}", userId);
            return filterAndDeduplicateUsers(authServiceUsers, userId);
        }

        logger.debug(
                "Found {} friends from empty DIRECT rooms: user_id={}",
                emptyRoomFriendIds.size(), userId
        );

        // ─────────────────────────────────────────────
        // 5. Fetch UserViews for empty-room friends (batch fetch)
        // ─────────────────────────────────────────────
        Map<UUID, UserView> emptyRoomFriendViews = fetchUserViews(emptyRoomFriendIds);
        List<UserView> emptyRoomFriends = new ArrayList<>(emptyRoomFriendViews.values());
        logger.debug(
                "Fetched {} UserViews for empty-room friends: user_id={}",
                emptyRoomFriends.size(), userId
        );

        // ─────────────────────────────────────────────
        // 6. Combine Auth Service users + empty-room friends, deduplicate, exclude self
        // ─────────────────────────────────────────────
        List<UserView> combinedUsers = new ArrayList<>(authServiceUsers);
        combinedUsers.addAll(emptyRoomFriends);

        List<UserView> result = filterAndDeduplicateUsers(combinedUsers, userId);

        logger.info(
                "Successfully built conversation starter list: total_users={}, auth_service={}, empty_room_friends={}, user_id={}",
                result.size(), authServiceUsers.size(), emptyRoomFriends.size(), userId
        );

        return result;
    }

    /**
     * Helper to filter out the current user and remove duplicates from a list of UserViews.
     *
     * @param users the list of UserViews to filter
     * @param currentUserId the ID of the current user to exclude
     * @return filtered and deduplicated list of UserViews
     */
    private List<UserView> filterAndDeduplicateUsers(List<UserView> users, UUID currentUserId) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        // Use a Set to track seen userIds for deduplication
        Set<UUID> seenUserIds = new HashSet<>();
        List<UserView> result = new ArrayList<>();

        for (UserView user : users) {
            if (user == null || user.userId() == null) {
                continue; // Skip invalid entries
            }

            // Exclude current user and duplicates
            if (!user.userId().equals(currentUserId) && seenUserIds.add(user.userId())) {
                result.add(user);
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // NEW: Get users available to add to a group room
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve a list of users available to add to a group room.
     *
     * <p>This endpoint combines two data sources:
     * <ol>
     *   <li>Users from the external Auth Service (via {@code getListUsers})</li>
     *   <li>Filters out users who are already members of the specified room</li>
     *   <li>Excludes the requester (current user) since they are already the admin</li>
     * </ol>
     * </p>
     *
     * <p><strong>Deduplication & Filtering:</strong>
     * <ul>
     *   <li>Excludes the requester ({@code requesterId}) from results</li>
     *   <li>Removes users who are already active members of the room</li>
     *   <li>Removes duplicate UserViews (by {@code userId}) using a {@code Set}</li>
     * </ul>
     * </p>
     *
     * <p><strong>Performance note:</strong> Uses bulk fetching for UserViews where possible.
     * Member IDs are fetched in a single query, then filtered in-memory.</p>
     *
     * @param roomId the UUID of the group room to add users to
     * @param requesterId the authenticated user requesting the list (group admin)
     * @param limit maximum number of users to return from Auth Service
     * @param offset offset for pagination from Auth Service
     * @param includeDeleted whether to include deleted users from Auth Service
     * @return List of UserView ready for "add to group" UI display, excluding existing members and requester
     */
    public List<UserView> getUsersToAddInGroup(
            UUID roomId,
            UUID requesterId,
            int limit,
            int offset,
            boolean includeDeleted
    ) {
        logger.info(
                "Fetching users to add to group: room_id={}, requester_id={}, limit={}, offset={}, include_deleted={}",
                roomId, requesterId, limit, offset, includeDeleted
        );

        // ─────────────────────────────────────────────
        // 1. Fetch users from external Auth Service
        // ─────────────────────────────────────────────
        List<UserView> authServiceUsers = userApiClient.getListUsers(limit, offset, includeDeleted);
        logger.debug(
                "Retrieved {} users from Auth Service for group addition",
                authServiceUsers.size()
        );

        // ─────────────────────────────────────────────
        // 2. Fetch existing members of the room to exclude them
        // ─────────────────────────────────────────────
        List<Member> existingMembers = memberQueryService.getAllActiveMembersByRoomId(roomId);
        Set<UUID> existingMemberIds = existingMembers.stream()
                .map(Member::userId)
                .collect(Collectors.toSet());
        
        logger.debug(
                "Retrieved {} existing members for room_id={} to exclude from results",
                existingMemberIds.size(), roomId
        );

        // ─────────────────────────────────────────────
        // 3. Filter: exclude existing members and the requester
        // ─────────────────────────────────────────────
        List<UserView> result = filterAndDeduplicateUsersForGroupAddition(
                authServiceUsers, requesterId, existingMemberIds
        );

        logger.info(
                "Successfully built user list for group addition: total_available={}, excluded_members={}, excluded_requester={}, room_id={}",
                result.size(), existingMemberIds.size(), 1, roomId
        );

        return result;
    }

    /**
     * Helper to filter out existing members, the requester, and remove duplicates from a list of UserViews.
     *
     * @param users the list of UserViews to filter
     * @param requesterId the ID of the current user (requester) to exclude
     * @param existingMemberIds set of user IDs who are already members of the room
     * @return filtered and deduplicated list of UserViews
     */
    private List<UserView> filterAndDeduplicateUsersForGroupAddition(
            List<UserView> users, 
            UUID requesterId, 
            Set<UUID> existingMemberIds
    ) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        // Use a Set to track seen userIds for deduplication
        Set<UUID> seenUserIds = new HashSet<>();
        List<UserView> result = new ArrayList<>();

        for (UserView user : users) {
            if (user == null || user.userId() == null) {
                continue; // Skip invalid entries
            }

            // Exclude requester, existing members, and duplicates
            if (!user.userId().equals(requesterId) 
                    && !existingMemberIds.contains(user.userId()) 
                    && seenUserIds.add(user.userId())) {
                result.add(user);
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // NEW: Get single room by ID for detail view (settings, member management)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve a single room's full details for the room detail/settings view.
     *
     * <p>Flow:
     * <ol>
     *   <li>Fetch room by ID via roomQueryService</li>
     *   <li>Verify current user is an active member of the room (authorization check)</li>
     *   <li>Fetch user's membership status to determine isAdmin flag</li>
     *   <li>For DIRECT rooms: find the OTHER participant (not current user) and fetch their UserView</li>
     *   <li>Build GetRoomByIdDTO with room metadata, admin/owner flags, and timestamps</li>
     *   <li>Return DTO ready for HTTP response (with relative image paths)</li>
     * </ol>
     * </p>
     *
     * <p><strong>Authorization:</strong> Returns Optional.empty() if:
     * <ul>
     *   <li>Room not found or is deleted</li>
     *   <li>Current user is not an active member of the room</li>
     * </ul>
     * </p>
     *
     * <p><strong>Image URL note:</strong> The returned DTO contains RELATIVE image paths.
     * Use {@link GetRoomByIdDTO#withProfileImageUrl(String)} and
     * {@link GetRoomByIdDTO#withCoverImageUrl(String)} to convert to absolute URLs
     * at the controller layer.</p>
     *
     * <p><strong>DIRECT room handling:</strong> For DIRECT rooms, the display name and
     * profile image are dynamically resolved from the OTHER participant's UserView
     * (not the current user), ensuring consistent UX where each participant sees
     * the other person's name and picture.</p>
     *
     * @param roomId the UUID of the room to fetch
     * @param currentUserId the authenticated user requesting the room details
     * @return Optional<GetRoomByIdDTO> with room details, or empty if not found/not authorized
     */
    public Optional<GetRoomByIdDTO> getRoomById(UUID roomId, UUID currentUserId) {
        logger.info("Fetching room details: room_id={}, requester_id={}", roomId, currentUserId);

        // ─────────────────────────────────────────────
        // 1. Fetch room by ID
        // ─────────────────────────────────────────────
        Optional<Room> roomOpt = roomQueryService.getRoomById(roomId);
        
        if (roomOpt.isEmpty()) {
            logger.debug("Room not found or inactive: room_id={}", roomId);
            return Optional.empty();
        }
        
        Room room = roomOpt.get();

        // ─────────────────────────────────────────────
        // 2. Verify user is an active member (authorization)
        // ─────────────────────────────────────────────
        boolean isMember = memberQueryService.isUserActiveMember(currentUserId, roomId);
        if (!isMember) {
            logger.warn(
                    "Access denied: user_id={} is not an active member of room_id={}",
                    currentUserId, roomId
            );
            return Optional.empty();
        }

        // ─────────────────────────────────────────────
        // 3. Fetch user's membership status for isAdmin flag
        // ─────────────────────────────────────────────
        Member.Status userStatus = memberQueryService.getMemberStatusInRoom(currentUserId, roomId);
        boolean isAdmin = (userStatus != null && userStatus == Member.Status.ADMIN);
        logger.debug(
                "User membership status: user_id={}, room_id={}, status={}, is_admin={}",
                currentUserId, roomId, userStatus, isAdmin
        );

        // ─────────────────────────────────────────────
        // 4. For DIRECT rooms: fetch the OTHER participant's UserView
        // ─────────────────────────────────────────────
        UserView otherParticipantUser = null;
        if (room.type() == Room.Type.DIRECT) {
            UUID otherParticipantId = findOtherParticipantInDirectRoom(roomId, currentUserId);
            if (otherParticipantId != null) {
                otherParticipantUser = fetchUserView(otherParticipantId);
                if (otherParticipantUser == null) {
                    logger.warn(
                            "Failed to fetch UserView for other participant in DIRECT room: room_id={}, other_participant_id={}",
                            roomId, otherParticipantId
                    );
                    return Optional.empty();
                }
                logger.debug(
                        "Resolved other participant for DIRECT room: room_id={}, other_user_id={}, username={}",
                        roomId, otherParticipantId, otherParticipantUser.username()
                );
            } else {
                logger.warn(
                        "Could not find other participant in DIRECT room: room_id={}, current_user_id={}",
                        roomId, currentUserId
                );
                return Optional.empty();
            }
        }

        // ─────────────────────────────────────────────
        // 5. Build DTO using factory method
        // ─────────────────────────────────────────────
        GetRoomByIdDTO dto = GetRoomByIdDTO.fromRoom(
                room,
                otherParticipantUser,  // For DIRECT: this is the OTHER participant (not current user)
                isAdmin,
                currentUserId          // Used for is_owner calculation in DTO factory
        );

        logger.info(
                "Successfully built room detail DTO: room_id={}, name='{}', type={}, is_group={}",
                dto.roomId(), dto.name(), dto.type(), dto.isGroup()
        );

        return Optional.of(dto);
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
     * @param lastMessage the Message domain object representing the room's last message (may be null for empty GROUP rooms)
     * @param otherParticipantUser the UserView of the OTHER participant in a DIRECT room 
     *        (required for DIRECT rooms, null for GROUP)
     * @param lastMessageSenderUsername the resolved username of the last message's sender (may be null for empty GROUP rooms)
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
                lastMessage,                   // May be null for empty GROUP rooms
                otherParticipantUser,          // For DIRECT: this is the OTHER participant (not current user)
                lastMessageSenderUsername,     // May be null for empty GROUP rooms
                currentUserId,                 // Used for is_mine calculation in LastMessagePreview
                unreadCount                    // Unread count for current user in this room
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