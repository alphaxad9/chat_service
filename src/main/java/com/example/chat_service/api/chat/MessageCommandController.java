package com.example.chat_service.api.chat;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.chat_service.application.messages.handlers.MessageCommandHandler;
import com.example.chat_service.application.messages.handlers.dtos.MessageCommandActionsResponse;
import com.example.chat_service.infrastructure.media.MediaUrlService;
import com.example.chat_service.infrastructure.security.UserContext;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for message command operations.
 *
 * <p>Handles HTTP requests to send messages (text, with image, replies) via {@code application/json}
 * or {@code multipart/form-data}. Authentication is handled by {@code JWTAuthenticationFilter}
 * which populates {@code UserContext} with the authenticated user ID. This controller extracts
 * that ID for ownership and delegates to the application layer for business logic orchestration.</p>
 *
 * <p><strong>Security principles:</strong>
 * <ul>
 *   <li>Never trust client-submitted {@code sender_id} — always extract from {@code UserContext}</li>
 *   <li>Domain aggregates enforce ownership checks; controller passes verified requester ID</li>
 *   <li>Image URLs returned to frontend are absolute; domain/database stores relative paths</li>
 * </ul>
 * </p>
 *
 * <p><strong>Image URL handling:</strong>
 * <p>The handler returns DTOs with RELATIVE image paths (e.g., {@code /uploads/messages/abc.jpg}).
 * This controller converts them to ABSOLUTE URLs using {@code MediaUrlService} before sending
 * the HTTP response, ensuring frontend-ready URLs without polluting the domain layer.</p>
 *
 * <pre>{@code
 * // Response example for message with image:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "room_id": "660e8400-e29b-41d4-a716-446655440001",
 *   "content": "Check this out!",
 *   "image_url": "http://127.0.0.1:8005/uploads/messages/abc123.jpg",
 *   "is_reply": false,
 *   "parent_preview": null,
 *   "created_at": "2024-01-15T10:30:00Z",
 *   "is_mine": true,
 *   "status": "SENT",
 *   "sender_username": "ishimwe",
 *   "sender_profile_image": "http://127.0.0.1:8005/uploads/users/profile/xyz.jpg",
 *   "has_image": true,
 *   "is_deleted": false
 * }
 * }</pre>
 * </p>
 */
@RestController
@RequestMapping("/api/messages")
public class MessageCommandController {

    private static final Logger logger = LoggerFactory.getLogger(MessageCommandController.class);

    private final MessageCommandHandler messageCommandHandler;
    private final MediaUrlService mediaUrlService;

    /**
     * Constructor injection — Spring will auto-wire dependencies
     * because they're annotated with @Component or @Service.
     */
    public MessageCommandController(
            MessageCommandHandler messageCommandHandler,
            MediaUrlService mediaUrlService
    ) {
        this.messageCommandHandler = messageCommandHandler;
        this.mediaUrlService = mediaUrlService;
    }

    // ─────────────────────────────────────────────────────────────────
    // SEND TEXT MESSAGE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a new text message to a room.
     *
     * <p><strong>Request body (application/json):</strong>
     * <pre>{@code
     * {
     *   "room_id": "660e8400-e29b-41d4-a716-446655440001",
     *   "content": "Hello, world!"
     * }
     * }</pre>
     * </p>
     */
    @PostMapping(
            path = "",
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public ResponseEntity<MessageCommandActionsResponse> sendMessage(
            @RequestBody
            SendMessageRequest requestDto
    ) {
        UUID senderId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for sendMessage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Processing text message send: room_id={}, sender_id={}, content_length={}",
                requestDto.roomId(), senderId, requestDto.content() != null ? requestDto.content().length() : 0
        );

        MessageCommandActionsResponse response = messageCommandHandler.sendMessage(
                requestDto.roomId(),
                senderId,
                requestDto.content()
        );

        response = convertImageUrlsToAbsolute(response, getCurrentRequest());

        logger.info(
                "Text message successfully sent: message_id={}, room_id={}, status={}",
                response.id(), response.roomId(), response.status()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

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

    // ─────────────────────────────────────────────────────────────────
    // SEND REPLY MESSAGE (TEXT ONLY)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a reply message to an existing message in a room.
     *
     * <p><strong>Request body (application/json):</strong>
     * <pre>{@code
     * {
     *   "room_id": "660e8400-e29b-41d4-a716-446655440001",
     *   "content": "I agree with that!",
     *   "parent_id": "550e8400-e29b-41d4-a716-446655440000"
     * }
     * }</pre>
     * </p>
     */
    @PostMapping(
            path = "/reply",
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public ResponseEntity<MessageCommandActionsResponse> sendReplyMessage(
            @RequestBody
            SendReplyMessageRequest requestDto
    ) {
        UUID senderId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for sendReplyMessage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Processing reply message send: room_id={}, sender_id={}, parent_id={}",
                requestDto.roomId(), senderId, requestDto.parentId()
        );

        MessageCommandActionsResponse response = messageCommandHandler.sendReplyMessage(
                requestDto.roomId(),
                senderId,
                requestDto.content(),
                requestDto.parentId()
        );

        response = convertImageUrlsToAbsolute(response, getCurrentRequest());

        logger.info(
                "Reply message successfully sent: message_id={}, room_id={}, parent_id={}, is_reply={}",
                response.id(), response.roomId(), requestDto.parentId(), response.isReply()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // SEND REPLY MESSAGE WITH IMAGE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send a reply message with an image attachment to an existing message in a room.
     *
     * <p><strong>Request format (multipart/form-data):</strong>
     * <ul>
     *   <li>{@code room_id}: UUID of the target room (form field)</li>
     *   <li>{@code content}: Reply text, 1-10000 chars, can be empty if image present (form field)</li>
     *   <li>{@code parent_id}: UUID of the message being replied to (form field)</li>
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
            path = "/reply/with-image",
            consumes = {"multipart/form-data"},
            produces = {"application/json"}
    )
    public ResponseEntity<MessageCommandActionsResponse> sendReplyMessageWithImage(
            @RequestPart("room_id") UUID roomId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart("parent_id") UUID parentId,
            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        UUID senderId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found in UserContext for sendReplyMessageWithImage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info(
                "Processing reply message with image send: room_id={}, sender_id={}, parent_id={}, has_image={}",
                roomId, senderId, parentId, image != null && !image.isEmpty()
        );

        MessageCommandActionsResponse response = messageCommandHandler.sendReplyMessageWithImage(
                roomId,
                senderId,
                content,
                parentId,
                image
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info(
                "Reply message with image successfully sent: message_id={}, room_id={}, parent_id={}, has_image={}",
                response.id(), response.roomId(), parentId, response.hasImage()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // MESSAGE UPDATE ACTIONS (return MessageCommandActionsResponse)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Delete a message (soft-delete).
     *
     * <p><strong>Authorization:</strong> Only the message sender can delete their own message.</p>
     *
     * @param messageId the UUID of the message to delete (path variable)
     * @return MessageCommandActionsResponse with is_deleted=true
     */
    @DeleteMapping("/{message_id}")
    public ResponseEntity<MessageCommandActionsResponse> deleteMessage(
            @PathVariable("message_id") UUID messageId,
            HttpServletRequest request
    ) {
        UUID actorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for deleteMessage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Deleting message: message_id={}, actor_id={}", messageId, actorId);

        MessageCommandActionsResponse response = messageCommandHandler.deleteMessage(messageId, actorId);

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Message deleted: message_id={}, is_deleted={}", messageId, response.isDeleted());
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a message as RECEIVED (delivered to recipient's device).
     *
     * <p><strong>Authorization:</strong> Only the message receiver (NOT the sender) can mark as received.</p>
     *
     * @param messageId the UUID of the message to mark (path variable)
     * @return MessageCommandActionsResponse with status="RECEIVED"
     */
    @PatchMapping("/{message_id}/received")
    public ResponseEntity<MessageCommandActionsResponse> markAsReceived(
            @PathVariable("message_id") UUID messageId,
            HttpServletRequest request
    ) {
        UUID actorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for markAsReceived");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Marking message as RECEIVED: message_id={}, actor_id={}", messageId, actorId);

        MessageCommandActionsResponse response = messageCommandHandler.markAsReceived(messageId, actorId);

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Message marked as RECEIVED: message_id={}, status={}", messageId, response.status());
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a message as SEEN (read by recipient).
     *
     * <p><strong>Authorization:</strong> Only the message receiver (NOT the sender) can mark as seen.</p>
     *
     * @param messageId the UUID of the message to mark (path variable)
     * @return MessageCommandActionsResponse with status="SEEN" and seen_at timestamp
     */
    @PatchMapping("/{message_id}/seen")
    public ResponseEntity<MessageCommandActionsResponse> markAsSeen(
            @PathVariable("message_id") UUID messageId,
            HttpServletRequest request
    ) {
        UUID actorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for markAsSeen");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Marking message as SEEN: message_id={}, actor_id={}", messageId, actorId);

        MessageCommandActionsResponse response = messageCommandHandler.markAsSeen(messageId, actorId);

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Message marked as SEEN: message_id={}, status={}, seen_at={}", 
                messageId, response.status(), response.createdAt());
        return ResponseEntity.ok(response);
    }

    /**
     * Update message content (edit text).
     *
     * <p><strong>Authorization:</strong> Only the message sender can edit their own message.</p>
     *
     * <p><strong>Request body (application/json):</strong>
     * <pre>{@code
     * {
     *   "new_content": "Updated message text here"
     * }
     * }</pre>
     * </p>
     *
     * @param messageId the UUID of the message to update (path variable)
     * @return MessageCommandActionsResponse with updated content
     */
    @PatchMapping(
            path = "/{message_id}/content",
            consumes = {"application/json"},
            produces = {"application/json"}
    )
    public ResponseEntity<MessageCommandActionsResponse> updateContent(
            @PathVariable("message_id") UUID messageId,
            @RequestBody UpdateContentRequest requestDto,
            HttpServletRequest request
    ) {
        UUID actorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for updateContent");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Updating message content: message_id={}, actor_id={}, content_length={}", 
                messageId, actorId, requestDto.newContent() != null ? requestDto.newContent().length() : 0);

        MessageCommandActionsResponse response = messageCommandHandler.updateContent(
                messageId,
                requestDto.newContent(),
                actorId
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Message content updated: message_id={}, content_length={}", 
                messageId, response.content().length());
        return ResponseEntity.ok(response);
    }

    /**
     * Update message image (edit or remove attachment).
     *
     * <p><strong>Authorization:</strong> Only the message sender can update their own message image.</p>
     *
     * <p><strong>Request format (multipart/form-data):</strong>
     * <ul>
     *   <li>{@code image}: New image file to attach (optional, file part)</li>
     *   <li>{@code remove}: Set to {@code true} to explicitly remove the existing image (optional, query param)</li>
     * </ul>
     * </p>
     *
     * <p><strong>Behavior:</strong>
     * <ul>
     *   <li>If {@code image} is provided: upload new image and replace existing</li>
     *   <li>If {@code remove=true}: clear the image URL (set to null)</li>
     *   <li>If neither: no change to image (domain keeps existing value)</li>
     * </ul>
     * </p>
     *
     * @param messageId the UUID of the message to update (path variable)
     * @param image the new image file (optional, multipart)
     * @param remove if true, explicitly remove the existing image (optional, query param)
     * @return MessageCommandActionsResponse with updated image_url and has_image flag
     */
    @PatchMapping(
            path = "/{message_id}/image",
            consumes = {"multipart/form-data"},
            produces = {"application/json"}
    )
    public ResponseEntity<MessageCommandActionsResponse> updateImage(
            @PathVariable("message_id") UUID messageId,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "remove", required = false) Boolean remove,
            HttpServletRequest request
    ) {
        UUID actorId = UserContext
                .getUserIdAsUuid()
                .orElseThrow(() -> {
                    logger.error("Unauthorized: No authenticated user found for updateImage");
                    return new RuntimeException("Unauthorized: No authenticated user found in context");
                });

        logger.info("Updating message image: message_id={}, actor_id={}, remove={}", 
                messageId, actorId, remove);

        // If remove=true, pass null to handler to clear the image
        // If image is provided, pass the MultipartFile to handler for upload
        // If neither, pass null (handler keeps existing value)
        MultipartFile imageToProcess = Boolean.TRUE.equals(remove) ? null : image;

        MessageCommandActionsResponse response = messageCommandHandler.updateImage(
                messageId,
                imageToProcess,
                actorId
        );

        response = convertImageUrlsToAbsolute(response, request);

        logger.info("Message image updated: message_id={}, has_image={}", 
                messageId, response.hasImage());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Get the current HttpServletRequest from the request context.
     * Used for endpoints that don't explicitly receive HttpServletRequest as a parameter.
     *
     * @return the current HttpServletRequest
     */
    private HttpServletRequest getCurrentRequest() {
        return ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder
                        .getRequestAttributes())
                .getRequest();
    }

    /**
     * Convert all relative image URLs in a MessageCommandActionsResponse to absolute URLs
     * using the MediaUrlService and the current HTTP request.
     *
     * @param response the DTO with relative image paths
     * @param request the current HttpServletRequest for building base URL
     * @return new DTO instance with absolute image URLs
     */
    private MessageCommandActionsResponse convertImageUrlsToAbsolute(
            MessageCommandActionsResponse response,
            HttpServletRequest request
    ) {
        // Convert message image URL
        if (response.hasImage() && response.imageUrl() != null && !response.imageUrl().isBlank()) {
            String absoluteImageUrl = mediaUrlService.buildMediaUrl(request, response.imageUrl());
            response = response.withImageUrl(absoluteImageUrl);
            logger.debug("Converted message image to absolute URL: {}", absoluteImageUrl);
        }

        // Convert sender profile image URL
        if (response.senderProfileImage() != null && !response.senderProfileImage().isBlank()) {
            String absoluteSenderImage = mediaUrlService.buildMediaUrl(request, response.senderProfileImage());
            response = response.withSenderProfileImage(absoluteSenderImage);
            logger.debug("Converted sender profile image to absolute URL: {}", absoluteSenderImage);
        }

        // Convert parent preview image URL (if present)
        if (response.parentPreview() != null && response.parentPreview().imageUrl() != null && !response.parentPreview().imageUrl().isBlank()) {
            String absoluteParentImage = mediaUrlService.buildMediaUrl(request, response.parentPreview().imageUrl());
            response = response.withParentImageUrl(absoluteParentImage);
            logger.debug("Converted parent preview image to absolute URL: {}", absoluteParentImage);
        }

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // REQUEST DTOs
    // ─────────────────────────────────────────────────────────────────

    /**
     * Request DTO for sending a text message.
     */
    public record SendMessageRequest(
            @JsonProperty("room_id") UUID roomId,
            @JsonProperty("content") String content
    ) {}

    /**
     * Request DTO for sending a reply message.
     */
    public record SendReplyMessageRequest(
            @JsonProperty("room_id") UUID roomId,
            @JsonProperty("content") String content,
            @JsonProperty("parent_id") UUID parentId
    ) {}

    /**
     * Request DTO for updating message content.
     */
    public record UpdateContentRequest(
            @JsonProperty("new_content") String newContent
    ) {}
}