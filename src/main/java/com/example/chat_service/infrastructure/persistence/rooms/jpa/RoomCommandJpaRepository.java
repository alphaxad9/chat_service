// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/jpa/RoomCommandJpaRepository.java
package com.example.chat_service.infrastructure.persistence.rooms.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.rooms.RoomEntity;

/**
 * Spring Data JPA repository for command-side (write) operations on {@link RoomEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code RoomCommandOrmRepository} by providing
 * type-safe, derived-query methods for aggregate persistence and retrieval.
 * <strong>No custom JPQL/SQL</strong> — all methods use Spring Data JPA's method-name derivation only.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isDeleted} is handled
 * explicitly via method names (e.g., {@code ...AndIsDeletedFalse()}) rather than
 * {@code @SQLRestriction}. This allows the same repository to support both
 * active-only and all-rooms queries using pure ORM derivation.</p>
 *
 * <p><strong>Unique constraint:</strong> Database-level uniqueness on room ID is enforced by primary key.
 * Application logic should check existence before creation to avoid {@code DataIntegrityViolationException}.</p>
 *
 * <p><strong>Not for read-side queries:</strong> This repository is optimized for
 * loading full aggregates for mutation. For read-only views, lists, or projections,
 * use {@code RoomQueryJpaRepository} when implemented.</p>
 */
@Repository
public interface RoomCommandJpaRepository extends JpaRepository<RoomEntity, UUID> {

    // ── Inherited Methods from JpaRepository<RoomEntity, UUID> ─────────
    // Basic CRUD operations (no isDeleted filtering — caller decides):
    //
    // • Optional<RoomEntity> findById(UUID id)
    //   → Loads entity by ID regardless of isDeleted status
    //
    // • <S extends RoomEntity> S save(S entity)
    //   → INSERT if new ID, UPDATE if ID exists (JPA merge pattern)
    //
    // • boolean existsById(UUID id)
    //   → Fast existence check regardless of isDeleted status

    // ── Derived Query Methods: Active Rooms Only (isDeleted = false) ──

    /**
     * Find an active room by creator ID and type.
     *
     * <p>Filters to {@code isDeleted = false} via method name derivation.
     * Used by {@code existsByCreatorAndType()} in command repository.</p>
     *
     * @param creatorId the UUID of the creator
     * @param type the room type (GROUP or DIRECT)
     * @return {@code true} if an active room exists with matching criteria
     */
    boolean existsByCreatorIdAndTypeAndIsDeletedFalse(UUID creatorId, RoomEntity.RoomType type);

    /**
     * Find an active DIRECT room by creator ID and friend ID.
     *
     * <p>Filters to {@code isDeleted = false} and {@code type = DIRECT} via method name derivation.
     * Used by {@code loadByCreatorAndFriendId()} in command repository.</p>
     *
     * @param creatorId the UUID of the room creator
     * @param friendId the UUID of the other participant in the direct conversation
     * @param type the room type (should be DIRECT)
     * @return Optional containing the room entity if found, empty otherwise
     */
    Optional<RoomEntity> findByCreatorIdAndFriendIdAndTypeAndIsDeletedFalse(
            UUID creatorId, UUID friendId, RoomEntity.RoomType type);

    /**
     * Check existence of a DIRECT room by creator ID and friend ID.
     *
     * <p>Checks all rooms regardless of deletion status.
     * Used by {@code existsByCreatorAndFriendId()} in command repository.</p>
     *
     * @param creatorId the UUID of the room creator
     * @param friendId the UUID of the other participant
     * @param type the room type (should be DIRECT)
     * @return {@code true} if a room (active or deleted) exists with matching criteria
     */
    boolean existsByCreatorIdAndFriendIdAndType(UUID creatorId, UUID friendId, RoomEntity.RoomType type);

    /**
     * Load all active rooms created by a specific user.
     *
     * <p>Filters to {@code isDeleted = false} via method name derivation.
     * Used by {@code bulkLoadActiveByCreatorId()}.
     * Results ordered by lastActivityAt descending for common UI patterns.</p>
     *
     * @param creatorId the UUID of the creator
     * @return list of active room entities only
     */
    List<RoomEntity> findAllByCreatorIdAndIsDeletedFalseOrderByLastActivityAtDesc(UUID creatorId);

    /**
     * Load all active rooms of a specific type.
     *
     * <p>Filters to {@code isDeleted = false} via method name derivation.
     * Used by {@code bulkLoadActiveByType()}.
     * Results ordered by lastActivityAt descending.</p>
     *
     * @param type the room type to filter
     * @return list of active room entities of the given type
     */
    List<RoomEntity> findAllByTypeAndIsDeletedFalseOrderByLastActivityAtDesc(RoomEntity.RoomType type);

    /**
     * Bulk lookup: find active rooms for multiple room IDs.
     *
     * <p>Filters to {@code isDeleted = false} via method name derivation.
     * Used by {@code bulkLoadActiveByIds()}.</p>
     *
     * @param roomIds collection of room UUIDs to lookup
     * @return list of matching active room entities only
     */
    List<RoomEntity> findAllByIdInAndIsDeletedFalse(Collection<UUID> roomIds);

    /**
     * Load active GROUP rooms with optional name prefix filtering.
     *
     * <p>Filters to {@code isDeleted = false} and {@code type = GROUP} via method name derivation.
     * Used by {@code bulkLoadActiveGroupsByNamePrefix()}.
     * Case-insensitive prefix match via {@code IgnoringCase} keyword.</p>
     *
     * @param groupNamePrefix prefix to match (case-insensitive)
     * @return list of active GROUP room entities with matching name prefix
     */
    List<RoomEntity> findAllByTypeAndGroupNameStartingWithIgnoreCaseAndIsDeletedFalseOrderByLastActivityAtDesc(
            RoomEntity.RoomType type,
            String groupNamePrefix
    );

    // ── Derived Query Methods: All Rooms (including deleted) ───────────

    /**
     * Load all rooms created by a specific user, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all room states.
     * Used by {@code bulkLoadByCreatorId()} for admin/audit operations.</p>
     *
     * @param creatorId the UUID of the creator
     * @return list of all room entities (active + deleted)
     */
    List<RoomEntity> findAllByCreatorId(UUID creatorId);

    /**
     * Load all rooms of a specific type, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all room states.
     * Used by {@code bulkLoadByType()} for system-wide operations.</p>
     *
     * @param type the room type
     * @return list of all room entities of given type (active + deleted)
     */
    List<RoomEntity> findAllByType(RoomEntity.RoomType type);

    /**
     * Bulk lookup: find rooms for multiple IDs, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all matching rooms.
     * Used by {@code bulkLoadByIds()} for batch operations.</p>
     *
     * @param roomIds collection of room UUIDs
     * @return list of matching room entities (active + deleted)
     */
    List<RoomEntity> findAllByIdIn(Collection<UUID> roomIds);

    /**
     * Load all GROUP rooms with optional name prefix, including deleted ones.
     *
     * <p>No {@code isDeleted} filter — returns all matching rooms.
     * Used by {@code bulkLoadGroupsByNamePrefix()} for admin search.
     * Case-insensitive prefix match via {@code IgnoringCase} keyword.</p>
     *
     * @param groupNamePrefix prefix to match (case-insensitive)
     * @return list of GROUP room entities with matching name (active + deleted)
     */
    List<RoomEntity> findAllByTypeAndGroupNameStartingWithIgnoreCase(
            RoomEntity.RoomType type,
            String groupNamePrefix
    );

    /**
     * Check existence by creator+type regardless of deletion status.
     *
     * <p>No {@code isDeleted} filter — checks all historical rooms.
     * Used by {@code existsByCreatorAndType()} for duplicate prevention.</p>
     *
     * @param creatorId the UUID of the creator
     * @param type the room type
     * @return {@code true} if any room (active or deleted) matches
     */
    boolean existsByCreatorIdAndType(UUID creatorId, RoomEntity.RoomType type);
}