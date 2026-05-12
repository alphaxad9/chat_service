// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/jpa/RoomQueryJpaRepository.java
package com.example.chat_service.infrastructure.persistence.rooms.jpa;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.rooms.RoomEntity;

/**
 * Spring Data JPA repository for query-side (read-only) operations on {@link RoomEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code RoomQueryOrmRepository} by providing
 * optimized read operations for the CQRS query side. All methods return active rooms only
 * ({@code isDeleted = false}) via explicit method name derivation.</p>
 *
 * <p><strong>Soft-delete handling:</strong> All query methods include {@code AndIsDeletedFalse}
 * in their names to ensure only active rooms are returned. No {@code @SQLRestriction}
 * or custom {@code @Query} is used — filtering is explicit and visible in method signatures.</p>
 *
 * <p><strong>Pure ORM derivation:</strong> All queries use Spring Data JPA's method-name
 * derivation. No JPQL/SQL strings — type-safe, refactor-friendly, and IDE-autocompleted.</p>
 *
 * <p><strong>Not for write operations:</strong> This repository is read-only. For command
 * operations (save, update, delete), use {@code RoomCommandJpaRepository}.</p>
 */
@Repository
public interface RoomQueryJpaRepository extends JpaRepository<RoomEntity, UUID> {

    // ── Single Entity Queries (Active Rooms Only) ──────────────────

    /**
     * Find an active room by ID.
     */
    Optional<RoomEntity> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Check if a room exists and is active.
     */
    boolean existsByIdAndIsDeletedFalse(UUID id);

    // ── Bulk Queries by Creator (Active Rooms Only) ───────────────────

    /**
     * Load all active rooms created by a specific user.
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByCreatorIdAndIsDeletedFalseOrderByLastActivityAtDesc(UUID creatorId);

    /**
     * Load all active rooms of a specific type created by a specific user.
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByCreatorIdAndTypeAndIsDeletedFalseOrderByLastActivityAtDesc(
            UUID creatorId,
            RoomEntity.RoomType type
    );

    /**
     * Count active rooms created by a user.
     */
    long countByCreatorIdAndIsDeletedFalse(UUID creatorId);

    /**
     * Load active rooms created by multiple users.
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByCreatorIdInAndIsDeletedFalseOrderByLastActivityAtDesc(
            Collection<UUID> creatorIds
    );

    // ── Bulk Queries by Type (Active Rooms Only) ───────────────────

    /**
     * Load all active rooms of a specific type.
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByTypeAndIsDeletedFalseOrderByLastActivityAtDesc(
            RoomEntity.RoomType type
    );

    /**
     * Load active GROUP rooms with name prefix (case-insensitive).
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByTypeAndGroupNameStartingWithIgnoreCaseAndIsDeletedFalseOrderByLastActivityAtDesc(
            RoomEntity.RoomType type,
            String groupNamePrefix
    );

    /**
     * Count active rooms of a specific type.
     */
    long countByTypeAndIsDeletedFalse(RoomEntity.RoomType type);

    // ── Bulk Lookup Queries (Active Rooms Only) ───────────────────

    /**
     * Bulk lookup active rooms by their IDs.
     */
    List<RoomEntity> findAllByIdInAndIsDeletedFalse(Collection<UUID> ids);

    // ── Activity-Based Queries (Active Rooms Only) ───────────────────

    /**
     * Load active rooms with lastActivityAt >= sinceTimestamp.
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByLastActivityAtGreaterThanEqualAndIsDeletedFalseOrderByLastActivityAtDesc(
            LocalDateTime sinceTimestamp
    );

    /**
     * Load all active rooms ordered by last activity.
     * Ordered by lastActivityAt descending.
     */
    List<RoomEntity> findAllByIsDeletedFalseOrderByLastActivityAtDesc();
}