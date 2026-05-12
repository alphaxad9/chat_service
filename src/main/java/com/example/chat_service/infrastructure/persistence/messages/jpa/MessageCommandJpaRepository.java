package com.example.chat_service.infrastructure.persistence.messages.jpa;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.messages.MessageEntity;

/**
 * Spring Data JPA repository for command-side (write) operations on {@link MessageEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code MessageCommandOrmRepository} by providing
 * type-safe, derived-query methods for aggregate persistence and retrieval.
 * <strong>No custom JPQL/SQL</strong> — all methods use Spring Data JPA's method-name derivation only.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isDeleted} is handled
 * explicitly via method names (e.g., {@code ...AndIsDeletedFalse()}) rather than
 * {@code @SQLRestriction}. This allows the same repository to support both
 * active-only and all-messages queries using pure ORM derivation.</p>
 *
 * <p><strong>Unique constraint:</strong> Database-level uniqueness on message ID is enforced by primary key.
 * Application logic should check existence before creation to avoid {@code DataIntegrityViolationException}.</p>
 *
 * <p><strong>Not for read-side queries:</strong> This repository is optimized for
 * loading full aggregates for mutation. For read-only views, lists, or projections,
 * use {@code MessageQueryJpaRepository} when implemented.</p>
 */
@Repository
public interface MessageCommandJpaRepository extends JpaRepository<MessageEntity, UUID> {

    // ── Inherited Methods from JpaRepository<MessageEntity, UUID> ─────────
    // Basic CRUD operations (no isDeleted filtering — caller decides):
    //
    // • Optional<MessageEntity> findById(UUID id)
    //   → Loads entity by ID regardless of isDeleted status
    //
    // • <S extends MessageEntity> S save(S entity)
    //   → INSERT if new ID, UPDATE if ID exists (JPA merge pattern)
    //
    // • boolean existsById(UUID id)
    //   → Fast existence check regardless of isDeleted status

    // ── Derived Query Methods: Active Messages Only (isDeleted = false) ──

    /**
     * Find an active message by room ID.
     *
     * <p>Filters to {@code isDeleted = false} via method name derivation.
     * Results ordered by createdAt ascending for chat history loading.</p>
     *
     * @param roomId the UUID of the room
     * @return list of active message entities in the room
     */
    List<MessageEntity> findAllByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID roomId);

    /**
     * Find active messages by room ID, limited by count (most recent first).
     *
     * <p>Filters to {@code isDeleted = false} and orders by createdAt descending.
     * Used for "latest N messages" previews.</p>
     *
     * @param roomId the UUID of the room
     * @param limit maximum number of messages to return
     * @return list of up to {@code limit} active message entities
     */
    List<MessageEntity> findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID roomId, int limit);

    /**
     * Find active messages by room ID created after a cursor message ID.
     *
     * <p>Filters to {@code isDeleted = false} and {@code createdAt > cursor message's createdAt}.
     * Used for cursor-based pagination (infinite scroll, "load newer").</p>
     *
     * @param roomId the UUID of the room
     * @param afterId the cursor message ID to paginate after
     * @param limit maximum number of messages to return
     * @return list of active message entities newer than the cursor
     */
    List<MessageEntity> findByRoomIdAndIsDeletedFalseAndCreatedAtGreaterThanOrderByCreatedAtAsc(
            UUID roomId, LocalDateTime afterCreatedAt, int limit);

    /**
     * Find active messages by room ID created before a cursor timestamp.
     *
     * <p>Filters to {@code isDeleted = false} and {@code createdAt < beforeTimestamp}.
     * Used for "load older messages" pagination.</p>
     *
     * @param roomId the UUID of the room
     * @param beforeTimestamp the timestamp to paginate before
     * @param limit maximum number of messages to return
     * @return list of active message entities older than the cursor
     */
    List<MessageEntity> findByRoomIdAndIsDeletedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID roomId, LocalDateTime beforeTimestamp, int limit);

    /**
     * Find active messages by sender ID.
     *
     * <p>Filters to {@code isDeleted = false}, ordered by createdAt descending.
     * Used for user message history.</p>
     *
     * @param senderId the UUID of the sender
     * @return list of active message entities sent by this user
     */
    List<MessageEntity> findAllBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID senderId);

    /**
     * Find active messages by sender ID in specific rooms.
     *
     * <p>Filters to {@code isDeleted = false} and {@code roomId IN (...)}.
     * Used for cross-room activity queries.</p>
     *
     * @param senderId the UUID of the sender
     * @param roomIds collection of room UUIDs to filter by
     * @return list of active message entities in the specified rooms
     */
    List<MessageEntity> findAllBySenderIdAndRoomIdInAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID senderId, Collection<UUID> roomIds);

    /**
     * Find active messages by delivery status.
     *
     * <p>Filters to {@code isDeleted = false}, ordered by createdAt descending.
     * Used for delivery monitoring and analytics.</p>
     *
     * @param status the message status to filter by
     * @return list of active message entities with the given status
     */
    List<MessageEntity> findAllByStatusAndIsDeletedFalseOrderByCreatedAtDesc(MessageEntity.Status status);

    /**
     * Find active SENT messages older than a threshold.
     *
     * <p>Filters to {@code isDeleted = false}, {@code status = SENT}, and {@code createdAt < olderThan}.
     * Used for retry/escalation background jobs.</p>
     *
     * @param status the message status (should be SENT)
     * @param olderThan timestamp threshold
     * @return list of stale active SENT messages
     */
    List<MessageEntity> findAllByStatusAndIsDeletedFalseAndCreatedAtBeforeOrderByCreatedAtAsc(
            MessageEntity.Status status, LocalDateTime olderThan);

    /**
     * Find active reply messages for a parent message.
     *
     * <p>Filters to {@code isDeleted = false} and {@code parentId = ...}, ordered by createdAt ascending.
     * Used for threaded conversation displays.</p>
     *
     * @param parentId the ID of the parent message
     * @return list of active reply message entities
     */
    List<MessageEntity> findAllByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId);

    /**
     * Check if a message has any active replies.
     *
     * <p>Efficient existence check for UI badges without loading full data.</p>
     *
     * @param parentId the ID of the parent message
     * @return {@code true} if at least one active reply exists
     */
    boolean existsByParentIdAndIsDeletedFalse(UUID parentId);

    /**
     * Bulk lookup active messages by multiple IDs.
     *
     * <p>Filters to {@code isDeleted = false}. Returns only found active messages.</p>
     *
     * @param messageIds collection of message UUIDs to lookup
     * @return list of matching active message entities
     */
    List<MessageEntity> findAllByIdInAndIsDeletedFalse(Collection<UUID> messageIds);

    /**
     * Find active messages by room and sender.
     *
     * <p>Filters to {@code isDeleted = false}, {@code roomId = ...}, and {@code senderId = ...}.
     * Used for per-user room activity queries.</p>
     *
     * @param roomId the UUID of the room
     * @param senderId the UUID of the sender
     * @return list of active message entities sent by this user in this room
     */
    List<MessageEntity> findAllByRoomIdAndSenderIdAndIsDeletedFalseOrderByCreatedAtAsc(
            UUID roomId, UUID senderId);

    /**
     * Find active messages with image attachments in a room.
     *
     * <p>Filters to {@code isDeleted = false}, {@code roomId = ...}, and {@code imageUrl IS NOT NULL}.
     * Used for media gallery queries.</p>
     *
     * @param roomId the UUID of the room
     * @return list of active message entities with images
     */
    List<MessageEntity> findAllByRoomIdAndImageUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(UUID roomId);

    /**
     * Find active reply messages in a room.
     *
     * <p>Filters to {@code isDeleted = false}, {@code roomId = ...}, and {@code parentId IS NOT NULL}.
     * Used for threaded conversation analytics.</p>
     *
     * @param roomId the UUID of the room
     * @return list of active reply message entities in the room
     */
    List<MessageEntity> findAllByRoomIdAndParentIdIsNotNullAndIsDeletedFalseOrderByCreatedAtAsc(UUID roomId);

    /**
     * Find active messages older than a threshold for bulk deletion.
     *
     * <p>Filters to {@code isDeleted = false}, {@code roomId = ...}, and {@code createdAt < olderThan}.
     * Used for retention policy enforcement.</p>
     *
     * @param roomId the UUID of the room
     * @param olderThan timestamp threshold
     * @return list of active message entities eligible for soft-delete
     */
    List<MessageEntity> findAllByRoomIdAndIsDeletedFalseAndCreatedAtBefore(UUID roomId, LocalDateTime olderThan);

    // ── Derived Query Methods: All Messages (including deleted) ──────────

    /**
     * Find all messages by room ID, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all message states.
     * Used for admin/audit operations and full history exports.</p>
     *
     * @param roomId the UUID of the room
     * @return list of all message entities in the room (active + deleted)
     */
    List<MessageEntity> findAllByRoomIdOrderByCreatedAtAsc(UUID roomId);

    /**
     * Find all messages by sender ID, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all message states.
     * Used for user data export or account deletion workflows.</p>
     *
     * @param senderId the UUID of the sender
     * @return list of all message entities sent by this user
     */
    List<MessageEntity> findAllBySenderIdOrderByCreatedAtDesc(UUID senderId);

    /**
     * Find all messages by delivery status, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all message states.
     * Used for system-wide delivery analytics.</p>
     *
     * @param status the message status to filter by
     * @return list of all message entities with the given status
     */
    List<MessageEntity> findAllByStatusOrderByCreatedAtDesc(MessageEntity.Status status);

    /**
     * Find all reply messages for a parent, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all reply states.
     * Used for full thread reconstruction or audit.</p>
     *
     * @param parentId the ID of the parent message
     * @return list of all reply message entities
     */
    List<MessageEntity> findAllByParentIdOrderByCreatedAtAsc(UUID parentId);

    /**
     * Bulk lookup messages by multiple IDs, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all matching messages.
     * Used for batch operations that need full state.</p>
     *
     * @param messageIds collection of message UUIDs
     * @return list of matching message entities (active + deleted)
     */
    List<MessageEntity> findAllByIdIn(Collection<UUID> messageIds);

    /**
     * Check existence by room and message ID regardless of deletion status.
     *
     * <p>No {@code isDeleted} filter — checks all historical messages.
     * Used for message-room relationship validation.</p>
     *
     * @param roomId the UUID of the room
     * @param messageId the UUID of the message
     * @return {@code true} if a message with the given ID exists in the room
     */
    boolean existsByIdAndRoomId(UUID messageId, UUID roomId);

    /**
     * Check existence by room and sender regardless of deletion status.
     *
     * <p>No {@code isDeleted} filter — checks all historical messages.
     * Used for permission checks or UI logic.</p>
     *
     * @param roomId the UUID of the room
     * @param senderId the UUID of the sender
     * @return {@code true} if the sender has any messages in the room
     */
    boolean existsByRoomIdAndSenderId(UUID roomId, UUID senderId);

    /**
     * Check if a message has any replies regardless of deletion status.
     *
     * <p>No {@code isDeleted} filter — checks all historical replies.
     * Used for admin/audit reply counts.</p>
     *
     * @param parentId the ID of the parent message
     * @return {@code true} if any reply (active or deleted) exists
     */
    boolean existsByParentId(UUID parentId);
}