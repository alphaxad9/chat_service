(give me full code file // chat_service/src/main/java/com/example/chat_service/application/rooms/handlers/RoomCommandHandler.java

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
     *   <li>Fetch usernames from external Auth Service</li>
     *   <li>Compose PrivateRoomCreationResponse with enriched member data</li>
     * </ol>
     * </p>
     *
     * <p><strong>Note:</strong> DIRECT rooms have no profile/cover images.
     * Images are derived from participant profiles at query time.</p>
     *
     * <p><strong>Image URL note:</strong> The returned DTO has no image fields for DIRECT rooms.
     * Frontend should fetch participant profile images separately via user service.</p>
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

            // Fetch members and usernames for existing room response
            List<MemberAggregate> members = memberCommandService.bulkLoadActiveByRoomId(existingRoom.get().id());
            Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(members);

            // Use manual construction for DIRECT rooms since fromRoom() validates GROUP only
            PrivateRoomCreationResponse response = buildDirectRoomResponse(
                    existingRoom.get(),
                    userIdToUsername,
                    creatorId
            );

            logger.info(
                    "Returned existing DIRECT room: room_id={}, member_count={}",
                    response.roomId(), response.members().size()
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
        // 5. Fetch usernames for both members from Auth Service
        // ─────────────────────────────────────────────
        Map<UUID, String> userIdToUsername = fetchUsernamesForMembers(createdMembers);

        // ─────────────────────────────────────────────
        // 6. Build enriched DTO for API response
        //    - Manual construction for DIRECT rooms (fromRoom() only accepts GROUP)
        //    - Sets is_group=false, name=null, no profile image
        //    - Creator's username replaced with "You" for personalized UX
        // ─────────────────────────────────────────────
        PrivateRoomCreationResponse response = buildDirectRoomResponse(
                savedRoom,
                userIdToUsername,
                creatorId
        );

        logger.info(
                "DIRECT room successfully created: room_id={}, member_count={}",
                response.roomId(), response.members().size()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Build a PrivateRoomCreationResponse for a DIRECT room.
     *
     * <p>Since {@code PrivateRoomCreationResponse.fromRoom()} only accepts GROUP rooms,
     * this helper manually constructs the response with appropriate values for DIRECT rooms:
     * <ul>
     *   <li>{@code name} is null (DIRECT rooms have no group name)</li>
     *   <li>{@code is_group} is false</li>
     *   <li>{@code profileImageUrl} is null (DIRECT rooms have no images)</li>
     *   <li>{@code hasProfileImage} is false</li>
     * </ul>
     * </p>
     *
     * @param room the DIRECT RoomAggregate
     * @param userIdToUsername map of userId to username for members
     * @param creatorId the authenticated user ID (for "You" substitution)
     * @return PrivateRoomCreationResponse configured for a DIRECT room
     */
    private PrivateRoomCreationResponse buildDirectRoomResponse(
            RoomAggregate room,
            Map<UUID, String> userIdToUsername,
            UUID creatorId
    ) {
        // Transform userId->username map to MemberPreview list, replacing creator's username with "You"
        List<PrivateRoomCreationResponse.MemberPreview> previews = userIdToUsername.entrySet().stream()
                .map(entry -> {
                    String username = entry.getValue();
                    String displayUsername = entry.getKey().equals(creatorId) ? "You" : username;
                    return new PrivateRoomCreationResponse.MemberPreview(displayUsername);
                })
                .toList();

        return new PrivateRoomCreationResponse(
                room.id(),
                null,                           // name is null for DIRECT rooms
                null,                           // no profile image for DIRECT rooms
                previews,
                true,                           // admin - creator has admin context in response
                false,                          // is_group = false for DIRECT rooms
                false                           // hasProfileImage = false
        );
    }

    /**
     * Fetch usernames for a list of member aggregates from external Auth Service.
     *
     * @param members list of MemberAggregate to fetch usernames for
     * @return map of userId -> username
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
} only change that direct room creation function bacuse for group every thing is 100% fine) as you mentioned in (```java
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
```

---

```java
// In RoomCommandHandler.java - update the createDirectRoom method:

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
     *   <li>Fetch full UserView objects from external Auth Service (for name + profile image)</li>
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
        Optional<RoomAggregate> existingRoom = roomCommandService.loadAggregateByCreatorAndFriend(creatorId, friendId);

        // If first lookup failed, try reversed order (bidirectional check)
        if (!existingRoom.isPresent()) {
            existingRoom = roomCommandService.loadAggregateByCreatorAndFriend(friendId, creatorId);
        }

        if (existingRoom.isPresent()) {
            logger.debug(
                    "Found existing DIRECT room: room_id={}, lookup_order=({},{})",
                    existingRoom.get().id(), creatorId, friendId
            );

            // Fetch members and full UserViews for existing room response
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
     * Fetch usernames for a list of member aggregates from external Auth Service.
     * (Legacy helper kept for GROUP room creation which only needs usernames)
     *
     * @param members list of MemberAggregate to fetch usernames for
     * @return map of userId -> username
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
```

---

## Expected Response After Fix

```json
{
  "room_id": "a036fffd-0ceb-42f9-ba67-5de2335c42a2",
  "name": "testuser9",
  "profile_image_url": "/media/profile_pictures/pexels-budget-bizar-92378004-18879101.jpg",
  "members": [
    {"username": "You"},
    {"username": "testuser9"}
  ],
  "admin": true,
  "is_group": false,
  "has_profile_image": true
}
```

### Key Changes Summary

| Change | Description |
|--------|-------------|
| **DTO parameter** | `fromRoom()` now accepts `Map<UUID, UserView>` instead of `Map<UUID, String>` to get full user data |
| **DIRECT room name** | Now set to friend's username (`friendUser.username()`) instead of `null` |
| **DIRECT room image** | Now set to friend's profile picture (`friendUser.profilePicture()`) instead of `null` |
| **Handler logic** | `createDirectRoom()` now calls `fetchUserViewsForMembers()` to get full UserView objects |
| **Fallback handling** | If Auth Service call fails, creates minimal UserView with placeholder username |

> **Note:** The controller layer should still convert the relative `profile_image_url` to an absolute URL using `MediaUrlService.buildMediaUrl(HttpServletRequest, String)` before sending the HTTP response.)