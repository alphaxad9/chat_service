// chat_service/src/main/java/com/example/chat_service/domain/messages/repositories/MessageQueryRepository.java
package com.example.chat_service.domain.messages.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.messages.Message;

/**
 * Interface for read-only query operations on message entities.
 *
 * <p>All methods return {@link Message} entities (not aggregates) optimized for read operations.
 * <strong>Important:</strong> All query methods automatically exclude messages where {@code isDeleted = true},
 * ensuring only active messages are returned. For historical/audit queries including deleted messages,
 * use a separate audit repository or the command repository.</p>
 *
 * <p>This is a domain-layer interface — infrastructure implementations (e.g., JPA, JDBC, MongoDB)
 * should reside in {@code infrastructure.persistence.messages.repositories}.</p>
 *
 * <p>Designed for CQRS read-side: fast, denormalized, projection-friendly queries.</p>
 */
public interface MessageQueryRepository {

    // ── Single Entity Queries (Active Messages Only) ──────────────────

    /**
     * Load an active message by its unique ID.
     *
     * @param messageId the unique identifier of the message
     * @return the message if found and active, otherwise {@link Optional#empty()}
     *
     * <p>Automatically filters out messages where {@code isDeleted = true}.
     * Use {@code Optional} to handle "not found or deleted" without exceptions.</p>
     */
    Optional<Message> findById(UUID messageId);

    /**
     * Load a message by its unique ID, including deleted messages.
     *
     * @param messageId the unique identifier of the message
     * @return the message if found (regardless of deletion status), otherwise {@link Optional#empty()}
     *
     * <p>Use for admin/audit operations where deleted message details are needed.
     * Most application logic should prefer {@link #findById(UUID)}.</p>
     */
    Optional<Message> findByIdIncludingDeleted(UUID messageId);

    /**
     * Check if a message exists and is active.
     *
     * @param messageId the unique identifier of the message
     * @return {@code true} if message exists and is active, {@code false} otherwise
     *
     * <p>Optimized boolean check — does not load full entity.
     * Returns {@code false} if message doesn't exist OR is deleted.</p>
     */
    boolean isActiveMessage(UUID messageId);

    // ── Latest Message Queries (Active Messages Only) ─────────────────

    /**
     * Load the most recent active message in a specific room.
     *
     * @param roomId the unique identifier of the room
     * @return the latest active {@link Message} if any exist, otherwise {@link Optional#empty()}
     *
     * <p>Excludes deleted messages. Returns the message with the highest {@code createdAt}
     * (or {@code updatedAt} if edited). Use for: conversation list previews, "last message"
     * displays, unread badge calculations.</p>
     *
     * <p>Efficient single-row query — does not load full room history.</p>
     */
    Optional<Message> findLatestActiveByRoomId(UUID roomId);

    /**
     * Load the most recent active message for multiple rooms in a single query.
     *
     * @param roomIds collection of room identifiers to check
     * @return map of roomId → latest active {@link Message} (only includes rooms that have messages)
     *
     * <p>Excludes deleted messages. Returns only rooms that have at least one active message.
     * Rooms with no active messages are omitted from the result map (not mapped to empty).
     * Use for: building conversation list views with "last message" previews for multiple rooms.</p>
     *
     * <p>More efficient than calling {@link #findLatestActiveByRoomId(UUID)} in a loop —
     * performs a single batched query instead of N+1 queries.</p>
     */
    Map<UUID, Message> findLatestActiveByRoomIds(Collection<UUID> roomIds);

    /**
     * Load the most recent active message summary for multiple rooms.
     *
     * @param roomIds collection of room identifiers to check
     * @return map of roomId → latest active {@link MessageSummary} (only includes rooms with messages)
     *
     * <p>Optimized projection version of {@link #findLatestActiveByRoomIds(Collection)}.
     * Returns lightweight summaries instead of full entities. Use for UI conversation lists
     * where only preview data is needed (sender, content snippet, timestamp, status).</p>
     *
     * <p>Reduces memory usage and network payload for large conversation lists.</p>
     */
    Map<UUID, MessageSummary> findLatestActiveSummariesByRoomIds(Collection<UUID> roomIds);

    // ── Bulk Queries by Room (Active Messages Only) ───────────────────

    /**
     * Load all active messages in a specific room.
     *
     * @param roomId the unique identifier of the room
     * @return list of active {@link Message} entities in this room
     *
     * <p>Excludes all messages where {@code isDeleted = true}.
     * Results are ordered by {@code createdAt} ascending (oldest first) for chat history.
     * Use for: loading conversation history, room message feeds.</p>
     *
     * <p>Consider pagination overloads for rooms with many messages in production.</p>
     */
    List<Message> findAllActiveByRoomId(UUID roomId);

    /**
     * Load active messages in a room, limited by count (most recent first).
     *
     * @param roomId the unique identifier of the room
     * @param limit maximum number of messages to return
     * @return list of up to {@code limit} active {@link Message} entities
     *
     * <p>Excludes deleted messages. Results ordered by {@code createdAt} descending.
     * Use for: "latest N messages" preview, chat window initial load.</p>
     */
    List<Message> findActiveByRoomIdLimited(UUID roomId, int limit);

    /**
     * Load active messages in a room with cursor-based pagination.
     *
     * @param roomId the unique identifier of the room
     * @param afterId optional cursor: only return messages created after this message ID
     * @param limit maximum number of messages to return
     * @return list of active {@link Message} entities matching the pagination criteria
     *
     * <p>Excludes deleted messages. Use for infinite-scroll chat interfaces where
     * clients request messages newer than a known message ID. More efficient than
     * offset-based pagination for large datasets.</p>
     */
    List<Message> findActiveByRoomIdAfter(UUID roomId, UUID afterId, int limit);

    /**
     * Load active messages in a room before a given cursor (for loading older history).
     *
     * @param roomId the unique identifier of the room
     * @param beforeId optional cursor: only return messages created before this message ID
     * @param limit maximum number of messages to return
     * @return list of active {@link Message} entities older than the cursor
     *
     * <p>Excludes deleted messages. Results ordered by {@code createdAt} descending.
     * Use for "load older messages" pagination in chat interfaces.</p>
     */
    List<Message> findActiveByRoomIdBefore(UUID roomId, UUID beforeId, int limit);

    /**
     * Count active messages in a room.
     *
     * @param roomId the unique identifier of the room
     * @return count of active messages (excludes deleted)
     *
     * <p>Efficient count query — does not load full entities.
     * Use for: room message counts, UI badges, pagination metadata.</p>
     */
    long countActiveByRoomId(UUID roomId);

    // ── Bulk Queries by Sender (Active Messages Only) ─────────────────

    /**
     * Load all active messages sent by a specific user.
     *
     * @param senderId the unique identifier of the sender
     * @return list of active {@link Message} entities sent by this user
     *
     * <p>Excludes deleted messages. Results ordered by {@code createdAt} descending.
     * Use for: user message history, activity logs, sender analytics.</p>
     *
     * <p>Consider pagination for users with many messages in production.</p>
     */
    List<Message> findAllActiveBySenderId(UUID senderId);

    /**
     * Load active messages sent by a user in specific rooms.
     *
     * @param senderId the unique identifier of the sender
     * @param roomIds collection of room identifiers to filter by
     * @return list of active {@link Message} entities sent by this user in the specified rooms
     *
     * <p>Excludes deleted messages. Useful for cross-room activity feeds or
     * permission checks without loading all user messages.</p>
     */
    List<Message> findActiveBySenderIdAndRooms(UUID senderId, Collection<UUID> roomIds);

    /**
     * Count active messages sent by a user.
     *
     * @param senderId the unique identifier of the sender
     * @return count of active messages sent by this user (excludes deleted)
     *
     * <p>Efficient count — does not load entities.
     * Use for: user engagement metrics, contribution stats.</p>
     */
    long countActiveBySenderId(UUID senderId);

    // ── Bulk Queries by Status (Active Messages Only) ─────────────────

    /**
     * Load active messages by delivery status.
     *
     * @param status the message status to filter by (SENT, RECEIVED, or SEEN)
     * @return list of active {@link Message} entities with the given status
     *
     * <p>Excludes deleted messages. Results ordered by {@code createdAt} descending.
     * Use for: monitoring undelivered messages, analytics on read rates,
     * retry logic for failed deliveries.</p>
     *
     * <p>Consider adding time-range filters for large datasets in production.</p>
     */
    List<Message> findActiveByStatus(Message.Status status);

    /**
     * Load active messages by status in a specific room.
     *
     * @param roomId the room identifier
     * @param status the message status to filter by
     * @return list of active {@link Message} entities in the room with the given status
     *
     * <p>Excludes deleted messages. Useful for room-specific delivery tracking
     * or per-conversation read receipt analytics.</p>
     */
    List<Message> findActiveByRoomAndStatus(UUID roomId, Message.Status status);

    /**
     * Load active SENT messages older than a threshold (potential delivery issues).
     *
     * @param olderThan timestamp threshold: only return messages created before this time
     * @return list of active SENT messages that may need retry or escalation
     *
     * <p>Excludes deleted messages. Use for background jobs that monitor
     * message delivery health or trigger fallback notification channels.</p>
     */
    List<Message> findActiveSentOlderThan(LocalDateTime olderThan);

    // ── Reply Chain Queries (Active Messages Only) ────────────────────

    /**
     * Load all active reply messages for a given parent message.
     *
     * @param parentId the ID of the parent message being replied to
     * @return list of active {@link Message} entities that are replies to this parent
     *
     * <p>Excludes deleted messages. Results ordered by {@code createdAt} ascending.
     * Use for: displaying threaded conversations, computing reply counts,
     * managing nested discussion trees.</p>
     */
    List<Message> findActiveRepliesTo(UUID parentId);

    /**
     * Load active reply messages for a parent, limited by count.
     *
     * @param parentId the ID of the parent message being replied to
     * @param limit maximum number of replies to return
     * @return list of up to {@code limit} active reply {@link Message} entities
     *
     * <p>Excludes deleted messages. Useful for "show N replies" preview UIs
     * with "view all" expansion.</p>
     */
    List<Message> findActiveRepliesToLimited(UUID parentId, int limit);

    /**
     * Check whether an active message has any active replies.
     *
     * @param messageId the ID of the message to check
     * @return {@code true} if at least one active reply exists for this message
     *
     * <p>Efficient existence check for UI indicators (e.g., "has replies" badge)
     * without loading full reply data. Excludes deleted replies from count.</p>
     */
    boolean hasActiveReplies(UUID messageId);

    /**
     * Count active replies for a given parent message.
     *
     * @param parentId the ID of the parent message being replied to
     * @return count of active reply messages (excludes deleted)
     *
     * <p>Efficient count — does not load entities.
     * Use for: reply count badges, engagement metrics.</p>
     */
    long countActiveRepliesTo(UUID parentId);

    // ── Bulk Lookup Queries (Active Messages Only) ───────────────────

    /**
     * Bulk lookup active messages by their IDs.
     *
     * @param messageIds collection of message identifiers to check
     * @return list of active {@link Message} entities for matching IDs
     *
     * <p>Returns only messages that exist and are active.
     * Does not throw if some IDs are not found or deleted.
     * Order of results is not guaranteed — use a map if order matters.
     * Use for: batch operations, permission checks, presence updates.</p>
     */
    List<Message> findActiveByIds(Collection<UUID> messageIds);

    /**
     * Bulk lookup active messages by room IDs.
     *
     * @param roomIds collection of room identifiers to check
     * @return list of active {@link Message} entities in matching rooms
     *
     * <p>Returns only active messages whose room is in the provided collection.
     * Ordered by {@code createdAt} descending.
     * Use for: multi-room message feeds, org-level activity streams.</p>
     */
    List<Message> findActiveByRoomIds(Collection<UUID> roomIds);

    // ── Activity/Time-Based Queries (Active Messages Only) ────────────

    /**
     * Load active messages with recent creation time.
     *
     * @param sinceTimestamp only return messages with {@code createdAt >= sinceTimestamp}
     * @param limit maximum number of results to return
     * @return list of recently created active {@link Message} entities
     *
     * <p>Excludes deleted messages. Results ordered by {@code createdAt} descending.
     * Use for: "new messages" feed, real-time notification targeting,
     * activity-based caching strategies.</p>
     */
    List<Message> findActiveCreatedSince(LocalDateTime sinceTimestamp, int limit);

    /**
     * Load active messages with recent activity (created or updated).
     *
     * @param sinceTimestamp only return messages with {@code updatedAt >= sinceTimestamp}
     * @param limit maximum number of results to return
     * @return list of recently updated active {@link Message} entities
     *
     * <p>Excludes deleted messages. Captures edits, status changes, and new messages.
     * Use for: sync endpoints, change detection, cache invalidation triggers.</p>
     */
    List<Message> findActiveUpdatedSince(LocalDateTime sinceTimestamp, int limit);

    // ── Content/Image Queries (Active Messages Only) ──────────────────

    /**
     * Load active messages that have image attachments in a room.
     *
     * @param roomId the room identifier
     * @return list of active {@link Message} entities with non-null image URLs
     *
     * <p>Excludes deleted messages. Useful for media galleries, image-only feeds,
     * or analytics on image sharing behavior within a conversation.</p>
     */
    List<Message> findActiveWithImagesByRoomId(UUID roomId);

    /**
     * Load active messages that have image attachments by sender.
     *
     * @param senderId the sender identifier
     * @return list of active {@link Message} entities with non-null image URLs
     *
     * <p>Excludes deleted messages. Useful for user media portfolios or
     * analytics on image sharing patterns per user.</p>
     */
    List<Message> findActiveWithImagesBySenderId(UUID senderId);

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    /**
     * Fetch minimal message info for active messages in a room.
     *
     * @param roomId the unique identifier of the room
     * @return list of {@link MessageSummary} records (lightweight DTO)
     *
     * <p>Optimized for UI lists: only fetches essential display fields.
     * Avoids loading full entity state when only list rendering is needed.
     * Implementations may use database projections or cached read models.</p>
     */
    List<MessageSummary> findActiveSummariesByRoomId(UUID roomId);

    /**
     * Fetch minimal message info for active messages by IDs.
     *
     * @param messageIds collection of message identifiers
     * @return list of {@link MessageSummary} records for matching active messages
     *
     * <p>Same optimization as {@link #findActiveSummariesByRoomId(UUID)}
     * but for arbitrary message ID lookups. Useful for batch UI rendering.</p>
     */
    List<MessageSummary> findActiveSummariesByIds(Collection<UUID> messageIds);

    /**
     * Fetch minimal info for active messages with pagination.
     *
     * @param roomId the room identifier
     * @param afterId optional cursor for pagination
     * @param limit maximum results to return
     * @return list of {@link MessageSummary} records for matching active messages
     *
     * <p>Optimized for infinite-scroll chat interfaces. Only fetches fields needed
     * for message bubbles: id, sender, content preview, image flag, timestamps, status.</p>
     */
    List<MessageSummary> findActiveSummariesByRoomIdAfter(UUID roomId, UUID afterId, int limit);

    /**
     * Fetch minimal info for active reply messages to a parent.
     *
     * @param parentId the ID of the parent message
     * @return list of {@link MessageSummary} records for active replies
     *
     * <p>Optimized for threaded conversation previews. Only fetches essential
     * fields for rendering reply indicators and quick previews.</p>
     */
    List<MessageSummary> findActiveReplySummariesTo(UUID parentId);

    // ── Nested DTO for Lightweight Projections ───────────────────────

    /**
     * Lightweight read model for message list displays.
     * Contains only fields needed for UI rendering or quick checks.
     */
    record MessageSummary(
        UUID messageId,
        UUID roomId,
        UUID senderId,
        String contentPreview,      // First N chars of content, or null if image-only
        boolean hasImage,           // Whether message has an image attachment
        boolean isReply,            // Whether this message is a reply
        UUID parentId,              // Parent message ID if isReply=true, else null
        Message.Status status,      // Delivery status for read receipts
        boolean isSeen,             // Convenience: status == SEEN
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime seenAt        // May be null if not seen
    ) {}
}