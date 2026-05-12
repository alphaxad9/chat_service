package com.example.chat_service.application.messages.handlers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the response after successfully executing a message command action
 * (send, edit content, edit image, delete, restore, mark as received, or mark as seen).
 * 
 * <p>Designed for immediate UI synchronization after a mutation. Contains:
 * <ul>
 *   <li>Message core data (id, room_id, content, image_url)</li>
 *   <li>Reply context (is_reply, parent preview with content/image and creator username)</li>
 *   <li>Sender info (username and profile image for display)</li>
 *   <li>State flags (is_mine, status, has_image, is_deleted)</li>
 *   <li>Operation metadata (created_at, updated_at)</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Supported operations:</strong>
 * <ul>
 *   <li>{@code "send"} - new message created</li>
 *   <li>{@code "update_content"} - message text was edited</li>
 *   <li>{@code "update_image"} - message image was changed/removed</li>
 *   <li>{@code "delete"} - message was soft-deleted</li>
 *   <li>{@code "restore"} - message was restored from soft-delete</li>
 *   <li>{@code "mark_as_received"} - receiver marked message as delivered</li>
 *   <li>{@code "mark_as_seen"} - receiver marked message as read</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Reply preview logic:</strong>
 * <ul>
 *   <li>If parent message has an image: {@code parent_preview.image_url} is populated, {@code parent_preview.content} is {@code null}</li>
 *   <li>If parent message has no image: {@code parent_preview.content} contains the text, {@code parent_preview.image_url} is {@code null}</li>
 *   <li>This "image-over-text" priority matches the frontend message preview behavior</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Design notes:</strong>
 * <ul>
 *   <li>{@code is_mine} reflects whether the requester is the message sender (for UI rendering: align right, show "Seen", etc.).</li>
 *   <li>Image URL fields contain RELATIVE paths from domain/DB (e.g. {@code /uploads/messages/abc.jpg}).
 *       Use {@link #withImageUrl(String)}, {@link #withSenderProfileImage(String)}, 
 *       and {@link #withParentImageUrl(String)} to convert to absolute URLs before sending HTTP response.</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output.</li>
 *   <li>For {@code delete} operations, frontend should use {@code is_deleted=true} 
 *       to render placeholder UI (e.g., "This message was deleted").</li>
 *   <li>{@code status} is serialized as uppercase String ("SENT", "RECEIVED", "SEEN") for API clarity.</li>
 * </ul>
 * </p>
 * 
 * <p>Usage example in service/controller layer:
 * <pre>{@code
 *   MessageAggregate message = messageCommandService.editContent(messageId, newContent, requesterId);
 *   Map<UUID, String> userIdToUsername = authClient.getUsernames(List.of(message.senderId()));
 *   Map<UUID, UserView> userIdToUser = authClient.getUserViews(List.of(message.senderId()));
 *   
 *   // If reply, fetch parent preview data
 *   ParentPreviewData parentPreview = null;
 *   if (message.isReply()) {
 *       Message parent = messageRepository.findById(message.parentId()).orElseThrow();
 *       String parentCreatorUsername = authClient.getUsername(parent.senderId());
 *       parentPreview = ParentPreviewData.fromMessage(parent, parentCreatorUsername);
 *   }
 *   
 *   UserView sender = userIdToUser.get(message.senderId());
 *   String senderUsername = userIdToUsername.get(message.senderId());
 *   
 *   MessageCommandActionsResponse response = MessageCommandActionsResponse.fromMessage(
 *       message,
 *       senderUsername,
 *       sender.profilePicture(),
 *       parentPreview,
 *       requesterId
 *   );
 *   
 *   // Convert relative → absolute URLs for frontend
 *   String mediaBaseUrl = "http://127.0.0.1:8005";
 *   response = response
 *       .withImageUrl(message.imageUrl() != null ? mediaBaseUrl + message.imageUrl() : null)
 *       .withSenderProfileImage(sender.profilePicture() != null ? mediaBaseUrl + sender.profilePicture() : null);
 *   if (response.parentPreview() != null && response.parentPreview().imageUrl() != null) {
 *       response = response.withParentImageUrl(mediaBaseUrl + response.parentPreview().imageUrl());
 *   }
 *   
 *   return ResponseEntity.ok(response);
 * }</pre>
 * </p>
 */
public record MessageCommandActionsResponse(

        @JsonProperty("id")
        UUID id,

        @JsonProperty("room_id")
        UUID roomId,

        @JsonProperty("content")
        String content,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("is_reply")
        boolean isReply,

        @JsonProperty("parent_preview")
        ParentPreview parentPreview,

        @JsonProperty("created_at")
        String createdAt,

        @JsonProperty("is_mine")
        boolean isMine,

        @JsonProperty("status")
        String status,

        @JsonProperty("sender_username")
        String senderUsername,

        @JsonProperty("sender_profile_image")
        String senderProfileImage,

        @JsonProperty("has_image")
        boolean hasImage,

        @JsonProperty("is_deleted")
        boolean isDeleted

) {

    /**
     * Minimal preview representation of a parent message for reply context.
     * 
     * <p>Follows "image-over-text" priority:
     * <ul>
     *   <li>If parent has image: {@code imageUrl} is set, {@code content} is {@code null}</li>
     *   <li>If parent has no image: {@code content} is set, {@code imageUrl} is {@code null}</li>
     * </ul>
     * </p>
     */
    public record ParentPreview(
            @JsonProperty("content")
            String content,

            @JsonProperty("image_url")
            String imageUrl,

            @JsonProperty("creator_username")
            String creatorUsername,

            @JsonProperty("has_image")
            boolean hasImage
    ) {
        /**
         * Factory method to create ParentPreview from a Message entity.
         * Applies image-over-text priority logic.
         * 
         * @param parent the parent Message entity
         * @param creatorUsername the username of the parent message's sender
         * @return ParentPreview with appropriate content/image based on priority rules
         */
        public static ParentPreview fromMessage(
                com.example.chat_service.domain.messages.Message parent,
                String creatorUsername
        ) {
            boolean parentHasImage = parent.hasImage();
            return new ParentPreview(
                    parentHasImage ? null : parent.content(),
                    parentHasImage ? parent.imageUrl() : null,
                    creatorUsername,
                    parentHasImage
            );
        }
    }

    /**
     * Factory method to create a MessageCommandActionsResponse from a MessageAggregate.
     * 
     * <p>Automatically determines {@code is_mine} by comparing requesterId with message senderId.
     * Serializes {@code status} as uppercase String for API consistency.
     * Image URLs are stored as relative paths; use {@link #withImageUrl(String)},
     * {@link #withSenderProfileImage(String)}, and {@link #withParentImageUrl(String)}
     * to convert to absolute URLs before sending to frontend.</p>
     * 
     * @param message the MessageAggregate containing current message state
     * @param senderUsername the resolved username of the message sender
     * @param senderProfileImage the resolved profile image URL (relative path) of the sender
     * @param parentPreview optional ParentPreview data if this message is a reply (null otherwise)
     * @param requesterId the UUID of the user who performed the action (for is_mine calculation)
     * @return MessageCommandActionsResponse ready for API response
     */
    public static MessageCommandActionsResponse fromMessage(
            com.example.chat_service.domain.messages.MessageAggregate message,
            String senderUsername,
            String senderProfileImage,
            ParentPreview parentPreview,
            UUID requesterId
    ) {
        String relativeImageUrl = message.imageUrl();
        String relativeSenderImage = senderProfileImage;
        
        boolean isMine = message.senderId().equals(requesterId);
        boolean hasImage = message.hasImage();
        boolean isDeleted = message.isDeleted();
        
        return new MessageCommandActionsResponse(
                message.id(),
                message.roomId(),
                message.content(),
                relativeImageUrl != null ? relativeImageUrl : "",
                message.isReply(),
                parentPreview,
                message.createdAt().toString(),  // ISO-8601 timestamp
                isMine,
                message.status().name(),         // "SENT", "RECEIVED", or "SEEN"
                senderUsername != null ? senderUsername : "",
                relativeSenderImage != null ? relativeSenderImage : "",
                hasImage,
                isDeleted
        );
    }

    /**
     * Convenience factory for testing or mock scenarios.
     * 
     * @param id the message UUID
     * @param roomId the room UUID
     * @param content the message text
     * @param senderUsername the sender's display username
     * @param isMine whether this message belongs to the current user
     * @param status the message status as String ("SENT", "RECEIVED", "SEEN")
     * @return MessageCommandActionsResponse for testing purposes
     */
    public static MessageCommandActionsResponse forTesting(
            UUID id,
            UUID roomId,
            String content,
            String senderUsername,
            boolean isMine,
            String status
    ) {
        return new MessageCommandActionsResponse(
                id,
                roomId,
                content,
                "",
                false,
                null,
                Instant.now().toString(),
                isMine,
                status,
                senderUsername,
                "",
                false,
                false
        );
    }

    /**
     * Create a new DTO instance with an updated imageUrl.
     * 
     * <p>Used to convert relative paths (from domain/DB) to absolute URLs
     * (for frontend consumption) without modifying the original immutable record.</p>
     * 
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // DTO from domain has: imageUrl = "/uploads/messages/abc.jpg"
     * MessageCommandActionsResponse response = MessageCommandActionsResponse.fromMessage(...);
     * 
     * // Convert to absolute URL for API response
     * response = response.withImageUrl("http://127.0.0.1:8005/uploads/messages/abc.jpg");
     * 
     * // Response JSON now contains:
     * // "image_url": "http://127.0.0.1:8005/uploads/messages/abc.jpg",
     * // "has_image": true
     * }</pre>
     * </p>
     * 
     * @param newImageUrl the absolute URL to use, or null/blank to clear
     * @return new MessageCommandActionsResponse instance with updated imageUrl and recalculated hasImage
     */
    public MessageCommandActionsResponse withImageUrl(String newImageUrl) {
        return new MessageCommandActionsResponse(
                this.id,
                this.roomId,
                this.content,
                newImageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,
                this.senderProfileImage,
                // Recalculate hasImage based on new value
                newImageUrl != null && !newImageUrl.isBlank(),
                this.isDeleted
        );
    }

    /**
     * Create a new DTO instance with an updated senderProfileImage.
     * 
     * <p>Used to convert relative paths to absolute URLs for sender avatar display.</p>
     * 
     * @param newSenderProfileImage the absolute URL to use, or null/blank to clear
     * @return new MessageCommandActionsResponse instance with updated senderProfileImage
     */
    public MessageCommandActionsResponse withSenderProfileImage(String newSenderProfileImage) {
        return new MessageCommandActionsResponse(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,
                newSenderProfileImage,
                this.hasImage,
                this.isDeleted
        );
    }

    /**
     * Create a new DTO instance with an updated parent preview image URL.
     * 
     * <p>Used when parentPreview is present and its image URL needs conversion
     * from relative to absolute path.</p>
     * 
     * @param newParentImageUrl the absolute URL for parent image, or null to clear
     * @return new MessageCommandActionsResponse instance with updated parentPreview.imageUrl
     */
    public MessageCommandActionsResponse withParentImageUrl(String newParentImageUrl) {
        if (this.parentPreview == null) {
            return this; // No parent preview to update
        }
        ParentPreview updatedPreview = new ParentPreview(
                this.parentPreview.content(),
                newParentImageUrl,
                this.parentPreview.creatorUsername(),
                newParentImageUrl != null && !newParentImageUrl.isBlank()
        );
        return new MessageCommandActionsResponse(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                updatedPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,
                this.senderProfileImage,
                this.hasImage,
                this.isDeleted
        );
    }

    /**
     * Create a new DTO instance with an updated status.
     * 
     * <p>Useful for real-time status updates (e.g., WebSocket push when message is seen).</p>
     * 
     * @param newStatus the new status as String ("SENT", "RECEIVED", or "SEEN")
     * @return new MessageCommandActionsResponse instance with updated status
     */
    public MessageCommandActionsResponse withStatus(String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("status cannot be null or blank");
        }
        // Validate status value
        try {
            com.example.chat_service.domain.messages.Message.Status.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Must be SENT, RECEIVED, or SEEN");
        }
        return new MessageCommandActionsResponse(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                newStatus.toUpperCase(),
                this.senderUsername,
                this.senderProfileImage,
                this.hasImage,
                this.isDeleted
        );
    }

    /**
     * Create a new DTO instance with an updated deleted state.
     * 
     * <p>Useful for optimistic UI updates before server confirmation.</p>
     * 
     * @param newDeleted the new deleted state
     * @return new MessageCommandActionsResponse instance with updated is_deleted
     */
    public MessageCommandActionsResponse withDeleted(boolean newDeleted) {
        return new MessageCommandActionsResponse(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,
                this.senderProfileImage,
                this.hasImage,
                newDeleted
        );
    }

    /**
     * Convert to Map for logging, testing, or manual serialization.
     * 
     * <p>Not required for Spring MVC responses (Jackson handles records automatically),
     * but useful for debugging, audit logging, or non-JSON use cases.</p>
     * 
     * @return immutable Map representation of this DTO
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("room_id", roomId.toString());
        map.put("content", content);
        map.put("image_url", imageUrl);
        map.put("is_reply", isReply);
        map.put("parent_preview", parentPreview != null ? parentPreviewToMap(parentPreview) : null);
        map.put("created_at", createdAt);
        map.put("is_mine", isMine);
        map.put("status", status);
        map.put("sender_username", senderUsername);
        map.put("sender_profile_image", senderProfileImage);
        map.put("has_image", hasImage);
        map.put("is_deleted", isDeleted);
        return Map.copyOf(map); // Return immutable copy
    }

    /**
     * Nested helper to convert ParentPreview to Map.
     * 
     * @param preview the ParentPreview to convert
     * @return Map representation or null if preview is null
     */
    private static Map<String, Object> parentPreviewToMap(ParentPreview preview) {
        if (preview == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("content", preview.content());
        map.put("image_url", preview.imageUrl());
        map.put("creator_username", preview.creatorUsername());
        map.put("has_image", preview.hasImage());
        return Map.copyOf(map); // Return immutable copy
    }
}