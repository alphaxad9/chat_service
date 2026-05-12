// chat_service/src/main/java/com/example/chat_service/domain/messages/MessageAggregate.java
package com.example.chat_service.domain.messages;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ← Imports for exceptions in sub-package
import com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageContentError;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageImageError;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageParentError;
import com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError;
import com.example.chat_service.domain.messages.exceptions.MessageStateTransitionError;
import com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError;
import com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedErrorWithNoId;
import static java.util.Objects.requireNonNull;

/**
 * Aggregate root for managing the lifecycle and state of a Message.
 * Enforces business rules, coordinates state transitions, guards operations,
 * and validates authorization for user-initiated actions.
 * 
 * <p>Authorization rules:
 * <ul>
 *   <li>Status updates (RECEIVED/SEEN): actor must be the receiver (NOT the sender)</li>
 *   <li>Content/image edits, delete/restore: actor must be the sender (owner)</li>
 * </ul>
 * </p>
 */
public final class MessageAggregate {

    private Message message; // Mutable reference to current state; Message itself is immutable

    private MessageAggregate(Message message) {
        this.message = requireNonNull(message, "message cannot be null");
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public Message message() {
        return message;
    }

    // ── Factory Methods ──────────────────────────────────────────────

    /**
     * Create an aggregate from an existing Message entity (e.g., loaded from repository).
     */
    public static MessageAggregate fromEntity(Message message) {
        return new MessageAggregate(message);
    }

    /**
     * Create a new message aggregate (no image, not a reply).
     */
    public static MessageAggregate createNew(
            UUID id,
            UUID roomId,
            UUID senderId,
            String content,
            LocalDateTime createdAt
    ) {
        validateMessageCreation(id, roomId, senderId, content, null, null);
        Message newMessage = Message.create(id, roomId, senderId, content);
        return new MessageAggregate(newMessage);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static MessageAggregate createNew(UUID id, UUID roomId, UUID senderId, String content) {
        return createNew(id, roomId, senderId, content, null);
    }

    /**
     * Create a new message aggregate with image attachment.
     */
    public static MessageAggregate createNewWithImage(
            UUID id,
            UUID roomId,
            UUID senderId,
            String content,
            String imageUrl,
            LocalDateTime createdAt
    ) {
        validateMessageCreation(id, roomId, senderId, content, imageUrl, null);
        Message newMessage = Message.createWithImage(id, roomId, senderId, content, imageUrl);
        return new MessageAggregate(newMessage);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static MessageAggregate createNewWithImage(UUID id, UUID roomId, UUID senderId, String content, String imageUrl) {
        return createNewWithImage(id, roomId, senderId, content, imageUrl, null);
    }

    /**
     * Create a new reply message aggregate (no image).
     */
    public static MessageAggregate createNewReply(
            UUID id,
            UUID roomId,
            UUID senderId,
            String content,
            UUID parentId,
            LocalDateTime createdAt
    ) {
        validateMessageCreation(id, roomId, senderId, content, null, parentId);
        Message newMessage = Message.createReply(id, roomId, senderId, content, parentId);
        return new MessageAggregate(newMessage);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static MessageAggregate createNewReply(UUID id, UUID roomId, UUID senderId, String content, UUID parentId) {
        return createNewReply(id, roomId, senderId, content, parentId, null);
    }

    /**
     * Create a new reply message aggregate with image attachment.
     */
    public static MessageAggregate createNewReplyWithImage(
            UUID id,
            UUID roomId,
            UUID senderId,
            String content,
            UUID parentId,
            String imageUrl,
            LocalDateTime createdAt
    ) {
        validateMessageCreation(id, roomId, senderId, content, imageUrl, parentId);
        Message newMessage = Message.createReplyWithImage(id, roomId, senderId, content, parentId, imageUrl);
        return new MessageAggregate(newMessage);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static MessageAggregate createNewReplyWithImage(UUID id, UUID roomId, UUID senderId, String content, UUID parentId, String imageUrl) {
        return createNewReplyWithImage(id, roomId, senderId, content, parentId, imageUrl, null);
    }

    /**
     * Validate message creation parameters before entity instantiation.
     */
    private static void validateMessageCreation(UUID id, UUID roomId, UUID senderId, String content, String imageUrl, UUID parentId) {
        if (id == null) {
            throw new InvalidMessageEntityError(null, roomId, senderId, "Message ID cannot be null");
        }
        if (roomId == null) {
            throw new InvalidMessageEntityError(id, null, senderId, "Room ID cannot be null");
        }
        if (senderId == null) {
            throw new InvalidMessageEntityError(id, roomId, null, "Sender ID cannot be null");
        }
        if (content == null || content.isBlank()) {
            throw new InvalidMessageContentError(id, content, content != null ? content.length() : null, 10000, "Content cannot be empty");
        }
        if (content.length() > 10000) {
            throw new InvalidMessageContentError(id, content, content.length(), 10000, "Content cannot exceed 10000 characters");
        }
        if (imageUrl != null && imageUrl.isBlank()) {
            throw new InvalidMessageImageError(id, imageUrl, "Image URL cannot be blank if provided");
        }
        if (parentId != null && parentId.equals(id)) {
            throw new InvalidMessageParentError(id, parentId, "Message cannot reference itself as parent");
        }
    }

    // ── Business Operations ──────────────────────────────────────────

    /**
     * Update message status to RECEIVED.
     * REQUIRES: actorId must NOT be the sender (receiver is marking as received).
     * @param actorId ID of the user performing the action
     */
    public MessageAggregate markAsReceived(UUID actorId) {
        ensureActive("mark_as_received");
        ensureNotSender(actorId, "mark_as_received");
        
        if (message.status() == Message.Status.RECEIVED) {
            return this; // Already received, no-op
        }
        if (message.status() == Message.Status.SEEN) {
            throw new MessageStateTransitionError(
                message.id(),
                message.status().name(),
                Message.Status.RECEIVED.name(),
                "Cannot mark as RECEIVED after message is already SEEN"
            );
        }
        
        this.message = message.withStatus(Message.Status.RECEIVED);
        return this;
    }

    /**
     * Update message status to SEEN.
     * REQUIRES: actorId must NOT be the sender (receiver is marking as read).
     * @param actorId ID of the user performing the action
     */
    public MessageAggregate markAsSeen(UUID actorId) {
        ensureActive("mark_as_seen");
        ensureNotSender(actorId, "mark_as_seen");
        
        if (message.status() == Message.Status.SEEN) {
            return this; // Already seen, no-op
        }
        if (message.status() == Message.Status.SENT) {
            throw new MessageStateTransitionError(
                message.id(),
                message.status().name(),
                Message.Status.SEEN.name(),
                "Cannot transition directly from SENT to SEEN; must be RECEIVED first"
            );
        }
        
        this.message = message.withStatus(Message.Status.SEEN);
        return this;
    }

    /**
     * Update message content.
     * REQUIRES: actorId must be the sender (owner).
     * @param newContent the new message text
     * @param actorId ID of the user performing the edit
     */
    public MessageAggregate withContent(String newContent, UUID actorId) {
        ensureActive("update_content");
        ensureSender(actorId, "update_content");
        
        if (newContent == null || newContent.isBlank()) {
            throw new InvalidMessageContentError(
                message.id(),
                newContent,
                newContent != null ? newContent.length() : null,
                10000,
                "Content cannot be empty"
            );
        }
        if (newContent.length() > 10000) {
            throw new InvalidMessageContentError(
                message.id(),
                newContent,
                newContent.length(),
                10000,
                "Content cannot exceed 10000 characters"
            );
        }
        
        this.message = message.withContent(newContent);
        return this;
    }

    /**
     * Update or set the image URL. Pass null to remove the image attachment.
     * REQUIRES: actorId must be the sender (owner).
     * @param newImageUrl the new image URL/path or null to clear
     * @param actorId ID of the user performing the update
     */
    public MessageAggregate withImage(String newImageUrl, UUID actorId) {
        ensureActive("update_image");
        ensureSender(actorId, "update_image");
        
        if (newImageUrl != null && newImageUrl.isBlank()) {
            throw new InvalidMessageImageError(
                message.id(),
                newImageUrl,
                "Image URL cannot be blank if provided"
            );
        }
        
        this.message = message.withImage(newImageUrl);
        return this;
    }

    /**
     * Soft-delete the message.
     * REQUIRES: actorId must be the sender (owner).
     * @param actorId ID of the user attempting to delete the message
     */
    public MessageAggregate delete(UUID actorId) {
        ensureActive("delete");
        ensureSender(actorId, "delete");
        
        if (message.isDeleted()) {
            throw new MessageStateTransitionError(
                message.id(),
                "active",
                "deleted",
                "Message is already deleted"
            );
        }
        
        this.message = message.toggleDeletion();
        return this;
    }

    /**
     * Restore a soft-deleted message.
     * REQUIRES: actorId must be the sender (owner).
     * @param actorId ID of the user attempting to restore the message
     */
    public MessageAggregate restore(UUID actorId) {
        ensureInactive("restore");
        ensureSender(actorId, "restore");
        
        if (!message.isDeleted()) {
            throw new MessageStateTransitionError(
                message.id(),
                "deleted",
                "active",
                "Message is not deleted"
            );
        }
        
        this.message = message.toggleDeletion();
        return this;
    }

    /**
     * Update only the updatedAt timestamp (metadata refresh without state change).
     * REQUIRES: actorId must be the sender (owner).
     * @param actorId ID of the user performing the touch
     */
    public MessageAggregate touch(UUID actorId) {
        ensureActive("touch");
        ensureSender(actorId, "touch");
        
        this.message = message.touch();
        return this;
    }

    /**
     * Internal touch for system use (no ownership check).
     * Use sparingly - prefer explicit actorId version for audit trails.
     */
    public MessageAggregate touchInternal() {
        ensureActive("touch_internal");
        this.message = message.touch();
        return this;
    }

    // ── State Queries (delegated to Message) ─────────────────────────

    public boolean isActive() {
        return message.isActive();
    }

    public boolean hasImage() {
        return message.hasImage();
    }

    public boolean isReply() {
        return message.isReply();
    }

    public boolean isSeen() {
        return message.isSeen();
    }

    public boolean isReceived() {
        return message.isReceived();
    }

    public boolean isSent() {
        return message.isSent();
    }

    public UUID id() { return message.id(); }
    public UUID roomId() { return message.roomId(); }
    public UUID senderId() { return message.senderId(); }
    public String content() { return message.content(); }
    public String imageUrl() { return message.imageUrl(); }
    public UUID parentId() { return message.parentId(); }
    public Message.Status status() { return message.status(); }
    public LocalDateTime seenAt() { return message.seenAt(); }
    public LocalDateTime createdAt() { return message.createdAt(); }
    public LocalDateTime updatedAt() { return message.updatedAt(); }
    public boolean isDeleted() { return message.isDeleted(); }

    // ── Helper Methods ───────────────────────────────────────────────

    /**
     * Verify that the actor is the sender/owner of this message.
     * Throws MessageUnauthorizedError if IDs don't match.
     * @param actorId ID of the user attempting the operation
     * @param operation Name of the operation for error context
     */
    private void ensureSender(UUID actorId, String operation) {
        if (actorId == null) {
            throw new MessageUnauthorizedErrorWithNoId(
                null,
                operation,
                "Actor ID cannot be null for sender check"
            );
        }
        if (!actorId.equals(message.senderId())) {
            throw new MessageUnauthorizedError(
                message.id(),
                actorId,
                operation,
                "User " + actorId + " is not the sender of message " + message.id() + " and cannot perform '" + operation + "'"
            );
        }
    }

    /**
     * Verify that the actor is NOT the sender (i.e., is the receiver).
     * Throws MessageUnauthorizedError if actor IS the sender.
     * @param actorId ID of the user attempting the operation
     * @param operation Name of the operation for error context
     */
    private void ensureNotSender(UUID actorId, String operation) {
        if (actorId == null) {
            throw new MessageUnauthorizedErrorWithNoId(
                null,
                operation,
                "Actor ID cannot be null for receiver check"
            );
        }
        if (actorId.equals(message.senderId())) {
            throw new MessageUnauthorizedError(
                message.id(),
                actorId,
                operation,
                "Sender " + actorId + " cannot mark their own message as '" + operation + "'; this action is for the receiver"
            );
        }
    }

    private void ensureActive(String operation) {
        if (!message.isActive()) {
            throw new MessageOperationNotAllowedError(
                message.id(),
                operation,
                "Message is deleted or inactive"
            );
        }
    }

    private void ensureInactive(String operation) {
        if (message.isActive()) {
            throw new MessageOperationNotAllowedError(
                message.id(),
                operation,
                "Message is active; this operation requires a deleted message"
            );
        }
    }

    // ── Standard Object Methods ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageAggregate that)) return false;
        return Objects.equals(message.id(), that.message.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(message.id());
    }

    @Override
    public String toString() {
        return "MessageAggregate{" +
                "id=" + message.id() +
                ", roomId=" + message.roomId() +
                ", senderId=" + message.senderId() +
                ", contentLength=" + message.content().length() +
                ", hasImage=" + message.hasImage() +
                ", isReply=" + message.isReply() +
                ", status=" + message.status() +
                ", isSeen=" + message.isSeen() +
                ", createdAt=" + message.createdAt() +
                ", isActive=" + message.isActive() +
                '}';
    }
}