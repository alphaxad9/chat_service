still i get (ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8000/zedvye_one/users/token/   -H "Content-Type: application/json"   -H "X-CSRFToken: EW8jqY3yDI8veaqY8kUXPT31p5NxLGKrCx4CX7zeXLxa2oETl7pBT57dlvZkLSpa"   -c cookies.txt -b cookies.txt   -d '{                                                                                             "identifier": "test9@example.com",                                                                                                "password": "Test123!"                                                                                                          }' | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  1524  100  1453  100    71    876     42  0:00:01  0:00:01 --:--:--   918
{
  "message": "testuser9 logged in successfully",
  "user": {
    "id": "71885bbe-1f48-42b6-90e7-f988af5231dd",
    "username": "testuser9",
    "email": "test9@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "profile_picture": "/media/profile_pictures/pexels-budget-bizar-92378004-18879101.jpg"
  },
  "access": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA4NjU5LCJpYXQiOjE3Nzg2MDgzNTksImp0aSI6IjQzYmVmOWFlMGRkZjQ5YjZhZGJlMmJjMmY1N2UzNTI0IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.AWfTeh2YIDsIOjWf4DANSZ1d8ITbPwEAvbTSdsxouudyXpVThTud_S-p0-E1XzxJ8vSUfL0q22U84xvwEcUOXa0MwEJGbRIpP16j4Q5ETULt8VEmPOkGf9RreKdRysgR07Yl2bXlAsYe6q2k5q6l04mOwNpp9RkoLB9FSavrLDrSQPv0-JJhcvMQpVrBq5yEn2YGfodDsSSb5cxx2COJpRw_XWdAhwXQpsecE3PV0bGr14-ggb85x5n6DZOO7YO9Hy4megcvAR-OT8wQKln8eTacs2hbPLGxjgdANAqaXMWus8A14ccgpvvJxxhk_ew5yU-BHTt-XxYfpkSDmykF9A",
  "refresh": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoicmVmcmVzaCIsImV4cCI6MTc3ODY5NDc1OSwiaWF0IjoxNzc4NjA4MzU5LCJqdGkiOiIzZWE4YjU1MTdlYTU0NTNiOTdhMTg3YzJlZmI1YmQ5NCIsInVzZXJfaWQiOiI3MTg4NWJiZS0xZjQ4LTQyYjYtOTBlNy1mOTg4YWY1MjMxZGQifQ.l9M-Hdp_ZAxLTuoCeo7Wp2g-6n4Mm6x10xrcKJzgupX6xeDbABC7YpLDLPgV2XQLf_conC3s-wXQQkvwvXmwttvIW3SIPoQvfe3-jdrb_VH6DccUbPn-RsErHh-Sb95V6jTRzR8Od_HntMU-_Nq2ik3GyD3y3ZZE04ekJ4WDwy0U9562BYJUkmnFAsAwNtfnGEKtBObvL0j-ah10mCLUXcRwJEbpT_QmCxNvjic8t2kcpEA5QNbA-CX_PME1gNHAzZOcWU2D9hwsmVlWWDyawoEbFdqtZ4GCjh-B5B8s3yzuI-1gY8lwYmfq9YMmRIh8WxzsLKO4zyAmclJdg5rDCA"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA4NjU5LCJpYXQiOjE3Nzg2MDgzNTksImp0aSI6IjQzYmVmOWFlMGRkZjQ5YjZhZGJlMmJjMmY1N2UzNTI0IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.AWfTeh2YIDsIOjWf4DANSZ1d8ITbPwEAvbTSdsxouudyXpVThTud_S-p0-E1XzxJ8vSUfL0q22U84xvwEcUOXa0MwEJGbRIpP16j4Q5ETULt8VEmPOkGf9RreKdRysgR07Yl2bXlAsYe6q2k5q6l04mOwNpp9RkoLB9FSavrLDrSQPv0-JJhcvMQpVrBq5yEn2YGfodDsSSb5cxx2COJpRw_XWdAhwXQpsecE3PV0bGr14-ggb85x5n6DZOO7YO9Hy4megcvAR-OT8wQKln8eTacs2hbPLGxjgdANAqaXMWus8A14ccgpvvJxxhk_ew5yU-BHTt-XxYfpkSDmykF9A" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "content=Check out this design mockup for the new feature!" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg" | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  861k    0   120  100  861k    366  2631k --:--:-- --:--:-- --:--:-- 2627k
{
  "timestamp": "2026-05-12T17:52:59.212Z",
  "status": 415,
  "error": "Unsupported Media Type",
  "path": "/api/messages/with-image"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ curl -X POST http://127.0.0.1:8005/api/messages/with-image \
  --cookie "access_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzc4NjA4NjU5LCJpYXQiOjE3Nzg2MDgzNTksImp0aSI6IjQzYmVmOWFlMGRkZjQ5YjZhZGJlMmJjMmY1N2UzNTI0IiwidXNlcl9pZCI6IjcxODg1YmJlLTFmNDgtNDJiNi05MGU3LWY5ODhhZjUyMzFkZCJ9.AWfTeh2YIDsIOjWf4DANSZ1d8ITbPwEAvbTSdsxouudyXpVThTud_S-p0-E1XzxJ8vSUfL0q22U84xvwEcUOXa0MwEJGbRIpP16j4Q5ETULt8VEmPOkGf9RreKdRysgR07Yl2bXlAsYe6q2k5q6l04mOwNpp9RkoLB9FSavrLDrSQPv0-JJhcvMQpVrBq5yEn2YGfodDsSSb5cxx2COJpRw_XWdAhwXQpsecE3PV0bGr14-ggb85x5n6DZOO7YO9Hy4megcvAR-OT8wQKln8eTacs2hbPLGxjgdANAqaXMWus8A14ccgpvvJxxhk_ew5yU-BHTt-XxYfpkSDmykF9A" \
  -F "room_id=ca9ee906-a23a-4e07-9cb5-08684cf3b21a" \
  -F "content=Check out this design mockup for the new feature!" \
  -F "image=@/home/ishimwe/projs/django/microservices/service_one/zedvye_six/chat_service/my_local_actions/pexels-marros-33143616.jpg" | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  861k    0   120  100  861k   6109  42.8M --:--:-- --:--:-- --:--:-- 44.2M
{
  "timestamp": "2026-05-12T17:53:48.739Z",
  "status": 415,
  "error": "Unsupported Media Type",
  "path": "/api/messages/with-image"
}
ishimwe@alpha:~/projs/django/microservices/service_one/zedvye_six/chat_service$ ) please comper (
    // ─────────────────────────────────────────────────────────────────
    // SEND MESSAGE WITH IMAGE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a new message with an image attachment to a room.
     *
     * <p><strong>Request format (multipart/form-data):</strong>
     * <ul>
     *   <li>{@code room_id}: UUID of the target room (form field)</li>
     *   <li>{@code content}: Message text, 1-10000 chars, can be empty if image present (form field)</li>
     *   <li>{@code image}: Image file to attach (file part, optional)</li>
     * </ul>
     * </p>
     *
     * <p><strong>Important:</strong> Image saving is handled by {@code MessageCommandHandler}
     * via {@code LocalMediaStorageService.saveMessageImage()}. This controller only passes 
     * the {@code MultipartFile} to the handler and converts the returned relative URL to 
     * absolute for the response.</p>
     */
    @PostMapping(
            path = "/with-image",
            consumes = {"multipart/form-data"},
            produces = {"application/json"}
    )
    public ResponseEntity<MessageCommandActionsResponse> sendMessageWithImage(
            @RequestPart("room_id") UUID roomId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        UUID senderId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for sendMessageWithImage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Processing message with image send: room_id={}, sender_id={}, has_image={}",
                roomId, senderId, image != null && !image.isEmpty()
        );

        MessageCommandActionsResponse response = messageCommandHandler.sendMessageWithImage(
                roomId,
                senderId,
                content,
                image
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info(
                "Message with image successfully sent: message_id={}, room_id={}, has_image={}",
                response.id(), response.roomId(), response.hasImage()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
) with (@RestController
@RequestMapping("/api/rooms")
public class RoomCommandController {

    private static final Logger logger = LoggerFactory.getLogger(RoomCommandController.class);

    private final RoomCommandHandler roomCommandHandler;
    private final MediaUrlService mediaUrlService;

    /**
     * Constructor injection — Spring will auto-wire dependencies
     * because they're annotated with @Component or @Service.
     */
    public RoomCommandController(
            RoomCommandHandler roomCommandHandler,
            MediaUrlService mediaUrlService
    ) {
        this.roomCommandHandler = roomCommandHandler;
        this.mediaUrlService = mediaUrlService;
    }

    // ─────────────────────────────────────────────────────────────────
    // GROUP ROOM CREATION (unchanged - working correctly)
    // ─────────────────────────────────────────────────────────────────

    @PostMapping(
            path = "/groups",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<GroupCreationResponse> createGroupRoom(

            @RequestPart("group_name")
            String groupName,

            @RequestPart(value = "description", required = false)
            String description,

            @RequestPart("participant_ids")
            String participantIdsJson,

            @RequestPart(value = "profile_image", required = false)
            MultipartFile profileImage,

            @RequestPart(value = "cover_image", required = false)
            MultipartFile coverImage,

            HttpServletRequest request

    ) {
        UUID creatorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for createGroupRoom");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        List<UUID> participantIds = parseUuidList(participantIdsJson);

        logger.info(
                "Processing GROUP room creation: creator_id={}, group_name='{}', participant_count={}",
                creatorId, groupName, participantIds.size()
        );

        GroupCreationResponse response = roomCommandHandler.createGroupRoom(
                creatorId,
                groupName,
                description,
                participantIds,
                profileImage,
                coverImage
        );

        if (response.hasProfileImage() && response.profileImageUrl() != null) {
            String absoluteProfileUrl = mediaUrlService.buildMediaUrl(request, response.profileImageUrl());
            response = response.withProfileImageUrl(absoluteProfileUrl);
            logger.debug("Converted profile image to absolute URL: {}", absoluteProfileUrl);
        }

        logger.info(
                "GROUP room successfully created: room_id={}, group_name='{}'",
                response.roomId(), response.name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
) and (
    // ─────────────────────────────────────────────────────────────────
    // SEND MESSAGE WITH IMAGE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a new message with an image attachment to a room.
     *
     * <p>Flow:
     * <ol>
     *   <li>Save uploaded image via LocalMediaStorageService → get relative path</li>
     *   <li>Create MessageAggregate with image using domain factory</li>
     *   <li>Persist message via messageCommandService</li>
     *   <li>Update room last activity via roomCommandService</li>
     *   <li>Increment unread count for all room members (excluding sender) via memberCommandService</li>
     *   <li>Fetch sender username and profile image from Auth Service</li>
     *   <li>Compose MessageCommandActionsResponse with enriched data</li>
     * </ol>
     * </p>
     *
     * <p><strong>Image URL note:</strong> The returned DTO contains RELATIVE image paths
     * for both the message image and sender profile image. The controller layer should convert
     * these to absolute URLs before sending the HTTP response.</p>
     *
     * @param roomId the room UUID where the message is sent
     * @param senderId the authenticated user ID sending the message
     * @param content the message text content (1-10000 chars, can be empty if image is present)
     * @param image the image file to attach; can be null or empty
     * @return MessageCommandActionsResponse ready for HTTP response (with relative image paths)
     */
    public MessageCommandActionsResponse sendMessageWithImage(
            UUID roomId,
            UUID senderId,
            String content,
            MultipartFile image
    ) {
        logger.info(
                "Sending message with image: room_id={}, sender_id={}, has_image={}",
                roomId, senderId, image != null && !image.isEmpty()
        );

        // ─────────────────────────────────────────────
        // 1. Handle image upload (if provided)
        //    - Convert MultipartFile → local file → RELATIVE URL path
        // ─────────────────────────────────────────────
        String relativeImageUrl = null;
        if (image != null && !image.isEmpty()) {
            relativeImageUrl = mediaStorageService.saveMessageImage(image);
            logger.debug("Message image saved (relative path): {}", relativeImageUrl);
        }

        // ─────────────────────────────────────────────
        // 2. Create message aggregate using domain factory
        //    - Validation happens inside createNewWithImage()
        // ─────────────────────────────────────────────
        UUID messageId = UUID.randomUUID();
        MessageAggregate messageAggregate = MessageAggregate.createNewWithImage(
                messageId,
                roomId,
                senderId,
                content,
                relativeImageUrl
        );

        // ─────────────────────────────────────────────
        // 3. Persist message via command service (transactional boundary)
        // ─────────────────────────────────────────────
        MessageAggregate savedMessage = messageCommandService.createMessageWithImage(messageAggregate);

        // ─────────────────────────────────────────────
        // 4. Update room last activity timestamp
        // ─────────────────────────────────────────────
        roomCommandService.updateLastActivity(roomId);
        logger.debug("Updated last activity for room: room_id={}", roomId);

        // ─────────────────────────────────────────────
        // 5. Increment unread count for all room members (excluding sender)
        // ─────────────────────────────────────────────
        incrementUnreadForRoomMembers(roomId, senderId);

        // ─────────────────────────────────────────────
        // 6. Fetch sender data from Auth Service
        // ─────────────────────────────────────────────
        UserView sender = fetchUserView(senderId);
        String senderUsername = sender.username();
        String senderProfileImage = sender.profilePicture();

        // ─────────────────────────────────────────────
        // 7. Build enriched DTO for API response
        // ─────────────────────────────────────────────
        MessageCommandActionsResponse response = MessageCommandActionsResponse.fromMessage(
                savedMessage,
                senderUsername,
                senderProfileImage,
                null,  // No parent preview for non-reply messages
                senderId
        );

        logger.info(
                "Message with image successfully sent: message_id={}, room_id={}, has_image={}",
                response.id(), response.roomId(), response.hasImage()
        );

        return response;
    }) with (
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
    }) you will get the solution  and we have (package com.example.chat_service.infrastructure.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling local file storage of media files.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate and sanitize uploaded files</li>
 *   <li>Generate unique filenames to prevent collisions</li>
 *   <li>Store files in configured local directory</li>
 *   <li>Return public URL path for stored file</li>
 * </ul>
 * </p>
 *
 * <p><strong>Security note:</strong> This implementation trusts the file extension
 * from the original filename. In production, add MIME type validation, size limits,
 * and extension whitelisting.</p>
 */
@Service
public class LocalMediaStorageService {

    /**
     * Relative path where post images are stored.
     * Resolved against application working directory.
     */
    private static final String POST_UPLOAD_DIR = "uploads/posts";

    /**
     * Relative path where message images are stored.
     */
    private static final String MESSAGE_UPLOAD_DIR = "uploads/messages";

    /**
     * Relative path where group profile images are stored.
     */
    private static final String GROUP_PROFILE_UPLOAD_DIR = "uploads/groups/profile";

    /**
     * Relative path where group cover/background images are stored.
     */
    private static final String GROUP_COVER_UPLOAD_DIR = "uploads/groups/cover";

    // ─────────────────────────────────────────────────────
    // POST IMAGE METHODS (unchanged)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded image file to local storage for posts.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/posts/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String savePostImage(MultipartFile file) {

        try {
            // ─────────────────────────────────────────────
            // 1. Ensure upload directory exists
            // ─────────────────────────────────────────────
            Path uploadPath = Paths.get(POST_UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // ─────────────────────────────────────────────
            // 2. Generate unique, safe filename
            // ─────────────────────────────────────────────
            String originalName = file.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                // Extract extension including the dot, e.g. ".jpg"
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                ).toLowerCase();
            }

            // Use UUID to guarantee uniqueness + prevent path traversal
            String filename = UUID.randomUUID() + extension;

            Path destination = uploadPath.resolve(filename);

            // ─────────────────────────────────────────────
            // 3. Save file to disk
            // ─────────────────────────────────────────────
            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // ─────────────────────────────────────────────
            // 4. Return public URL path (relative to web root)
            // ─────────────────────────────────────────────
            return "/uploads/posts/" + filename;

        } catch (IOException e) {
            // Wrap checked exception in runtime exception for handler layer
            throw new RuntimeException(
                    "Failed to save image: " + file.getOriginalFilename(),
                    e
            );
        }
    }

    /**
     * Delete a previously saved post image by its public path.
     *
     * @param publicPath the path returned by savePostImage(), e.g. "/uploads/posts/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deletePostImage(String publicPath) {
        try {
            // Remove leading slash and convert to filesystem path
            String relativePath = publicPath.replaceFirst("^/", "");
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                return Files.deleteIfExists(filePath);
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to delete image: " + publicPath,
                    e
            );
        }
    }

    // ─────────────────────────────────────────────────────
    // MESSAGE IMAGE METHODS (new)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded image file to local storage for messages.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/messages/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String saveMessageImage(MultipartFile file) {

        try {
            // ─────────────────────────────────────────────
            // 1. Ensure upload directory exists
            // ─────────────────────────────────────────────
            Path uploadPath = Paths.get(MESSAGE_UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // ─────────────────────────────────────────────
            // 2. Generate unique, safe filename
            // ─────────────────────────────────────────────
            String originalName = file.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                // Extract extension including the dot, e.g. ".jpg"
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                ).toLowerCase();
            }

            // Use UUID to guarantee uniqueness + prevent path traversal
            String filename = UUID.randomUUID() + extension;

            Path destination = uploadPath.resolve(filename);

            // ─────────────────────────────────────────────
            // 3. Save file to disk
            // ─────────────────────────────────────────────
            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // ─────────────────────────────────────────────
            // 4. Return public URL path (relative to web root)
            // ─────────────────────────────────────────────
            return "/uploads/messages/" + filename;

        } catch (IOException e) {
            // Wrap checked exception in runtime exception for handler layer
            throw new RuntimeException(
                    "Failed to save image: " + file.getOriginalFilename(),
                    e
            );
        }
    }

    /**
     * Delete a previously saved message image by its public path.
     *
     * @param publicPath the path returned by saveMessageImage(), e.g. "/uploads/messages/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deleteMessageImage(String publicPath) {
        try {
            // Remove leading slash and convert to filesystem path
            String relativePath = publicPath.replaceFirst("^/", "");
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                return Files.deleteIfExists(filePath);
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to delete image: " + publicPath,
                    e
            );
        }
    }

    // ─────────────────────────────────────────────────────
    // GROUP PROFILE IMAGE METHODS (new)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded group profile image file to local storage.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/groups/profile/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String saveGroupProfileImage(MultipartFile file) {
        return saveImageToDirectory(file, GROUP_PROFILE_UPLOAD_DIR, "/uploads/groups/profile/");
    }

    /**
     * Delete a previously saved group profile image by its public path.
     *
     * @param publicPath the path returned by saveGroupProfileImage(), e.g. "/uploads/groups/profile/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deleteGroupProfileImage(String publicPath) {
        return deleteImageFromPath(publicPath);
    }

    // ─────────────────────────────────────────────────────
    // GROUP COVER/BACKGROUND IMAGE METHODS (new)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded group cover/background image file to local storage.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/groups/cover/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String saveGroupCoverImage(MultipartFile file) {
        return saveImageToDirectory(file, GROUP_COVER_UPLOAD_DIR, "/uploads/groups/cover/");
    }

    /**
     * Delete a previously saved group cover/background image by its public path.
     *
     * @param publicPath the path returned by saveGroupCoverImage(), e.g. "/uploads/groups/cover/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deleteGroupCoverImage(String publicPath) {
        return deleteImageFromPath(publicPath);
    }

    // ─────────────────────────────────────────────────────
    // INTERNAL HELPER METHODS
    // ─────────────────────────────────────────────────────

    /**
     * Internal helper to save an image to a specific directory.
     *
     * @param file the MultipartFile to save
     * @param uploadDir the filesystem directory path
     * @param publicPrefix the public URL prefix to return
     * @return the public URL path for the saved image
     */
    private String saveImageToDirectory(MultipartFile file, String uploadDir, String publicPrefix) {
        try {
            // ─────────────────────────────────────────────
            // 1. Ensure upload directory exists
            // ─────────────────────────────────────────────
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // ─────────────────────────────────────────────
            // 2. Generate unique, safe filename
            // ─────────────────────────────────────────────
            String originalName = file.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                // Extract extension including the dot, e.g. ".jpg"
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                ).toLowerCase();
            }

            // Use UUID to guarantee uniqueness + prevent path traversal
            String filename = UUID.randomUUID() + extension;

            Path destination = uploadPath.resolve(filename);

            // ─────────────────────────────────────────────
            // 3. Save file to disk
            // ─────────────────────────────────────────────
            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // ─────────────────────────────────────────────
            // 4. Return public URL path (relative to web root)
            // ─────────────────────────────────────────────
            return publicPrefix + filename;

        } catch (IOException e) {
            // Wrap checked exception in runtime exception for handler layer
            throw new RuntimeException(
                    "Failed to save image: " + file.getOriginalFilename(),
                    e
            );
        }
    }

    /**
     * Internal helper to delete an image by its public path.
     *
     * @param publicPath the public URL path to delete
     * @return true if file was deleted, false if not found
     */
    private boolean deleteImageFromPath(String publicPath) {
        try {
            // Remove leading slash and convert to filesystem path
            String relativePath = publicPath.replaceFirst("^/", "");
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                return Files.deleteIfExists(filePath);
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to delete image: " + publicPath,
                    e
            );
        }
    }
})