package com.example.chat_service.application.rooms.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.chat_service.application.rooms.handlers.dtos.GroupUpdateActionsResponse;
import com.example.chat_service.application.rooms.handlers.dtos.GroupCreationResponse;
import com.example.chat_service.application.rooms.handlers.dtos.PrivateRoomCreationResponse;
import com.example.chat_service.application.rooms.services.RoomCommandServiceInterface;
import com.example.chat_service.application.members.services.MemberCommandServiceInterface;
import com.example.chat_service.domain.rooms.RoomAggregate;
import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.dtos.users.services.UserApiClient;
import com.example.chat_service.infrastructure.media.LocalMediaStorageService;

/**
 * Application-layer orchestrator for room commands.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create/load room aggregates using domain factories</li>
 *   <li>Delegate persistence to command services (Room + Member)</li>
 *   <li>Handle media uploads via LocalMediaStorageService</li>
 *   <li>Fetch external user data via UserApiClient</li>
 *   <li>Build enriched API DTO responses (GroupCreationResponse, PrivateRoomCreationResponse)</li>
 *   <li>Coordinate group room creation with bulk member creation</li>
 *   <li>Handle DIRECT room deduplication via bidirectional creator+friend lookup</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to RoomAggregate/MemberAggregate)</li>
 *   <li>Directly access database (delegated to Repositories via Services)</li>
 *   <li>Build absolute URLs — that's handled at the controller layer via MediaUrlService</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler returns DTOs with RELATIVE image paths
 * (as stored in the domain/database). The controller layer is responsible for converting
 * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}.
 * This mirrors the pattern used in {@code PostCommandHandler}.</p>
 *
 * <p><strong>DIRECT room deduplication:</strong> Before creating a new DIRECT room,
 * the handler checks BOTH orderings of creator+friend using {@code loadByCreatorAndFriendId}.
 * If either lookup finds an existing room, that room is returned to prevent duplicate
 * conversations between the same two users.</p>
 *
 * <p><strong>Group room participant validation:</strong> When creating a GROUP room,
 * the handler requires at least 2 participant IDs (excluding the creator). It then
 * creates member records for all participants in a single transactional flow.</p>
 */
@Component
public class RoomCommandHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(RoomCommandHandler.class);

    private final RoomCommandServiceInterface roomCommandService;
    private final MemberCommandServiceInterface memberCommandService;
    private final UserApiClient userApiClient;
    private final LocalMediaStorageService mediaStorageService;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param roomCommandService handles persistence of RoomAggregate
     * @param memberCommandService handles persistence of MemberAggregate (for group participants)
     * @param userApiClient fetches user data from external Auth Service
     * @param mediaStorageService handles local file storage for uploaded images
     */
    public RoomCommandHandler(
            RoomCommandServiceInterface roomCommandService,
            MemberCommandServiceInterface memberCommandService,
            UserApiClient userApiClient,
            LocalMediaStorageService mediaStorageService
    ) {
        this.roomCommandService = roomCommandService;
        this.memberCommandService = memberCommandService;
        this.userApiClient = userApiClient;
        this.mediaStorageService = mediaStorageService;
    }

    // ─────────────────────────────────────────────────────────────────
    // GROUP ROOM CREATION
    // ─────────────────────────────────────────────────────────────────

    /**
     * Create a new GROUP room with participants and optional profile/cover images.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate participant count (must be >= 2 excluding creator)</li>
     *   <li>Save optional profile/cover images via LocalMediaStorageService → get relative paths</li>
     *   <li>Build RoomAggregate using domain factory with RELATIVE paths</li>
     *   <li>Persist room via roomCommandService</li>
     *   <li>Create member records for creator (ADMIN) and all participants (USER)</li>
     *   <li>Fetch usernames for all members from external Auth Service</li>
     *   <li>Compose GroupCreationResponse with enriched member data</li>
     * </ol>
     * </p>
     *
     * <p><strong>Image URL note:</strong> The returned DTO contains RELATIVE image paths
     * (e.g., {@code /uploads/groups/profile/abc.jpg}). The controller layer should convert
     * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}
     * before sending the HTTP response.</p>
     *
     * @param creatorId authenticated user ID creating the group
     * @param groupName the name for the new group (1-100 chars)
     * @param description optional description (max 500 chars)
     * @param participantIds list of user IDs to add as members (must have >= 2 IDs, excluding creator)
     * @param profileImage optional profile/avatar image file; can be null or empty
     * @param coverImage optional cover/background image file; can be null or empty
     * @return GroupCreationResponse ready for HTTP response (with relative image paths)
     * @throws IllegalArgumentException if participant count is invalid
     */
    public GroupCreationResponse createGroupRoom(
            UUID creatorId,
            String groupName,
            String description,
            List<UUID> participantIds,
            MultipartFile profileImage,
            MultipartFile coverImage
    ) {
        logger.info(
                "Creating GROUP room: creator_id={}, group_name='{}', participant_count={}",
                creatorId, groupName, participantIds != null ? participantIds.size() : 0
        );

        // ─────────────────────────────────────────────
        // 1. Validate participant count (>= 2 excluding creator)
        // ─────────────────────────────────────────────
        if (participantIds == null || participantIds.size() < 2) {
            throw new IllegalArgumentException(
                    "GROUP room requires at least 2 participants (excluding creator). Provided: " +
                    (participantIds != null ? participantIds.size() : 0)
            );
        }

        // ─────────────────────────────────────────────
        // 2. Handle image uploads (if provided)
        //    - Convert MultipartFile → local file → RELATIVE URL path
        //    - These relative paths are what get stored in the database
        // ─────────────────────────────────────────────
        String relativeProfileImageUrl = null;
        String relativeCoverImageUrl = null;

        if (profileImage != null && !profileImage.isEmpty()) {
            relativeProfileImageUrl = mediaStorageService.saveGroupProfileImage(profileImage);
            logger.debug("Profile image saved (relative path): {}", relativeProfileImageUrl);
        }

        if (coverImage != null && !coverImage.isEmpty()) {
            relativeCoverImageUrl = mediaStorageService.saveGroupCoverImage(coverImage);
            logger.debug("Cover image saved (relative path): {}", relativeCoverImageUrl);
        }

        // ─────────────────────────────────────────────
        // 3. Create room aggregate using domain factory
        //    - Validation happens inside createNewGroup()
        //    - Domain only sees RELATIVE image paths, keeping it environment-agnostic
        // ─────────────────────────────────────────────
        UUID roomId = UUID.randomUUID();
        RoomAggregate roomAggregate = RoomAggregate.createNewGroup(
                roomId,
                creatorId,
                groupName,
                description,
                relativeCoverImageUrl,    // ← Store RELATIVE path in domain/DB
                relativeProfileImageUrl   // ← Store RELATIVE path in domain/DB
        );

        // ─────────────────────────────────────────────
        // 4. Persist room via command service (transactional boundary)
        // ─────────────────────────────────────────────
        RoomAggregate savedRoom = roomCommandService.createGroupRoom(roomAggregate);

        // ─────────────────────────────────────────────
        // 5. Create member records for all participants
        //    - Creator gets ADMIN status
        //    - All participants get USER status
        //    - All operations happen within same transaction
        // ─────────────────────────────────────────────
        List<MemberAggregate> createdMembers = new ArrayList<>();

        // Create creator as ADMIN
        MemberAggregate creatorMember = MemberAggregate.createNewAsAdmin(
                UUID.randomUUID(),
                creatorId,
                savedRoom.id()
        );
        createdMembers.add(memberCommandService.createMember(creatorMember));

        // Create participants as USERs
        for (UUID participantId : participantIds) {
            // Skip if participant is same as creator (defensive)
            if (participantId.equals(creatorId)) {
                continue;
            }
            MemberAggregate participantMember = MemberAggregate.createNewAsUser(
                    UUID.randomUUID(),
                    participantId,
                    savedRoom.id()
            );
            createdMembers.add(memberCommandService.createMember(participantMember));
        }

        logger.info(
                "Created {} member records for GROUP room: room_id={}",
                createdMembers.size(),
                savedRoom.id()
        );

        // ─────────────────────────────────────────────
        // 6. Fetch usernames for all members from Auth Service
        //    - Build map of userId -> username for DTO enrichment
        // ─────────────────────────────────────────────
        List<UUID> allMemberUserIds = createdMembers.stream()
                .map(MemberAggregate::userId)
                .collect(Collectors.toList());

        Map<UUID, String> userIdToUsername = new HashMap<>();
        for (UUID userId : allMemberUserIds) {
            try {
                UserView user = userApiClient.getUserById(userId);
                userIdToUsername.put(userId, user.username());
            } catch (Exception e) {
                logger.warn("Failed to fetch username for user_id={}: {}", userId, e.getMessage());
                userIdToUsername.put(userId, "Unknown");
            }
        }

        // ─────────────────────────────────────────────
        // 7. Build enriched DTO for API response
        //    - Factory method sets admin=true, is_group=true
        //    - Creator's username replaced with "You" for personalized UX
        //    - profileImageUrl contains RELATIVE path (controller converts to absolute)
        // ─────────────────────────────────────────────
        GroupCreationResponse response = GroupCreationResponse.fromRoom(
                savedRoom,
                userIdToUsername,
                creatorId
        );

        logger.info(
                "GROUP room successfully created: room_id={}, group_name='{}', member_count={}",
                response.roomId(), response.name(), response.members().size()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // DIRECT ROOM CREATION (with bidirectional deduplication)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Create or retrieve a DIRECT message room between two users.
     *
     * <p>Flow:
     * <ol>
     *   <li>Check if DIRECT room already exists: first lookup (creatorId, friendId)</li>
     *   <li>If not found: second lookup (friendId, creatorId) — bidirectional check</li>
     *   <li>If either lookup finds a room: return existing room as PrivateRoomCreationResponse</li>
     *   <li>If both lookups fail: create new DIRECT room aggregate</li>
     *   <li>Persist room via roomCommandService</li>
     *   <li>Create member records for both participants (both as USER status)</li>
     *   <li>Fetch FULL UserView objects from external Auth Service (for name + profile image)</li>
     *   <li>Compose PrivateRoomCreationResponse with enriched member data</li>
     * </ol>
     * </p>
     *
     * <p><strong>Note:</strong> DIRECT rooms have no profile/cover images stored on the room.
     * Instead, the room displays the OTHER participant's profile picture and username
     * (like WhatsApp/Instagram), fetched from the Auth Service.</p>
     *
     * <p><strong>Image URL note:</strong> The returned DTO contains a RELATIVE profile image path
     * (from the friend's UserView). The controller layer should convert this to an absolute URL
     * using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)} before sending
     * the HTTP response.</p>
     *
     * @param creatorId authenticated user ID initiating the conversation
     * @param friendId the other participant's user ID
     * @return PrivateRoomCreationResponse ready for HTTP response
     */
    public PrivateRoomCreationResponse createDirectRoom(
            UUID creatorId,
            UUID friendId
    ) {
        logger.info(
                "Creating/retrieving DIRECT room: creator_id={}, friend_id={}",
                creatorId, friendId
        );

        // ─────────────────────────────────────────────
        // 1. Check if DIRECT room already exists (bidirectional deduplication)
        //    - First lookup: (creatorId, friendId)
        //    - Second lookup: (friendId, creatorId) — same conversation, reversed roles
        //    - If either finds a room, return it to prevent duplicate conversations
        // ─────────────────────────────────────────────
        Optional<RoomAggregate> existingRoom = roomCommandService.loadByCreatorAndFriendId(creatorId, friendId);

        // If first lookup failed, try reversed order (bidirectional check)
        if (!existingRoom.isPresent()) {
            existingRoom = roomCommandService.loadByCreatorAndFriendId(friendId, creatorId);
        }

        if (existingRoom.isPresent()) {
            logger.debug(
                    "Found existing DIRECT room: room_id={}, lookup_order=({},{})",
                    existingRoom.get().id(), creatorId, friendId
            );

            // Fetch members and FULL UserViews for existing room response
            // (needed for friend's username as room name + friend's profile pic as room image)
            List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(existingRoom.get().id());
            Map<UUID, UserView> userIdToUser = fetchUserViewsForMembers(members);

            PrivateRoomCreationResponse response = PrivateRoomCreationResponse.fromRoom(
                    existingRoom.get(),
                    userIdToUser,
                    creatorId
            );

            logger.info(
                    "Returned existing DIRECT room: room_id={}, name='{}', has_image={}",
                    response.roomId(), response.name(), response.hasProfileImage()
            );

            return response;
        }

        // ─────────────────────────────────────────────
        // 2. Create new DIRECT room aggregate
        //    - Validation happens inside createNewDirect()
        //    - DIRECT rooms have no images (null for profile/cover)
        // ─────────────────────────────────────────────
        UUID roomId = UUID.randomUUID();
        RoomAggregate roomAggregate = RoomAggregate.createNewDirect(
                roomId,
                creatorId,
                friendId
        );

        // ─────────────────────────────────────────────
        // 3. Persist room via command service (transactional boundary)
        // ─────────────────────────────────────────────
        RoomAggregate savedRoom = roomCommandService.createDirectRoom(roomAggregate);

        // ─────────────────────────────────────────────
        // 4. Create member records for both participants
        //    - Both get USER status in DIRECT rooms
        //    - All operations happen within same transaction
        // ─────────────────────────────────────────────
        List<MemberAggregate> createdMembers = new ArrayList<>();

        // Create creator as USER
        MemberAggregate creatorMember = MemberAggregate.createNewAsUser(
                UUID.randomUUID(),
                creatorId,
                savedRoom.id()
        );
        createdMembers.add(memberCommandService.createMember(creatorMember));

        // Create friend as USER
        MemberAggregate friendMember = MemberAggregate.createNewAsUser(
                UUID.randomUUID(),
                friendId,
                savedRoom.id()
        );
        createdMembers.add(memberCommandService.createMember(friendMember));

        logger.info(
                "Created {} member records for DIRECT room: room_id={}",
                createdMembers.size(),
                savedRoom.id()
        );

        // ─────────────────────────────────────────────
        // 5. Fetch FULL UserView objects (not just usernames) from Auth Service
        //    - Needed for DIRECT rooms: friend's username = room name, friend's profile pic = room image
        // ─────────────────────────────────────────────
        Map<UUID, UserView> userIdToUser = fetchUserViewsForMembers(createdMembers);

        // ─────────────────────────────────────────────
        // 6. Build enriched DTO for API response
        //    - For DIRECT rooms: name=friend's username, profile_image_url=friend's profile pic
        //    - Creator's username replaced with "You" for personalized UX
        // ─────────────────────────────────────────────
        PrivateRoomCreationResponse response = PrivateRoomCreationResponse.fromRoom(
                savedRoom,
                userIdToUser,
                creatorId
        );

        logger.info(
                "DIRECT room successfully created: room_id={}, name='{}', has_image={}",
                response.roomId(), response.name(), response.hasProfileImage()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // GROUP ROOM UPDATE ACTIONS (return GroupUpdateActionsResponse)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Delete a GROUP room and return the response DTO.
     *
     * @param roomId the UUID of the room to delete
     * @param requesterId the UUID of the user requesting deletion
     * @return GroupUpdateActionsResponse with operation="delete"
     */
    public GroupUpdateActionsResponse deleteRoom(UUID roomId, UUID requesterId) {
        logger.info("Deleting GROUP room: room_id={}, requester_id={}", roomId, requesterId);

        RoomAggregate savedRoom = roomCommandService.deleteRoom(roomId, requesterId);

        // Fetch members and usernames for response
        List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(roomId);
        Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(members);

        GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(
                savedRoom,
                userIdToUsername,
                requesterId
        );

        logger.info("GROUP room deleted: room_id={}, operation=delete", roomId);
        return response;
    }

    /**
     * Update group name and return the response DTO.
     *
     * @param roomId the UUID of the room to update
     * @param newGroupName the new group name
     * @param requesterId the UUID of the user performing the update
     * @return GroupUpdateActionsResponse with operation="update_name"
     */
    public GroupUpdateActionsResponse updateGroupName(UUID roomId, String newGroupName, UUID requesterId) {
        logger.info("Updating group name: room_id={}, new_name='{}', requester_id={}", roomId, newGroupName, requesterId);

        RoomAggregate savedRoom = roomCommandService.updateGroupName(roomId, newGroupName, requesterId);

        // Fetch members and usernames for response
        List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(roomId);
        Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(members);

        GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(
                savedRoom,
                userIdToUsername,
                requesterId
        );

        logger.info("Group name updated: room_id={}, new_name='{}'", roomId, newGroupName);
        return response;
    }

    /**
     * Update room description and return the response DTO.
     *
     * @param roomId the UUID of the room to update
     * @param newDescription the new description
     * @param requesterId the UUID of the user performing the update
     * @return GroupUpdateActionsResponse with operation="update_description"
     */
    public GroupUpdateActionsResponse updateDescription(UUID roomId, String newDescription, UUID requesterId) {
        logger.info("Updating description: room_id={}, requester_id={}", roomId, requesterId);

        RoomAggregate savedRoom = roomCommandService.updateDescription(roomId, newDescription, requesterId);

        // Fetch members and usernames for response
        List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(roomId);
        Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(members);

        GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(
                savedRoom,
                userIdToUsername,
                requesterId
        );

        logger.info("Description updated: room_id={}", roomId);
        return response;
    }

  


    /**
     * Update cover image and return the response DTO.
     *
     * @param roomId the UUID of the room to update
     * @param coverImage the new cover image file (optional, multipart)
     * @param remove if true, explicitly remove the existing cover image
     * @param requesterId the UUID of the user performing the update
     * @return GroupUpdateActionsResponse with operation="update_cover_image"
     */
    public GroupUpdateActionsResponse updateCoverImage(
            UUID roomId,
            MultipartFile coverImage,
            Boolean remove,
            UUID requesterId
    ) {
        logger.info("Updating cover image: room_id={}, requester_id={}, remove={}", roomId, requesterId, remove);

        // ─────────────────────────────────────────────
        // Handle image upload or removal
        // - If remove=true: set URL to null to clear
        // - If coverImage provided: save via LocalMediaStorageService → get relative path
        // - Otherwise: keep existing (domain handles null as "no change")
        // ─────────────────────────────────────────────
        String newCoverImageUrl = null;

        if (Boolean.TRUE.equals(remove)) {
            // Explicitly remove the cover image
            logger.debug("Removing cover image for room: room_id={}", roomId);
            newCoverImageUrl = null;
        } else if (coverImage != null && !coverImage.isEmpty()) {
            // Save new image and get relative path
            newCoverImageUrl = mediaStorageService.saveGroupCoverImage(coverImage);
            logger.debug("Cover image saved (relative path): {}", newCoverImageUrl);
        }
        // else: no image provided and not removing → pass null, domain keeps existing value

        RoomAggregate savedRoom = roomCommandService.updateCoverImage(roomId, newCoverImageUrl, requesterId);

        // Fetch members and usernames for response
        List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(roomId);
        Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(members);

        GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(
                savedRoom,
                userIdToUsername,
                requesterId
        );

        logger.info("Cover image updated: room_id={}, has_cover_image={}", roomId, response.hasCoverImage());
        return response;
    }

    /**
     * Update profile image and return the response DTO.
     *
     * @param roomId the UUID of the room to update
     * @param profileImage the new profile image file (optional, multipart)
     * @param remove if true, explicitly remove the existing profile image
     * @param requesterId the UUID of the user performing the update
     * @return GroupUpdateActionsResponse with operation="update_profile_image"
     */
    public GroupUpdateActionsResponse updateProfileImage(
            UUID roomId,
            MultipartFile profileImage,
            Boolean remove,
            UUID requesterId
    ) {
        logger.info("Updating profile image: room_id={}, requester_id={}, remove={}", roomId, requesterId, remove);

        // ─────────────────────────────────────────────
        // Handle image upload or removal
        // - If remove=true: set URL to null to clear
        // - If profileImage provided: save via LocalMediaStorageService → get relative path
        // - Otherwise: keep existing (domain handles null as "no change")
        // ─────────────────────────────────────────────
        String newProfileImageUrl = null;

        if (Boolean.TRUE.equals(remove)) {
            // Explicitly remove the profile image
            logger.debug("Removing profile image for room: room_id={}", roomId);
            newProfileImageUrl = null;
        } else if (profileImage != null && !profileImage.isEmpty()) {
            // Save new image and get relative path
            newProfileImageUrl = mediaStorageService.saveGroupProfileImage(profileImage);
            logger.debug("Profile image saved (relative path): {}", newProfileImageUrl);
        }
        // else: no image provided and not removing → pass null, domain keeps existing value

        RoomAggregate savedRoom = roomCommandService.updateProfileImage(roomId, newProfileImageUrl, requesterId);

        // Fetch members and usernames for response
        List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(roomId);
        Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(members);

        GroupUpdateActionsResponse response = GroupUpdateActionsResponse.fromRoom(
                savedRoom,
                userIdToUsername,
                requesterId
        );

        logger.info("Profile image updated: room_id={}, has_profile_image={}", roomId, response.hasProfileImage());
        return response;
    }




    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fetch full UserView objects for a list of member aggregates from external Auth Service.
     *
     * <p>Used for DIRECT rooms where we need the friend's profile picture for the room display.</p>
     *
     * @param members list of MemberAggregate to fetch UserViews for
     * @return map of userId -> UserView (with username, profilePicture, etc.)
     */
    private Map<UUID, UserView> fetchUserViewsForMembers(List<MemberAggregate> members) {
        Map<UUID, UserView> userIdToUser = new HashMap<>();
        for (MemberAggregate member : members) {
            try {
                UserView user = userApiClient.getUserById(member.userId());
                userIdToUser.put(member.userId(), user);
            } catch (Exception e) {
                logger.warn(
                        "Failed to fetch UserView for user_id={} (member_id={}): {}",
                        member.userId(), member.id(), e.getMessage()
                );
                // Fallback: create minimal UserView with just username
                UserView fallback = new UserView(
                        member.userId(),
                        "user_" + member.userId().toString().substring(0, 8),
                        null, null, null, null
                );
                userIdToUser.put(member.userId(), fallback);
            }
        }
        return userIdToUser;
    }

    /**
     * Fetch username strings for a list of member aggregates from external Auth Service.
     *
     * <p>Used for GROUP room update responses where only usernames are needed.</p>
     *
     * @param members list of MemberAggregate to fetch usernames for
     * @return map of userId -> username string
     */
    private Map<UUID, String> fetchUsernamesForMembers(List<MemberAggregate> members) {
        Map<UUID, String> userIdToUsername = new HashMap<>();
        for (MemberAggregate member : members) {
            try {
                UserView user = userApiClient.getUserById(member.userId());
                userIdToUsername.put(member.userId(), user.username());
            } catch (Exception e) {
                logger.warn(
                        "Failed to fetch username for user_id={} (member_id={}): {}",
                        member.userId(), member.id(), e.getMessage()
                );
                userIdToUsername.put(member.userId(), "Unknown");
            }
        }
        return userIdToUsername;
    }

}