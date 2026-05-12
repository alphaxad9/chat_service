now also update these two files (// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/repositories/RoomCommandOrmRepository.java
package com.example.chat_service.infrastructure.persistence.rooms.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.RoomAggregate;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomEntityError;
import com.example.chat_service.domain.rooms.exceptions.RoomAlreadyExistsError;
import com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError;
import com.example.chat_service.domain.rooms.repositories.RoomCommandRepository;
import com.example.chat_service.infrastructure.persistence.rooms.RoomEntity;
import com.example.chat_service.infrastructure.persistence.rooms.RoomMapper;
import com.example.chat_service.infrastructure.persistence.rooms.jpa.RoomCommandJpaRepository;

/**
 * JPA/Hibernate implementation of {@link RoomCommandRepository}.
 *
 * <p>Handles write-side operations for Room aggregates using Spring Data JPA.
 * Leverages {@link RoomCommandJpaRepository} for persistence and {@link RoomMapper}
 * for domain ↔ entity conversion.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isDeleted} is handled
 * explicitly via method names. Methods without {@code AndIsDeletedFalse} suffix
 * return all rooms (active + deleted); methods with the suffix return active-only.</p>
 *
 * <p><strong>Transaction management:</strong> All methods run within a transaction
 * via class-level {@code @Transactional}. Rollback occurs automatically on
 * unchecked exceptions, preserving aggregate consistency.</p>
 *
 * <p><strong>Exception mapping:</strong> Database constraint violations are
 * translated to domain-specific exceptions for clean error handling in application layer.</p>
 */
@Repository
@Transactional
public class RoomCommandOrmRepository implements RoomCommandRepository {

    private final RoomCommandJpaRepository roomJpaRepository;

    public RoomCommandOrmRepository(RoomCommandJpaRepository roomJpaRepository) {
        this.roomJpaRepository = roomJpaRepository;
    }

    @Override
    public void save(RoomAggregate aggregate) {
        RoomEntity entity = RoomMapper.aggregateToEntity(aggregate);
        
        try {
            // JPA merge pattern: save handles both insert and update
            // If entity with ID exists → UPDATE; otherwise → INSERT
            roomJpaRepository.save(entity);
            
        } catch (DataIntegrityViolationException e) {
            // Map database constraint violations to domain exceptions
            String errorMsg = e.getRootCause() != null 
                ? e.getRootCause().getMessage().toLowerCase() 
                : e.getMessage().toLowerCase();
            
            // Check for duplicate room ID (should be rare with UUIDs, but possible in tests)
            if (errorMsg.contains("rooms_pkey") || 
                (errorMsg.contains("duplicate") && errorMsg.contains("key") && errorMsg.contains("rooms"))) {
                throw new RoomAlreadyExistsError(
                    aggregate.room().id(),
                    aggregate.room().creatorId(),
                    aggregate.room().type().name(),
                    "Room with ID " + aggregate.room().id() + " already exists"
                );
            }
            
            // Check for NOT NULL constraints on required fields
            if (errorMsg.contains("creator_id") && errorMsg.contains("null")) {
                throw new InvalidRoomEntityError(
                    aggregate.room().id(),
                    null,
                    aggregate.room().type().name(),
                    "Database constraint violated: creator_id cannot be null"
                );
            }
            if (errorMsg.contains("type") && errorMsg.contains("null")) {
                throw new InvalidRoomEntityError(
                    aggregate.room().id(),
                    aggregate.room().creatorId(),
                    null,
                    "Database constraint violated: type cannot be null"
                );
            }
            if (errorMsg.contains("last_activity_at") && errorMsg.contains("null")) {
                throw new InvalidRoomEntityError(
                    aggregate.room().id(),
                    aggregate.room().creatorId(),
                    aggregate.room().type().name(),
                    "Database constraint violated: last_activity_at cannot be null"
                );
            }
            
            // Re-throw as generic integrity error if no specific mapping
            throw new DataIntegrityViolationException(
                "Failed to persist room " + aggregate.room().id() + ": " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public RoomAggregate load(UUID roomId) {
        try {
            // Standard findById returns all rooms; we filter to active-only for command operations
            // If you need to allow operations on deleted rooms, use loadOptional instead
            RoomEntity entity = roomJpaRepository.findById(roomId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new RoomNotFoundError(
                    roomId,
                    "Room not found or already deleted"
                ));
            
            return RoomMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            RoomNotFoundError notFound = new RoomNotFoundError(
                roomId,
                "Room not found"
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public Optional<RoomAggregate> loadOptional(UUID roomId) {
        // Base findById returns all rooms (active + deleted); caller decides how to handle
        return roomJpaRepository.findById(roomId)
            .map(RoomMapper::entityToAggregate);
    }

    @Override
    public boolean exists(UUID roomId) {
        // Base existsById checks all rooms (active + deleted)
        return roomJpaRepository.existsById(roomId);
    }

    @Override
    public boolean existsByCreatorAndType(UUID creatorId, Room.Type type) {
        // Base method checks all rooms (active + deleted) for duplicate prevention
        return roomJpaRepository.existsByCreatorIdAndType(
            creatorId,
            RoomEntity.RoomType.fromDomain(type)
        );
    }

    // ── Bulk Load Operations ───────────────────────────────────────────

    @Override
    public List<RoomAggregate> bulkLoadByCreatorId(UUID creatorId) {
        // Base findAllByCreatorId returns ALL rooms (active + deleted) for admin/audit
        List<RoomEntity> entities = roomJpaRepository.findAllByCreatorId(creatorId);
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveByCreatorId(UUID creatorId) {
        // Active-only variant filters isDeleted = false via method name derivation
        List<RoomEntity> entities = roomJpaRepository.findAllByCreatorIdAndIsDeletedFalseOrderByLastActivityAtDesc(creatorId);
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadByType(Room.Type type) {
        // Base findAllByType returns ALL rooms of type (active + deleted)
        RoomEntity.RoomType persistenceType = RoomEntity.RoomType.fromDomain(type);
        List<RoomEntity> entities = roomJpaRepository.findAllByType(persistenceType);
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveByType(Room.Type type) {
        // Active-only variant filters isDeleted = false via method name derivation
        RoomEntity.RoomType persistenceType = RoomEntity.RoomType.fromDomain(type);
        List<RoomEntity> entities = roomJpaRepository.findAllByTypeAndIsDeletedFalseOrderByLastActivityAtDesc(persistenceType);
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadByIds(Collection<UUID> roomIds) {
        // Base findAllByIdIn returns ALL matching rooms (active + deleted)
        List<RoomEntity> entities = roomJpaRepository.findAllByIdIn(roomIds);
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveByIds(Collection<UUID> roomIds) {
        // Active-only variant filters isDeleted = false via method name derivation
        List<RoomEntity> entities = roomJpaRepository.findAllByIdInAndIsDeletedFalse(roomIds);
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadGroupsByNamePrefix(String groupNamePrefix) {
        // Base method returns ALL GROUP rooms with name prefix (active + deleted)
        // Case-insensitive via IgnoringCase keyword in method name
        List<RoomEntity> entities = roomJpaRepository.findAllByTypeAndGroupNameStartingWithIgnoreCase(
            RoomEntity.RoomType.GROUP,
            groupNamePrefix != null ? groupNamePrefix : ""
        );
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveGroupsByNamePrefix(String groupNamePrefix) {
        // Active-only variant with case-insensitive name prefix matching
        List<RoomEntity> entities = roomJpaRepository.findAllByTypeAndGroupNameStartingWithIgnoreCaseAndIsDeletedFalseOrderByLastActivityAtDesc(
            RoomEntity.RoomType.GROUP,
            groupNamePrefix != null ? groupNamePrefix : ""
        );
        return entities.stream()
            .map(RoomMapper::entityToAggregate)
            .collect(Collectors.toList());
    }
})(// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/jpa/RoomCommandJpaRepository.java
package com.example.chat_service.infrastructure.persistence.rooms.jpa;

import java.util.Collection;
import java.util.List;
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
}) add   boolean existsByCreatorAndFriendId(UUID creatorId, UUID friendId);
 and   Optional<RoomAggregate> loadByCreatorAndFriendId(UUID creatorId, UUID friendId);
 and add friendId where its left out, give me these two full code files