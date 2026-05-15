package com.example.chat_service.application.messages.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.MessageAggregate;

/**
 * Application-layer interface for message command (write) operations.
 *
 * <p>Orchestrates business logic and coordinates domain aggregates with infrastructure
 * repositories. All methods operate on {@link MessageAggregate} to preserve domain invariants.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Command methods accept IDs — service loads aggregate, applies business logic, persists result</li>
 *   <li>Only {@code createMessage} variants accept pre-built aggregates (for initial construction)</li>
 *   <li>Validation and business rules live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence, caching) are delegated to repositories</li>
 *   <li>Transaction boundaries should be applied at the implementation level</li>
 *   <li>Authorization checks (e.g., sender/receiver validation) should be performed before calling protected operations</li>
 * </ul></p>
 */
public interface MessageCommandServiceInterface {

    // ── Core Lifecycle Commands ────────────────────────────────────────

    /**
     * Create a new message aggregate (text-only, not a reply) and persist it.
     *
     * @param aggregate the validated message aggregate to create (must be new, non-reply, no image)
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError
     *         if required fields (id, roomId, senderId, content) are null or invalid
     * @throws com.example.chat_service.domain.messages.exceptions.MessageAlreadyExistsError
     *         if a message with the given ID already exists
     */
    MessageAggregate createMessage(MessageAggregate aggregate);

    /**
     * Create a new message aggregate with image attachment and persist it.
     *
     * @param aggregate the validated message aggregate to create (must have image, not a reply)
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError
     *         if required fields are null/invalid or imageUrl is blank
     * @throws com.example.chat_service.domain.messages.exceptions.MessageAlreadyExistsError
     *         if a message with the given ID already exists
     */
    MessageAggregate createMessageWithImage(MessageAggregate aggregate);

    /**
     * Create a new reply message aggregate (text-only) and persist it.
     *
     * @param aggregate the validated message aggregate to create (must be a reply, no image)
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError
     *         if required fields are null/invalid or parentId is missing/invalid
     * @throws com.example.chat_service.domain.messages.exceptions.MessageAlreadyExistsError
     *         if a message with the given ID already exists
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if the parent message does not exist
     */
    MessageAggregate createReplyMessage(MessageAggregate aggregate);

    /**
     * Create a new reply message aggregate with image attachment and persist it.
     *
     * @param aggregate the validated message aggregate to create (must be a reply with image)
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError
     *         if required fields are null/invalid or imageUrl/parentId is invalid
     * @throws com.example.chat_service.domain.messages.exceptions.MessageAlreadyExistsError
     *         if a message with the given ID already exists
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if the parent message does not exist
     */
    MessageAggregate createReplyMessageWithImage(MessageAggregate aggregate);

    /**
     * Soft-delete a message (sender-initiated action).
     *
     * <p>Service loads aggregate by ID, verifies ownership via actorId (must be sender),
     * applies domain logic, and persists the updated state.</p>
     *
     * @param messageId the UUID of the message to delete
     * @param actorId ID of the user attempting to delete (must match message's senderId)
     * @return the updated aggregate with {@code isDeleted = true}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId does not match the message's senderId
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is already deleted or inactive
     */
    MessageAggregate deleteMessage(UUID messageId, UUID actorId);

    /**
     * Restore a soft-deleted message (sender-initiated action).
     *
     * <p>Service loads aggregate by ID, verifies ownership via actorId (must be sender),
     * applies domain logic, and persists the updated state.</p>
     *
     * @param messageId the UUID of the message to restore
     * @param actorId ID of the user attempting to restore (must match message's senderId)
     * @return the updated aggregate with {@code isDeleted = false}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId does not match the message's senderId
     * @throws com.example.chat_service.domain.messages.exceptions.MessageStateTransitionError
     *         if the message is not in a deleted state
     */
    MessageAggregate restoreMessage(UUID messageId, UUID actorId);

    // ── Delivery Status Commands (receiver-initiated) ──────────────────

    /**
     * Mark a message as RECEIVED (delivered to recipient's device).
     *
     * <p>Service loads aggregate by ID, verifies actor is the receiver (NOT sender),
     * applies domain logic, and persists the updated state.</p>
     *
     * @param messageId the UUID of the message to update
     * @param actorId ID of the user marking as received (must be the receiver, not sender)
     * @return the updated aggregate with refreshed {@code status} and {@code updatedAt}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId matches the sender (only receiver can mark as received)
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is deleted or already in an incompatible state
     * @throws com.example.chat_service.domain.messages.exceptions.MessageStateTransitionError
     *         if status transition is invalid (e.g., SEEN -> RECEIVED)
     */
    MessageAggregate markAsReceived(UUID messageId, UUID actorId);

    /**
     * Mark a message as SEEN (read by recipient).
     *
     * <p>Service loads aggregate by ID, verifies actor is the receiver (NOT sender),
     * applies domain logic, and persists the updated state. Sets {@code seenAt} timestamp.</p>
     *
     * @param messageId the UUID of the message to update
     * @param actorId ID of the user marking as seen (must be the receiver, not sender)
     * @return the updated aggregate with refreshed {@code status}, {@code seenAt}, and {@code updatedAt}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId matches the sender (only receiver can mark as seen)
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is deleted or already in an incompatible state
     * @throws com.example.chat_service.domain.messages.exceptions.MessageStateTransitionError
     *         if status transition is invalid (e.g., SENT -> SEEN without RECEIVED first)
     */
    MessageAggregate markAsSeen(UUID messageId, UUID actorId);

    // ── Content & Media Update Commands (sender-initiated) ─────────────

    /**
     * Update the content/text of a message.
     *
     * <p>Service loads aggregate by ID, verifies caller is the sender, applies domain logic,
     * and persists the updated state. Only valid for active messages.</p>
     *
     * @param messageId the UUID of the message to update
     * @param newContent the new message text (1-10000 chars, non-blank)
     * @param actorId ID of the user performing the edit (must match message's senderId)
     * @return the updated aggregate with refreshed {@code content} and {@code updatedAt}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId does not match the message's senderId
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is deleted or inactive
     * @throws com.example.chat_service.domain.messages.exceptions.InvalidMessageContentError
     *         if newContent is null, blank, or exceeds 10000 characters
     */
    MessageAggregate updateContent(UUID messageId, String newContent, UUID actorId);

    /**
     * Update or set the image URL for a message. Pass null to remove the image attachment.
     *
     * <p>Service loads aggregate by ID, verifies caller is the sender, applies domain logic,
     * and persists the updated state. Only valid for active messages.</p>
     *
     * @param messageId the UUID of the message to update
     * @param newImageUrl the new image URL/path or null to clear
     * @param actorId ID of the user performing the update (must match message's senderId)
     * @return the updated aggregate with refreshed {@code imageUrl} and {@code updatedAt}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId does not match the message's senderId
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is deleted or inactive
     * @throws com.example.chat_service.domain.messages.exceptions.InvalidMessageImageError
     *         if newImageUrl is blank (but not null)
     */
    MessageAggregate updateImage(UUID messageId, String newImageUrl, UUID actorId);

    // ── Activity & Utility Commands ────────────────────────────────────

    /**
     * Touch the aggregate to refresh its {@code updatedAt} timestamp.
     *
     * <p>Useful for cache invalidation, presence tracking, or forcing persistence
     * without changing business-relevant state. Requires sender ownership for audit trails.
     * Service loads aggregate by ID, verifies ownership via actorId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param messageId the UUID of the message to touch
     * @param actorId ID of the user performing the touch (must match message's senderId)
     * @return the updated aggregate with refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError
     *         if actorId does not match the message's senderId
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is inactive or deleted
     */
    MessageAggregate touch(UUID messageId, UUID actorId);

    /**
     * Internal touch for system use (no ownership check).
     *
     * <p>Use sparingly — prefer explicit actorId version for audit trails.
     * Typically used by background jobs or system maintenance tasks.
     * Service loads aggregate by ID, applies domain logic, and persists the updated state.</p>
     *
     * @param messageId the UUID of the message to touch
     * @return the updated aggregate with refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     * @throws com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError
     *         if the message is inactive or deleted
     */
    MessageAggregate touchInternal(UUID messageId);

    // ── Query Support Methods (for command orchestration) ──────────────

    /**
     * Load a message aggregate by its unique ID for mutation.
     *
     * @param messageId the UUID of the message to load
     * @return the loaded aggregate ready for business operations
     * @throws com.example.chat_service.domain.messages.exceptions.MessageNotFoundError
     *         if no message exists with the given ID
     */
    MessageAggregate loadAggregate(UUID messageId);

    /**
     * Load a message aggregate by its unique ID, returning optional.
     *
     * <p>Use this when message existence is uncertain and you want to avoid
     * exception handling for the "not found" case.</p>
     *
     * @param messageId the UUID of the message to load
     * @return {@link Optional} containing the aggregate if found, empty otherwise
     */
    Optional<MessageAggregate> loadAggregateOptional(UUID messageId);

    /**
     * Load a message aggregate by room ID and message ID.
     *
     * <p>Useful for validating that a message belongs to a specific room before operations.</p>
     *
     * @param roomId the room identifier
     * @param messageId the message identifier
     * @return the loaded message aggregate if found and belongs to the room, otherwise {@link Optional#empty()}
     */
    Optional<MessageAggregate> loadByRoomAndId(UUID roomId, UUID messageId);

    /**
     * Check if a message aggregate exists by ID (fast existence check).
     *
     * @param messageId the UUID of the message
     * @return {@code true} if a message exists with the given ID
     */
    boolean aggregateExists(UUID messageId);

    /**
     * Check if a message exists in a specific room.
     *
     * @param roomId the room identifier
     * @param messageId the message identifier
     * @return {@code true} if a message with the given ID exists in the room
     *
     * <p>Useful for validating message-room relationships without loading full state.</p>
     */
    boolean aggregateExistsInRoom(UUID roomId, UUID messageId);

    /**
     * Check whether a sender has any messages in a specific room.
     *
     * @param roomId the room identifier
     * @param senderId the sender identifier
     * @return {@code true} if the sender has any messages in the room
     *
     * <p>Useful for permission checks or UI logic (e.g., "has this user posted in this room?").</p>
     */
    boolean aggregateExistsByRoomAndSender(UUID roomId, UUID senderId);

    // ── Bulk Read Operations (for command orchestration & read models) ─

    /**
     * Bulk load all message aggregates for a specific room.
     *
     * @param roomId the unique identifier of the room
     * @return list of all message aggregates in this room (including deleted messages)
     *
     * <p>Use this when you need to iterate over all messages in a room for operations like:
     * <ul>
     *   <li>Displaying full conversation history</li>
     *   <li>Computing room-level statistics</li>
     *   <li>Exporting or archiving room data</li>
     * </ul>
     *
     * <p>Consider pagination for rooms with many messages in production implementations.</p>
     */
    List<MessageAggregate> bulkLoadByRoomId(UUID roomId);

    /**
     * Bulk load all active message aggregates for a specific room.
     *
     * @param roomId the unique identifier of the room
     * @return list of active message aggregates (excludes soft-deleted messages)
     *
     * <p>Optimized for common read patterns where only current/visible messages matter.
     * More efficient than filtering {@link #bulkLoadByRoomId(UUID)} results in memory.</p>
     */
    List<MessageAggregate> bulkLoadActiveByRoomId(UUID roomId);

    /**
     * Bulk load active message aggregates for a specific room, limited by count.
     *
     * @param roomId the unique identifier of the room
     * @param limit maximum number of messages to return (most recent first)
     * @return list of up to {@code limit} active message aggregates
     *
     * <p>Useful for paginated message feeds, chat history loading, or "load more" patterns.
     * Messages are typically ordered by {@code createdAt} descending (newest first).</p>
     */
    List<MessageAggregate> bulkLoadActiveByRoomIdLimited(UUID roomId, int limit);

    /**
     * Bulk load active message aggregates for a specific room, with cursor-based pagination.
     *
     * @param roomId the unique identifier of the room
     * @param afterId optional cursor: only return messages created after this message ID
     * @param limit maximum number of messages to return
     * @return list of active message aggregates matching the pagination criteria
     *
     * <p>Use this for infinite-scroll chat interfaces where clients request messages
     * newer than a known message ID. More efficient than offset-based pagination for large datasets.</p>
     */
    List<MessageAggregate> bulkLoadActiveByRoomIdAfter(UUID roomId, UUID afterId, int limit);

    /**
     * Bulk load all message aggregates sent by a specific user.
     *
     * @param senderId the unique identifier of the sender
     * @return list of all message aggregates sent by this user (including deleted messages)
     *
     * <p>Use this for:
     * <ul>
     *   <li>User message history / activity logs</li>
     *   <li>Computing sender-level statistics</li>
     *   <li>Handling user account deletion or data export</li>
     * </ul>
     *
     * <p>Consider pagination for users with many messages in production implementations.</p>
     */
    List<MessageAggregate> bulkLoadBySenderId(UUID senderId);

    /**
     * Bulk load all active message aggregates sent by a specific user.
     *
     * @param senderId the unique identifier of the sender
     * @return list of active message aggregates sent by this user (excludes soft-deleted)
     *
     * <p>Optimized for UI displays or business logic that only concerns visible messages.</p>
     */
    List<MessageAggregate> bulkLoadActiveBySenderId(UUID senderId);

    /**
     * Bulk load active message aggregates sent by a user in specific rooms.
     *
     * @param senderId the unique identifier of the sender
     * @param roomIds collection of room identifiers to filter by
     * @return list of active message aggregates sent by this user in the specified rooms
     *
     * <p>Useful for permission checks, room-specific activity feeds, or cross-room analytics.</p>
     */
    List<MessageAggregate> bulkLoadActiveBySenderIdAndRooms(UUID senderId, Collection<UUID> roomIds);

    /**
     * Bulk load message aggregates by delivery status.
     *
     * @param status the message status to filter by (SENT, RECEIVED, or SEEN)
     * @return list of message aggregates with the given status (including deleted)
     *
     * <p>Useful for:
     * <ul>
     *   <li>Monitoring undelivered messages (SENT but not RECEIVED)</li>
     *   <li>Analytics on message read rates</li>
     *   <li>Retry logic for failed deliveries</li>
     * </ul>
     *
     * <p>Consider adding time-range filters for large datasets in production.</p>
     */
    List<MessageAggregate> bulkLoadByStatus(Message.Status status);

    /**
     * Bulk load active message aggregates by delivery status.
     *
     * @param status the message status to filter by
     * @return list of active message aggregates with the given status (excludes deleted)
     *
     * <p>Optimized version that excludes soft-deleted messages from results.</p>
     */
    List<MessageAggregate> bulkLoadActiveByStatus(Message.Status status);

    /**
     * Bulk load active messages with SENT status that are older than a threshold.
     *
     * @param olderThan timestamp threshold: only return messages created before this time
     * @return list of active SENT messages that may need retry or escalation
     *
     * <p>Useful for background jobs that monitor message delivery health or trigger
     * fallback notification channels for stale undelivered messages.</p>
     */
    List<MessageAggregate> bulkLoadActiveSentOlderThan(LocalDateTime olderThan);

    /**
     * Bulk load all reply messages for a given parent message.
     *
     * @param parentId the ID of the parent message being replied to
     * @return list of all message aggregates that are replies to this parent (including deleted)
     *
     * <p>Useful for displaying threaded conversations, computing reply counts,
     * or managing nested discussion trees.</p>
     */
    List<MessageAggregate> bulkLoadRepliesTo(UUID parentId);

    /**
     * Bulk load active reply messages for a given parent message.
     *
     * @param parentId the ID of the parent message being replied to
     * @return list of active reply message aggregates (excludes soft-deleted)
     *
     * <p>Optimized for UI displays where only visible replies should be shown.</p>
     */
    List<MessageAggregate> bulkLoadActiveRepliesTo(UUID parentId);

    /**
     * Check whether a message has any replies.
     *
     * @param messageId the ID of the message to check
     * @return {@code true} if at least one reply exists for this message, {@code false} otherwise
     *
     * <p>Efficient existence check for UI indicators (e.g., "has replies" badge)
     * without loading full reply data.</p>
     */
    boolean hasReplies(UUID messageId);

    /**
     * Bulk load message aggregates for multiple message IDs.
     *
     * @param messageIds collection of message identifiers to look up
     * @return list of message aggregates for matching IDs
     *
     * <p>Useful for batch operations like:
     * <ul>
     *   <li>Checking which of several messages exist and loading their state</li>
     *   <li>Bulk message updates, deletions, or status changes</li>
     *   <li>Permission checks for multiple messages at once</li>
     * </ul>
     *
     * <p>Returns only found messages — does not throw if some IDs are not found.</p>
     */
    List<MessageAggregate> bulkLoadByIds(Collection<UUID> messageIds);

    /**
     * Bulk load active message aggregates for multiple message IDs.
     *
     * @param messageIds collection of message identifiers to look up
     * @return list of active message aggregates for matching IDs (excludes soft-deleted)
     *
     * <p>Same as {@link #bulkLoadByIds(Collection)} but filters out deleted messages.
     * Useful for messaging, presence, or UI features that only show active messages.</p>
     */
    List<MessageAggregate> bulkLoadActiveByIds(Collection<UUID> messageIds);

    /**
     * Bulk load active messages for a specific sender in a specific room.
     *
     * @param roomId the room identifier
     * @param senderId the sender identifier
     * @return list of active message aggregates sent by this user in this room
     *
     * <p>Useful for:
     * <ul>
     *   <li>Displaying a user's message history within a specific conversation</li>
     *   <li>Checking if a user has posted in a room before allowing certain actions</li>
     *   <li>Computing per-user, per-room engagement metrics</li>
     * </ul>
     */
    List<MessageAggregate> bulkLoadActiveByRoomAndSender(UUID roomId, UUID senderId);

    /**
     * Bulk load active messages with images for a specific room.
     *
     * @param roomId the room identifier
     * @return list of active message aggregates that have image attachments
     *
     * <p>Useful for media galleries, image-only feeds, or analytics on image sharing behavior.</p>
     */
    List<MessageAggregate> bulkLoadActiveWithImagesByRoomId(UUID roomId);

    /**
     * Bulk load active reply messages for a specific room.
     *
     * @param roomId the room identifier
     * @return list of active message aggregates that are replies (have parentId set)
     *
     * <p>Useful for threaded conversation views or analytics on reply engagement.</p>
     */
    List<MessageAggregate> bulkLoadActiveRepliesByRoomId(UUID roomId);

    // ── Bulk Command Operations ────────────────────────────────────────

    /**
     * Bulk soft-delete all active messages in a room that are older than a threshold.
     *
     * @param roomId the room identifier
     * @param olderThan timestamp threshold: only delete messages created before this time
     * @param actorId the user ID performing the deletion (for audit/logging)
     * @return count of messages that were soft-deleted
     *
     * <p>Useful for automated retention policies, GDPR compliance, or room archival.
     * Only affects active (non-deleted) messages; already-deleted messages are ignored.</p>
     *
     * <p>Implementations should ensure this operation is atomic and respects
     * domain authorization rules (caller must have appropriate permissions).</p>
     */
    int bulkDeleteOldMessagesInRoom(UUID roomId, LocalDateTime olderThan, UUID actorId);

    /**
     * Bulk update status for multiple messages (e.g., mark many as RECEIVED).
     *
     * @param messageIds collection of message IDs to update
     * @param newStatus the target status (must be a valid transition for each message)
     * @param actorId the user ID performing the update (for audit/logging)
     * @return count of messages that were successfully updated
     *
     * <p>Useful for batch acknowledgment operations, e.g., marking all messages
     * in a room as SEEN when a user re-opens a conversation.</p>
     *
     * <p>Implementations should validate status transitions per message and
     * skip (not fail) messages where the transition is invalid or unauthorized.</p>
     */
    int bulkUpdateStatus(Collection<UUID> messageIds, Message.Status newStatus, UUID actorId);
        /**
     * Bulk mark all active messages in a room as RECEIVED for a specific receiver.
     *
     * <p>Service loads all active message aggregates for the room, verifies that the actor
     * is the intended receiver for each message (NOT the sender), applies domain logic
     * to transition status to RECEIVED where valid, and persists updated state.</p>
     *
     * <p><strong>Behavior:</strong>
     * <ul>
     *   <li>Only processes active (non-deleted) messages where actorId matches the message receiverId</li>
     *   <li>Skips messages where actor is the sender (only receiver can mark as received)</li>
     *   <li>Skips messages already in RECEIVED or SEEN state (idempotent operation)</li>
     *   <li>Logs warnings for skipped messages but continues processing remaining messages</li>
     *   <li>Fail-soft: partial success is possible if some messages fail validation</li>
     * </ul></p>
     *
     * @param roomId the UUID of the room containing messages to mark as received
     * @param actorId the UUID of the user marking messages as received (must be the receiver, not sender)
     * @return count of messages that were successfully transitioned to RECEIVED status
     *
     * <p><strong>Use cases:</strong>
     * <ul>
     *   <li>User opens a chat room — automatically mark all pending messages as delivered</li>
     *   <li>Background job synchronizing delivery status across devices</li>
     *   <li>Reconnection logic after network interruption</li>
     * </ul></p>
     *
     * <p><strong>Note:</strong> This operation does NOT mark messages as SEEN — use a separate
     * bulk operation or individual {@link #markAsSeen(UUID, UUID)} calls for read receipts.</p>
     */
    int bulkMarkAsReceivedInRoom(UUID roomId, UUID actorId);
}