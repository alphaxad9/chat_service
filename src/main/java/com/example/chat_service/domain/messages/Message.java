// chat_service/src/main/java/com/example/chat_service/domain/messages/Message.java
package com.example.chat_service.domain.messages;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing a Message aggregate.
 * Manages message content, optional image attachment, delivery status, replies, and timestamps.
 * 
 * <p>Constructor is public to allow infrastructure mapping from persistence layer.
 * Validation is enforced in constructor, so instantiation is always safe.</p>
 */
public final class Message {

    private final UUID id;                        // Message ID (primary key)
    private final UUID roomId;                    // Reference to the room this message belongs to
    private final UUID senderId;                  // Reference to the user who sent the message
    private final String content;                 // Message text content (required)
    private final String imageUrl;                // Optional single image URL/path (nullable)
    private final UUID parentId;                  // Reference to parent message if this is a reply (nullable)
    private final Status status;                  // Delivery status: SENT, RECEIVED, or SEEN
    private final LocalDateTime seenAt;           // Timestamp when message was seen (nullable)
    private final LocalDateTime createdAt;        // Message creation timestamp
    private final LocalDateTime updatedAt;        // Last update timestamp
    private final boolean isDeleted;              // Soft delete flag

    // ── Status Enum ──────────────────────────────────────────────────
    public enum Status {
        SENT,      // Message sent to server
        RECEIVED,  // Message delivered to recipient's device
        SEEN       // Message read by recipient
    }

    // ── Constructor with validation ──────────────────────────────────
    /**
     * Public constructor for domain creation and infrastructure mapping.
     * All arguments are validated to ensure domain invariants.
     */
    public Message(UUID id, UUID roomId, UUID senderId, String content, String imageUrl,
                   UUID parentId, Status status, LocalDateTime seenAt,
                   LocalDateTime createdAt, LocalDateTime updatedAt, boolean isDeleted) {
        
        // Validate required fields
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (roomId == null) throw new IllegalArgumentException("roomId cannot be null");
        if (senderId == null) throw new IllegalArgumentException("senderId cannot be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        if (content.length() > 10000) {
            throw new IllegalArgumentException("content cannot exceed 10000 characters");
        }
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");

        // Image URL validation (if provided, must not be blank)
        if (imageUrl != null && imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl cannot be blank if provided");
        }

        // Parent ID validation: cannot reference itself
        if (parentId != null && Objects.equals(parentId, id)) {
            throw new IllegalArgumentException("parentId cannot reference the same message");
        }

        // Status invariant: if SEEN, seenAt must be provided
        if (status == Status.SEEN && seenAt == null) {
            throw new IllegalArgumentException("seenAt is required when status is SEEN");
        }
        // If not SEEN, seenAt should be null (enforce consistency)
        if (status != Status.SEEN && seenAt != null) {
            throw new IllegalArgumentException("seenAt should be null when status is not SEEN");
        }

        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.parentId = parentId;
        this.status = status;
        this.seenAt = seenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    // ── Factory Methods ─────────────────────────────────────────────
    
    /**
     * Create a new message with SENT status (no image, not a reply).
     */
    public static Message create(UUID id, UUID roomId, UUID senderId, String content) {
        LocalDateTime now = LocalDateTime.now();
        return new Message(id, roomId, senderId, content, null, null,
                          Status.SENT, null, now, now, false);
    }

    /**
     * Create a new message with SENT status and an image attachment (not a reply).
     * @param imageUrl URL/path to the single image attachment
     */
    public static Message createWithImage(UUID id, UUID roomId, UUID senderId, 
                                          String content, String imageUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new Message(id, roomId, senderId, content, imageUrl, null,
                          Status.SENT, null, now, now, false);
    }

    /**
     * Create a new reply message with SENT status (no image).
     * @param parentId the ID of the message being replied to
     */
    public static Message createReply(UUID id, UUID roomId, UUID senderId, 
                                      String content, UUID parentId) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId cannot be null for reply messages");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Message(id, roomId, senderId, content, null, parentId,
                          Status.SENT, null, now, now, false);
    }

    /**
     * Create a new reply message with SENT status and an image attachment.
     * @param parentId the ID of the message being replied to
     * @param imageUrl URL/path to the single image attachment
     */
    public static Message createReplyWithImage(UUID id, UUID roomId, UUID senderId, 
                                               String content, UUID parentId, String imageUrl) {
        if (parentId == null) {
            throw new IllegalArgumentException("parentId cannot be null for reply messages");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Message(id, roomId, senderId, content, imageUrl, parentId,
                          Status.SENT, null, now, now, false);
    }

    // ── Getters (no setters - immutable) ───────────────────────────
    public UUID id() { return id; }
    public UUID roomId() { return roomId; }
    public UUID senderId() { return senderId; }
    public String content() { return content; }
    public String imageUrl() { return imageUrl; }
    public UUID parentId() { return parentId; }
    public Status status() { return status; }
    public LocalDateTime seenAt() { return seenAt; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public boolean isDeleted() { return isDeleted; }

    // ── State Queries ──────────────────────────────────────────────
    public boolean isActive() {
        return !isDeleted;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    public boolean isReply() {
        return parentId != null;
    }

    public boolean isSeen() {
        return status == Status.SEEN;
    }

    public boolean isReceived() {
        return status == Status.RECEIVED;
    }

    public boolean isSent() {
        return status == Status.SENT;
    }

    // ── State Transformers (return new instance) ───────────────────
    
    /**
     * Update message status. Automatically sets seenAt when transitioning to SEEN.
     * @param newStatus the new delivery status
     * @return new Message instance with updated status and timestamp
     * @throws IllegalArgumentException if status transition is invalid
     */
    public Message withStatus(Status newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus cannot be null");
        }
        if (Objects.equals(this.status, newStatus)) {
            return this; // No change needed
        }
        
        // Enforce valid status transitions: SENT -> RECEIVED -> SEEN
        if (this.status == Status.SENT && newStatus == Status.SEEN) {
            throw new IllegalArgumentException("Cannot transition directly from SENT to SEEN");
        }
        if (this.status == Status.RECEIVED && newStatus == Status.SENT) {
            throw new IllegalArgumentException("Cannot downgrade from RECEIVED to SENT");
        }
        if (this.status == Status.SEEN && newStatus != Status.SEEN) {
            throw new IllegalArgumentException("Cannot change status after message is SEEN");
        }
        
        LocalDateTime newSeenAt = (newStatus == Status.SEEN) ? LocalDateTime.now() : this.seenAt;
        
        return new Message(this.id, this.roomId, this.senderId, this.content, this.imageUrl,
                          this.parentId, newStatus, newSeenAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Update message content.
     * @param newContent the new message text (required, 1-10000 chars, non-blank)
     * @return new Message instance with updated content and timestamp
     * @throws IllegalArgumentException if content is invalid
     */
    public Message withContent(String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        if (newContent.length() > 10000) {
            throw new IllegalArgumentException("content cannot exceed 10000 characters");
        }
        if (Objects.equals(this.content, newContent)) {
            return this; // No change needed
        }
        return new Message(this.id, this.roomId, this.senderId, newContent, this.imageUrl,
                          this.parentId, this.status, this.seenAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Update or set the image URL. Pass null to remove the image attachment.
     * @param newImageUrl the new image URL/path or null to clear
     * @return new Message instance with updated image and timestamp
     * @throws IllegalArgumentException if URL is blank (but not null)
     */
    public Message withImage(String newImageUrl) {
        if (newImageUrl != null && newImageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl cannot be blank if provided");
        }
        if (Objects.equals(this.imageUrl, newImageUrl)) {
            return this; // No change needed
        }
        return new Message(this.id, this.roomId, this.senderId, this.content, newImageUrl,
                          this.parentId, this.status, this.seenAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Soft-delete or restore the message.
     */
    public Message toggleDeletion() {
        return new Message(this.id, this.roomId, this.senderId, this.content, this.imageUrl,
                          this.parentId, this.status, this.seenAt, this.createdAt, LocalDateTime.now(), !this.isDeleted);
    }

    /**
     * Update only the updatedAt timestamp (metadata refresh without state change).
     */
    public Message touch() {
        return new Message(this.id, this.roomId, this.senderId, this.content, this.imageUrl,
                          this.parentId, this.status, this.seenAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    // ── Standard Object Methods ────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message message)) return false;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", senderId=" + senderId +
                ", contentLength=" + content.length() +
                ", hasImage=" + hasImage() +
                ", isReply=" + isReply() +
                ", parentId=" + parentId +
                ", status=" + status +
                ", isSeen=" + isSeen() +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive() +
                '}';
    }
}