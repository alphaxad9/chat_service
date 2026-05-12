package com.example.chat_service.application.messages.handlers;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.chat_service.application.members.services.MemberCommandServiceInterface;
import com.example.chat_service.application.messages.handlers.dtos.MessageCommandActionsResponse;
import com.example.chat_service.application.messages.services.MessageCommandServiceInterface;
import com.example.chat_service.application.rooms.services.RoomCommandServiceInterface;
import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.domain.messages.MessageAggregate;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.dtos.users.services.UserApiClient;
import com.example.chat_service.infrastructure.media.LocalMediaStorageService;

/**
 * Application-layer orchestrator for message commands.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create/load message aggregates using domain factories</li>
 *   <li>Delegate persistence to command services (Message + Room + Member)</li>
 *   <li>Handle media uploads via LocalMediaStorageService</li>
 *   <li>Fetch external user data via UserApiClient</li>
 *   <li>Build enriched API DTO responses (MessageCommandActionsResponse)</li>
 *   <li>Update room last activity after successful message creation</li>
 *   <li>Increment unread message count for all room members (excluding sender)</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to MessageAggregate)</li>
 *   <li>Directly access database (delegated to Repositories via Services)</li>
 *   <li>Build absolute URLs — that's handled at the controller layer via MediaUrlService</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler returns DTOs with RELATIVE image paths
 * (as stored in the domain/database). The controller layer is responsible for converting
 * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}.
 * This mirrors the pattern used in {@code RoomCommandHandler} and {@code PostCommandHandler}.</p>
 *
 * <p><strong>Reply preview logic:</strong> When a message is a reply, the handler fetches
 * the parent message to build a {@code ParentPreview} with image-over-text priority:
 * if the parent has an image, only the image is shown; otherwise, the text content is shown.</p>
 *
 * <p><strong>Unread message tracking:</strong> After a message is successfully created,
 * the handler loads all active members in the room and increments their unread count by 1,
 * excluding the message sender (who doesn't need to be notified of their own message).</p>
 */
@Component
public class MessageCommandHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(MessageCommandHandler.class);

    private final MessageCommandServiceInterface messageCommandService;
    private final RoomCommandServiceInterface roomCommandService;
    private final MemberCommandServiceInterface memberCommandService;
    private final UserApiClient userApiClient;
    private final LocalMediaStorageService mediaStorageService;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param messageCommandService handles persistence of MessageAggregate
     * @param roomCommandService handles room updates (last activity timestamp)
     * @param memberCommandService handles member updates (unread message counts)
     * @param userApiClient fetches user data from external Auth Service
     * @param mediaStorageService handles local file storage for uploaded images
     */
    public MessageCommandHandler(
            MessageCommandServiceInterface messageCommandService,
            RoomCommandServiceInterface roomCommandService,
            MemberCommandServiceInterface memberCommandService,
            UserApiClient userApiClient,
            LocalMediaStorageService mediaStorageService
    ) {
        this.messageCommandService = messageCommandService;
        this.roomCommandService = roomCommandService;
        this.memberCommandService = memberCommandService;
        this.userApiClient = userApiClient;
        this.mediaStorageService = mediaStorageService;
    }

    // ─────────────────────────────────────────────────────────────────
    // SEND MESSAGE (without image)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a new text message to a room.
     *
     * <p>Flow:
     * <ol>
     *   <li>Create MessageAggregate using domain factory</li>
     *   <li>Persist message via messageCommandService</li>
     *   <li>Update room last activity via roomCommandService</li>
     *   <li>Increment unread count for all room members (excluding sender) via memberCommandService</li>
     *   <li>Fetch sender username and profile image from Auth Service</li>
     *   <li>Compose MessageCommandActionsResponse with enriched data</li>
     * </ol>
     * </p>
     *
     * <p><strong>Image URL note:</strong> The returned DTO contains RELATIVE image paths
     * (e.g., {@code /uploads/messages/abc.jpg}). The controller layer should convert
     * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}
     * before sending the HTTP response.</p>
     *
     * @param roomId the room UUID where the message is sent
     * @param senderId the authenticated user ID sending the message
     * @param content the message text content (1-10000 chars)
     * @return MessageCommandActionsResponse ready for HTTP response (with relative image paths)
     */
    public MessageCommandActionsResponse sendMessage(
            UUID roomId,
            UUID senderId,
            String content
    ) {
        logger.info(
                "Sending message: room_id={}, sender_id={}, content_length={}",
                roomId, senderId, content != null ? content.length() : 0
        );

        // ─────────────────────────────────────────────
        // 1. Create message aggregate using domain factory
        //    - Validation happens inside createNew()
        // ─────────────────────────────────────────────
        UUID messageId = UUID.randomUUID();
        MessageAggregate messageAggregate = MessageAggregate.createNew(
                messageId,
                roomId,
                senderId,
                content
        );

        // ─────────────────────────────────────────────
        // 2. Persist message via command service (transactional boundary)
        // ─────────────────────────────────────────────
        MessageAggregate savedMessage = messageCommandService.createMessage(messageAggregate);

        // ─────────────────────────────────────────────
        // 3. Update room last activity timestamp
        // ─────────────────────────────────────────────
        roomCommandService.updateLastActivity(roomId);
        logger.debug("Updated last activity for room: room_id={}", roomId);

        // ─────────────────────────────────────────────
        // 4. Increment unread count for all room members (excluding sender)
        // ─────────────────────────────────────────────
        incrementUnreadForRoomMembers(roomId, senderId);

        // ─────────────────────────────────────────────
        // 5. Fetch sender data from Auth Service
        // ─────────────────────────────────────────────
        UserView sender = fetchUserView(senderId);
        String senderUsername = sender.username();
        String senderProfileImage = sender.profilePicture();

        // ─────────────────────────────────────────────
        // 6. Build enriched DTO for API response
        //    - is_mine is auto-calculated by comparing senderId with requesterId
        //    - imageUrl contains RELATIVE path (controller converts to absolute)
        // ─────────────────────────────────────────────
        MessageCommandActionsResponse response = MessageCommandActionsResponse.fromMessage(
                savedMessage,
                senderUsername,
                senderProfileImage,
                null,  // No parent preview for non-reply messages
                senderId  // requesterId = senderId for is_mine calculation
        );

        logger.info(
                "Message successfully sent: message_id={}, room_id={}, status={}",
                response.id(), response.roomId(), response.status()
        );

        return response;
    }

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
    }

    // ─────────────────────────────────────────────────────────────────
    // SEND REPLY MESSAGE (without image)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a reply message to an existing message in a room.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate parent message exists</li>
     *   <li>Create MessageAggregate as reply using domain factory</li>
     *   <li>Persist message via messageCommandService</li>
     *   <li>Update room last activity via roomCommandService</li>
     *   <li>Increment unread count for all room members (excluding sender) via memberCommandService</li>
     *   <li>Fetch sender username/profile image AND parent creator username for preview</li>
     *   <li>Build ParentPreview with image-over-text priority</li>
     *   <li>Compose MessageCommandActionsResponse with enriched data</li>
     * </ol>
     * </p>
     *
     * <p><strong>Reply preview logic:</strong>
     * <ul>
     *   <li>If parent has image: {@code parent_preview.image_url} is set, {@code parent_preview.content} is null</li>
     *   <li>If parent has no image: {@code parent_preview.content} is set, {@code parent_preview.image_url} is null</li>
     * </ul>
     * </p>
     *
     * @param roomId the room UUID where the message is sent
     * @param senderId the authenticated user ID sending the message
     * @param content the reply message text content (1-10000 chars)
     * @param parentId the UUID of the message being replied to
     * @return MessageCommandActionsResponse ready for HTTP response (with relative image paths)
     */
    public MessageCommandActionsResponse sendReplyMessage(
            UUID roomId,
            UUID senderId,
            String content,
            UUID parentId
    ) {
        logger.info(
                "Sending reply message: room_id={}, sender_id={}, parent_id={}",
                roomId, senderId, parentId
        );

        // ─────────────────────────────────────────────
        // 1. Create message aggregate as reply using domain factory
        //    - Validation happens inside createNewReply()
        // ─────────────────────────────────────────────
        UUID messageId = UUID.randomUUID();
        MessageAggregate messageAggregate = MessageAggregate.createNewReply(
                messageId,
                roomId,
                senderId,
                content,
                parentId
        );

        // ─────────────────────────────────────────────
        // 2. Persist message via command service (transactional boundary)
        // ─────────────────────────────────────────────
        MessageAggregate savedMessage = messageCommandService.createReplyMessage(messageAggregate);

        // ─────────────────────────────────────────────
        // 3. Update room last activity timestamp
        // ─────────────────────────────────────────────
        roomCommandService.updateLastActivity(roomId);
        logger.debug("Updated last activity for room: room_id={}", roomId);

        // ─────────────────────────────────────────────
        // 4. Increment unread count for all room members (excluding sender)
        // ─────────────────────────────────────────────
        incrementUnreadForRoomMembers(roomId, senderId);

        // ─────────────────────────────────────────────
        // 5. Fetch sender data from Auth Service
        // ─────────────────────────────────────────────
        UserView sender = fetchUserView(senderId);
        String senderUsername = sender.username();
        String senderProfileImage = sender.profilePicture();

        // ─────────────────────────────────────────────
        // 6. Fetch parent message and build preview
        // ─────────────────────────────────────────────
        MessageCommandActionsResponse.ParentPreview parentPreview = buildParentPreview(parentId);

        // ─────────────────────────────────────────────
        // 7. Build enriched DTO for API response
        // ─────────────────────────────────────────────
        MessageCommandActionsResponse response = MessageCommandActionsResponse.fromMessage(
                savedMessage,
                senderUsername,
                senderProfileImage,
                parentPreview,
                senderId
        );

        logger.info(
                "Reply message successfully sent: message_id={}, room_id={}, parent_id={}, is_reply={}",
                response.id(), response.roomId(), parentId, response.isReply()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // SEND REPLY MESSAGE WITH IMAGE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a reply message with an image attachment to an existing message in a room.
     *
     * <p>Flow:
     * <ol>
     *   <li>Save uploaded image via LocalMediaStorageService → get relative path</li>
     *   <li>Validate parent message exists</li>
     *   <li>Create MessageAggregate as reply with image using domain factory</li>
     *   <li>Persist message via messageCommandService</li>
     *   <li>Update room last activity via roomCommandService</li>
     *   <li>Increment unread count for all room members (excluding sender) via memberCommandService</li>
     *   <li>Fetch sender username/profile image AND parent creator username for preview</li>
     *   <li>Build ParentPreview with image-over-text priority</li>
     *   <li>Compose MessageCommandActionsResponse with enriched data</li>
     * </ol>
     * </p>
     *
     * @param roomId the room UUID where the message is sent
     * @param senderId the authenticated user ID sending the message
     * @param content the reply message text content (1-10000 chars, can be empty if image is present)
     * @param parentId the UUID of the message being replied to
     * @param image the image file to attach; can be null or empty
     * @return MessageCommandActionsResponse ready for HTTP response (with relative image paths)
     */
    public MessageCommandActionsResponse sendReplyMessageWithImage(
            UUID roomId,
            UUID senderId,
            String content,
            UUID parentId,
            MultipartFile image
    ) {
        logger.info(
                "Sending reply message with image: room_id={}, sender_id={}, parent_id={}, has_image={}",
                roomId, senderId, parentId, image != null && !image.isEmpty()
        );

        // ─────────────────────────────────────────────
        // 1. Handle image upload (if provided)
        // ─────────────────────────────────────────────
        String relativeImageUrl = null;
        if (image != null && !image.isEmpty()) {
            relativeImageUrl = mediaStorageService.saveMessageImage(image);
            logger.debug("Message image saved (relative path): {}", relativeImageUrl);
        }

        // ─────────────────────────────────────────────
        // 2. Create message aggregate as reply with image using domain factory
        // ─────────────────────────────────────────────
        UUID messageId = UUID.randomUUID();
        MessageAggregate messageAggregate = MessageAggregate.createNewReplyWithImage(
                messageId,
                roomId,
                senderId,
                content,
                parentId,
                relativeImageUrl
        );

        // ─────────────────────────────────────────────
        // 3. Persist message via command service (transactional boundary)
        // ─────────────────────────────────────────────
        MessageAggregate savedMessage = messageCommandService.createReplyMessageWithImage(messageAggregate);

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
        // 7. Fetch parent message and build preview
        // ─────────────────────────────────────────────
        MessageCommandActionsResponse.ParentPreview parentPreview = buildParentPreview(parentId);

        // ─────────────────────────────────────────────
        // 8. Build enriched DTO for API response
        // ─────────────────────────────────────────────
        MessageCommandActionsResponse response = MessageCommandActionsResponse.fromMessage(
                savedMessage,
                senderUsername,
                senderProfileImage,
                parentPreview,
                senderId
        );

        logger.info(
                "Reply message with image successfully sent: message_id={}, room_id={}, parent_id={}, has_image={}",
                response.id(), response.roomId(), parentId, response.hasImage()
        );

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Increment unread message count by 1 for all active members in a room,
     * excluding the message sender.
     *
     * <p>This ensures that when a new message is sent, all other participants
     * see an unread badge/notification for that room.</p>
     *
     * @param roomId the room UUID
     * @param senderId the user ID who sent the message (to be excluded from increment)
     */
    private void incrementUnreadForRoomMembers(UUID roomId, UUID senderId) {
        try {
            // Load all active members in the room
            var members = memberCommandService.bulkLoadActiveByRoomId(roomId);
            logger.debug(
                    "Loaded {} active members for unread increment: room_id={}",
                    members.size(),
                    roomId
            );

            // Increment unread count for each member except the sender
            for (MemberAggregate member : members) {
                if (!member.userId().equals(senderId)) {
                    memberCommandService.addUnreadMessages(member.id(), 1);
                    logger.debug(
                            "Incremented unread for member: member_id={}, user_id={}, room_id={}",
                            member.id(),
                            member.userId(),
                            roomId
                    );
                }
            }
        } catch (Exception e) {
            // Log but don't fail the message send operation
            // Unread counts can be reconciled later via background job if needed
            logger.warn(
                    "Failed to increment unread messages for room members: room_id={}, sender_id={}, error={}",
                    roomId,
                    senderId,
                    e.getMessage()
            );
        }
    }

    /**
     * Fetch full UserView for a user ID from external Auth Service.
     *
     * @param userId the user ID to fetch
     * @return UserView with username, profilePicture, etc.
     */
    private UserView fetchUserView(UUID userId) {
        try {
            return userApiClient.getUserById(userId);
        } catch (Exception e) {
            logger.warn("Failed to fetch UserView for user_id={}: {}", userId, e.getMessage());
            // Fallback: create minimal UserView with just username
            return new UserView(
                    userId,
                    "user_" + userId.toString().substring(0, 8),
                    null, null, null, null
            );
        }
    }

    /**
     * Build ParentPreview for a reply message by fetching the parent message
     * and its creator's username from the Auth Service.
     *
     * <p>Applies image-over-text priority: if parent has image, only image is shown;
     * otherwise, content is shown.</p>
     *
     * @param parentId the UUID of the parent message
     * @return ParentPreview with appropriate content/image based on priority rules, or null if parent not found
     */
    private MessageCommandActionsResponse.ParentPreview buildParentPreview(UUID parentId) {
        try {
            // Load parent message from repository via command service
            Optional<MessageAggregate> parentOpt = messageCommandService.loadAggregateOptional(parentId);
            
            if (parentOpt.isEmpty()) {
                logger.warn("Parent message not found for reply preview: parent_id={}", parentId);
                return null;
            }
            
            MessageAggregate parentAggregate = parentOpt.get();
            
            // Fetch parent creator's username
            String parentCreatorUsername = fetchUserView(parentAggregate.message().senderId()).username();
            
            // Build preview with image-over-text priority
            return MessageCommandActionsResponse.ParentPreview.fromMessage(
                    parentAggregate.message(),
                    parentCreatorUsername
            );
            
        } catch (Exception e) {
            logger.warn("Failed to build parent preview for parent_id={}: {}", parentId, e.getMessage());
            return null;
        }
    }
}