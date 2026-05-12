// chat_service/src/main/java/com/example/chat_service/domain/messages/repositories/MessageCommandRepository.java
package com.example.chat_service.domain.messages.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.MessageAggregate;
import com.example.chat_service.domain.messages.exceptions.MessageNotFoundError;

/**
 * Interface for write operations on message aggregates.
 *
 * <p>All methods operate on full {@link MessageAggregate} instances to preserve domain invariants.
 * Implementations are responsible for persistence, optimistic concurrency control (if used),
 * and ensuring aggregate state (including soft-delete flags, status transitions, and reply chains)
 * is durably stored.</p>
 *
 * <p>This is a domain-layer interface — infrastructure implementations (e.g., JPA, JDBC, MongoDB)
 * should reside in {@code infrastructure.persistence.messages.repositories}.</p>
 */
public interface MessageCommandRepository {

    /**
     * Persist a message aggregate.
     *
     * <p>This method handles both creation and updates:
     * <ul>
     *   <li>If the message does not exist (new ID), it performs an INSERT.</li>
     *   <li>If it exists, it performs an UPDATE based on the message ID.</li>
     * </ul>
     *
     * <p>The aggregate must be fully validated before calling this method.
     * This includes ensuring content length limits, image URL formats, valid status transitions,
     * and that reply parent references exist and are not self-referential.</p>
     *
     * @param aggregate the message aggregate to persist
     */
    void save(MessageAggregate aggregate);

    /**
     * Persist multiple message aggregates in a single transaction.
     *
     * <p>Useful for:
     * <ul>
     *   <li>Batch message imports or migrations</li>
     *   <li>Atomic creation of message threads with replies</li>
     *   <li>Bulk status updates across multiple messages</li>
     * </ul>
     *
     * <p>All aggregates must be valid before calling. If any aggregate fails validation
     * or persistence, the entire operation should roll back (transactional semantics).</p>
     *
     * @param aggregates collection of message aggregates to persist
     */
    void bulkSave(Collection<MessageAggregate> aggregates);

    /**
     * Load an existing message aggregate by its unique ID.
     *
     * @param messageId the unique identifier of the message
     * @return the loaded message aggregate
     * @throws MessageNotFoundError if no message exists with the given ID
     *
     * <p>Used before applying any update command (e.g., markAsSeen, withContent, delete).</p>
     */
    MessageAggregate load(UUID messageId);

    /**
     * Load an existing message aggregate by its unique ID, returning optional.
     *
     * @param messageId the unique identifier of the message
     * @return the message aggregate if found, otherwise {@link Optional#empty()}
     *
     * <p>Use this when message existence is uncertain and you want to avoid
     * exception handling for the "not found" case.</p>
     */
    Optional<MessageAggregate> loadOptional(UUID messageId);

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
     * Check whether a message record exists for the given message ID.
     *
     * @param messageId the unique identifier of the message
     * @return {@code true} if a message with the given ID exists, {@code false} otherwise
     *
     * <p>Useful for fast validation before attempting to load or update.
     * Avoids loading full aggregate state when only existence matters.</p>
     */
    boolean exists(UUID messageId);

    /**
     * Check whether a message exists in a specific room.
     *
     * @param roomId the room identifier
     * @param messageId the message identifier
     * @return {@code true} if a message with the given ID exists in the room, {@code false} otherwise
     *
     * <p>Useful for validating message-room relationships without loading full state.</p>
     */
    boolean existsInRoom(UUID roomId, UUID messageId);

    /**
     * Check whether any messages exist for a given sender in a room.
     *
     * @param roomId the room identifier
     * @param senderId the sender identifier
     * @return {@code true} if the sender has any messages in the room, {@code false} otherwise
     *
     * <p>Useful for permission checks or UI logic (e.g., "has this user posted in this room?").</p>
     */
    boolean existsByRoomAndSender(UUID roomId, UUID senderId);

    // ── Query by Room ──────────────────────────────────────────────────

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

    // ── Query by Sender ────────────────────────────────────────────────

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

    // ── Query by Status ────────────────────────────────────────────────

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
    List<MessageAggregate> bulkLoadActiveSentOlderThan(java.time.LocalDateTime olderThan);

    // ── Query by Reply Chain ───────────────────────────────────────────

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

    // ── Bulk Load by IDs ───────────────────────────────────────────────

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

    // ── Bulk Operations by Room + Sender ───────────────────────────────

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

    // ── Cleanup / Maintenance ──────────────────────────────────────────

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
    int bulkDeleteOldMessagesInRoom(UUID roomId, java.time.LocalDateTime olderThan, UUID actorId);

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
}