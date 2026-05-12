package com.example.chat_service.infrastructure.persistence.messages.jpa;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.messages.MessageEntity;

/**
 * Spring Data JPA repository for query-side (read-only) operations on {@link MessageEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code MessageQueryOrmRepository} by providing
 * optimized read operations for the CQRS query side. All methods return active messages only
 * ({@code isDeleted = false}) via explicit method name derivation.</p>
 *
 * <p><strong>Soft-delete handling:</strong> All query methods include {@code AndIsDeletedFalse}
 * in their names to ensure only active messages are returned. No {@code @SQLRestriction}
 * or custom {@code @Query} is used — filtering is explicit and visible in method signatures.</p>
 *
 * <p><strong>Pure ORM derivation:</strong> All queries use Spring Data JPA's method-name
 * derivation. No JPQL/SQL strings — type-safe, refactor-friendly, and IDE-autocompleted.</p>
 *
 * <p><strong>Not for write operations:</strong> This repository is read-only. For command
 * operations (save, update, delete), use {@code MessageCommandJpaRepository}.</p>
 */
@Repository
public interface MessageQueryJpaRepository extends JpaRepository<MessageEntity, UUID> {

    // ── Single Entity Queries (Active Messages Only) ──────────────────

    /**
     * Find an active message by ID.
     */
    Optional<MessageEntity> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Check if a message exists and is active.
     */
    boolean existsByIdAndIsDeletedFalse(UUID id);

    // ── Latest Message Queries (Active Messages Only) ─────────────────

    /**
     * Find the most recent active message in a room.
     * Ordered by createdAt descending, limited to 1 result.
     */
    Optional<MessageEntity> findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID roomId);

    /**
     * Find the most recent active messages for multiple rooms.
     * Returns one message per room (the latest), ordered by room then createdAt desc.
     */
    List<MessageEntity> findFirstByRoomIdInAndIsDeletedFalseOrderByRoomIdCreatedAtDesc(Collection<UUID> roomIds);

    // ── Bulk Queries by Room (Active Messages Only) ───────────────────

    /**
     * Load all active messages in a room, ordered by creation ascending (chat history).
     */
    List<MessageEntity> findAllByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID roomId);

    /**
     * Load active messages in a room, limited by count, most recent first.
     */
    List<MessageEntity> findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID roomId, int limit);

    /**
     * Load active messages in a room created after a cursor timestamp.
     */
    List<MessageEntity> findByRoomIdAndIsDeletedFalseAndCreatedAtGreaterThanOrderByCreatedAtAsc(
            UUID roomId, LocalDateTime afterTimestamp, int limit);

    /**
     * Load active messages in a room created before a cursor timestamp.
     */
    List<MessageEntity> findByRoomIdAndIsDeletedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID roomId, LocalDateTime beforeTimestamp, int limit);

    /**
     * Count active messages in a room.
     */
    long countByRoomIdAndIsDeletedFalse(UUID roomId);

    // ── Bulk Queries by Sender (Active Messages Only) ─────────────────

    /**
     * Load all active messages sent by a user, ordered by creation descending.
     */
    List<MessageEntity> findAllBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID senderId);

    /**
     * Load active messages sent by a user in specific rooms.
     */
    List<MessageEntity> findAllBySenderIdAndRoomIdInAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID senderId, Collection<UUID> roomIds);

    /**
     * Count active messages sent by a user.
     */
    long countBySenderIdAndIsDeletedFalse(UUID senderId);

    // ── Bulk Queries by Status (Active Messages Only) ─────────────────

    /**
     * Load active messages by delivery status, ordered by creation descending.
     */
    List<MessageEntity> findAllByStatusAndIsDeletedFalseOrderByCreatedAtDesc(MessageEntity.Status status);

    /**
     * Load active messages by status in a specific room.
     */
    List<MessageEntity> findAllByRoomIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            UUID roomId, MessageEntity.Status status);

    /**
     * Load active SENT messages older than a threshold.
     */
    List<MessageEntity> findAllByStatusAndIsDeletedFalseAndCreatedAtBeforeOrderByCreatedAtAsc(
            MessageEntity.Status status, LocalDateTime olderThan);

    // ── Reply Chain Queries (Active Messages Only) ────────────────────

    /**
     * Load active reply messages for a parent, ordered by creation ascending.
     */
    List<MessageEntity> findAllByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId);

    /**
     * Load active reply messages for a parent, limited by count.
     */
    List<MessageEntity> findTopByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentId, int limit);

    /**
     * Check if an active message has any active replies.
     */
    boolean existsByParentIdAndIsDeletedFalse(UUID parentId);

    /**
     * Count active replies for a parent message.
     */
    long countByParentIdAndIsDeletedFalse(UUID parentId);

    // ── Bulk Lookup Queries (Active Messages Only) ───────────────────

    /**
     * Bulk lookup active messages by their IDs.
     */
    List<MessageEntity> findAllByIdInAndIsDeletedFalse(Collection<UUID> ids);

    /**
     * Bulk lookup active messages by room IDs, ordered by creation descending.
     */
    List<MessageEntity> findAllByRoomIdInAndIsDeletedFalseOrderByCreatedAtDesc(Collection<UUID> roomIds);

    // ── Activity/Time-Based Queries (Active Messages Only) ────────────

    /**
     * Load active messages created since a timestamp, limited by count.
     */
    List<MessageEntity> findByIsDeletedFalseAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            LocalDateTime sinceTimestamp, int limit);

    /**
     * Load active messages updated since a timestamp, limited by count.
     */
    List<MessageEntity> findByIsDeletedFalseAndUpdatedAtGreaterThanEqualOrderByUpdatedAtDesc(
            LocalDateTime sinceTimestamp, int limit);

    // ── Content/Image Queries (Active Messages Only) ──────────────────

    /**
     * Load active messages with image attachments in a room.
     */
    List<MessageEntity> findAllByRoomIdAndImageUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(UUID roomId);

    /**
     * Load active messages with image attachments by sender.
     */
    List<MessageEntity> findAllBySenderIdAndImageUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(UUID senderId);
}