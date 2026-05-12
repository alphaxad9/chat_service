package com.example.chat_service.application.messages.handlers.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing a message for query/list API responses.
 * 
 * <p>Designed for read-side operations: fetching message history, loading chat threads,
 * displaying message details. Contains:
 * <ul>
 *   <li>Message core data (id, room_id, content, image_url)</li>
 *   <li>Reply context (is_reply, parent preview with content/image and creator username)</li>
 *   <li>Sender info (username and profile image for display)</li>
 *   <li>State flags (is_mine, status, has_image, is_deleted)</li>
 *   <li>Timestamps (created_at, updated_at, seen_at)</li>
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
 *   <li>When {@code is_mine=true}, {@code sender_username} is set to "You" for personalized UX.</li>
 *   <li>Image URL fields contain RELATIVE paths from domain/DB (e.g. {@code /uploads/messages/abc.jpg}).
 *       Use {@link #withImageUrl(String)}, {@link #withSenderProfileImage(String)}, 
 *       and {@link #withParentImageUrl(String)} to convert to absolute URLs before sending HTTP response.</li>
 *   <li>All fields use {@code @JsonProperty} for consistent snake_case JSON output.</li>
 *   <li>For soft-deleted messages ({@code is_deleted=true}), frontend should render placeholder UI (e.g., "This message was deleted").</li>
 *   <li>{@code status} is serialized as uppercase String ("SENT", "RECEIVED", "SEEN") for API clarity.</li>
 * </ul>
 * </p>
 * 
 * <p>Usage example in service/controller layer:
 * <pre>{@code
 *   // Query-side: load message from repository
 *   Message message = messageQueryRepository.findById(messageId)
 *       .orElseThrow(() -> new MessageNotFoundException(messageId));
 *   
 *   // Enrich with external data
 *   UserView sender = authClient.getUserView(message.senderId());
 *   String senderUsername = sender.username();
 *   String senderProfileImage = sender.profilePicture();
 *   
 *   // Build parent preview if this is a reply
 *   MessageQueryResponseDTO.ParentPreview parentPreview = null;
 *   if (message.isReply() && message.parentId() != null) {
 *       Message parent = messageQueryRepository.findById(message.parentId()).orElse(null);
 *       if (parent != null) {
 *           String parentCreatorUsername = authClient.getUsername(parent.senderId());
 *           parentPreview = MessageQueryResponseDTO.ParentPreview.fromMessage(parent, parentCreatorUsername);
 *       }
 *   }
 *   
 *   // Build response DTO - sender_username will be "You" if is_mine=true
 *   MessageQueryResponseDTO response = MessageQueryResponseDTO.fromMessage(
 *       message,
 *       senderUsername,
 *       senderProfileImage,
 *       parentPreview,
 *       requesterId  // for is_mine calculation
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
public record MessageQueryResponseDTO(

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
        boolean isDeleted,

        @JsonProperty("updated_at")
        String updatedAt,

        @JsonProperty("seen_at")
        String seenAt

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
     * Factory method to create a MessageQueryResponseDTO from a Message domain object.
     * 
     * <p>Automatically determines {@code is_mine} by comparing requesterId with message senderId.
     * <strong>When {@code is_mine=true}, {@code sender_username} is set to "You"</strong> for personalized UX.
     * Serializes {@code status} as uppercase String for API consistency.
     * Image URLs are stored as relative paths; use {@link #withImageUrl(String)},
     * {@link #withSenderProfileImage(String)}, and {@link #withParentImageUrl(String)}
     * to convert to absolute URLs before sending to frontend.</p>
     * 
     * @param message the Message domain object containing current message state
     * @param senderUsername the resolved username of the message sender
     * @param senderProfileImage the resolved profile image URL (relative path) of the sender
     * @param parentPreview optional ParentPreview data if this message is a reply (null otherwise)
     * @param requesterId the UUID of the user who requested this message (for is_mine calculation)
     * @return MessageQueryResponseDTO ready for API response
     */
    public static MessageQueryResponseDTO fromMessage(
            com.example.chat_service.domain.messages.Message message,
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
        
        // Format timestamps as ISO-8601 strings
        String createdAtStr = message.createdAt() != null 
            ? message.createdAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        String updatedAtStr = message.updatedAt() != null 
            ? message.updatedAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        String seenAtStr = message.seenAt() != null 
            ? message.seenAt().format(DateTimeFormatter.ISO_DATE_TIME) 
            : null;
        
        // Personalize sender username: show "You" when the requester is the sender
        String displaySenderUsername = isMine ? "You" : (senderUsername != null ? senderUsername : "");
        
        return new MessageQueryResponseDTO(
                message.id(),
                message.roomId(),
                message.content(),
                relativeImageUrl != null ? relativeImageUrl : "",
                message.isReply(),
                parentPreview,
                createdAtStr,
                isMine,
                message.status().name(),         // "SENT", "RECEIVED", or "SEEN"
                displaySenderUsername,           // "You" if is_mine, else actual username
                relativeSenderImage != null ? relativeSenderImage : "",
                hasImage,
                isDeleted,
                updatedAtStr,
                seenAtStr
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
     * @return MessageQueryResponseDTO for testing purposes
     */
    public static MessageQueryResponseDTO forTesting(
            UUID id,
            UUID roomId,
            String content,
            String senderUsername,
            boolean isMine,
            String status
    ) {
        String now = java.time.Instant.now().toString();
        // Apply "You" logic for testing consistency
        String displayUsername = isMine ? "You" : (senderUsername != null ? senderUsername : "");
        
        return new MessageQueryResponseDTO(
                id,
                roomId,
                content,
                "",
                false,
                null,
                now,
                isMine,
                status,
                displayUsername,
                "",
                false,
                false,
                now,
                null
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
     * MessageQueryResponseDTO response = MessageQueryResponseDTO.fromMessage(...);
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
     * @return new MessageQueryResponseDTO instance with updated imageUrl and recalculated hasImage
     */
    public MessageQueryResponseDTO withImageUrl(String newImageUrl) {
        return new MessageQueryResponseDTO(
                this.id,
                this.roomId,
                this.content,
                newImageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,  // Keep existing senderUsername (already personalized)
                this.senderProfileImage,
                // Recalculate hasImage based on new value
                newImageUrl != null && !newImageUrl.isBlank(),
                this.isDeleted,
                this.updatedAt,
                this.seenAt
        );
    }

    /**
     * Create a new DTO instance with an updated senderProfileImage.
     * 
     * <p>Used to convert relative paths to absolute URLs for sender avatar display.</p>
     * 
     * @param newSenderProfileImage the absolute URL to use, or null/blank to clear
     * @return new MessageQueryResponseDTO instance with updated senderProfileImage
     */
    public MessageQueryResponseDTO withSenderProfileImage(String newSenderProfileImage) {
        return new MessageQueryResponseDTO(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,  // Keep existing senderUsername (already personalized)
                newSenderProfileImage,
                this.hasImage,
                this.isDeleted,
                this.updatedAt,
                this.seenAt
        );
    }

    /**
     * Create a new DTO instance with an updated parent preview image URL.
     * 
     * <p>Used when parentPreview is present and its image URL needs conversion
     * from relative to absolute path.</p>
     * 
     * @param newParentImageUrl the absolute URL for parent image, or null to clear
     * @return new MessageQueryResponseDTO instance with updated parentPreview.imageUrl
     */
    public MessageQueryResponseDTO withParentImageUrl(String newParentImageUrl) {
        if (this.parentPreview == null) {
            return this; // No parent preview to update
        }
        ParentPreview updatedPreview = new ParentPreview(
                this.parentPreview.content(),
                newParentImageUrl,
                this.parentPreview.creatorUsername(),
                newParentImageUrl != null && !newParentImageUrl.isBlank()
        );
        return new MessageQueryResponseDTO(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                updatedPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,  // Keep existing senderUsername (already personalized)
                this.senderProfileImage,
                this.hasImage,
                this.isDeleted,
                this.updatedAt,
                this.seenAt
        );
    }

    /**
     * Create a new DTO instance with an updated status.
     * 
     * <p>Useful for real-time status updates (e.g., WebSocket push when message is seen).</p>
     * 
     * @param newStatus the new status as String ("SENT", "RECEIVED", or "SEEN")
     * @return new MessageQueryResponseDTO instance with updated status
     */
    public MessageQueryResponseDTO withStatus(String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("status cannot be null or blank");
        }
        // Validate status value
        try {
            com.example.chat_service.domain.messages.Message.Status.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Must be SENT, RECEIVED, or SEEN");
        }
        return new MessageQueryResponseDTO(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                newStatus.toUpperCase(),
                this.senderUsername,  // Keep existing senderUsername (already personalized)
                this.senderProfileImage,
                this.hasImage,
                this.isDeleted,
                this.updatedAt,
                this.seenAt
        );
    }

    /**
     * Create a new DTO instance with an updated deleted state.
     * 
     * <p>Useful for optimistic UI updates before server confirmation.</p>
     * 
     * @param newDeleted the new deleted state
     * @return new MessageQueryResponseDTO instance with updated is_deleted
     */
    public MessageQueryResponseDTO withDeleted(boolean newDeleted) {
        return new MessageQueryResponseDTO(
                this.id,
                this.roomId,
                this.content,
                this.imageUrl,
                this.isReply,
                this.parentPreview,
                this.createdAt,
                this.isMine,
                this.status,
                this.senderUsername,  // Keep existing senderUsername (already personalized)
                this.senderProfileImage,
                this.hasImage,
                newDeleted,
                this.updatedAt,
                this.seenAt
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
        map.put("updated_at", updatedAt);
        map.put("seen_at", seenAt);
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