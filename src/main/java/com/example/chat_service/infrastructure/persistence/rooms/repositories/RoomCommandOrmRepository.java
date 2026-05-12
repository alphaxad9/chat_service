// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/repositories/RoomCommandOrmRepository.java

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
    public Optional<RoomAggregate> loadByCreatorAndFriendId(UUID creatorId, UUID friendId) {
        // Find DIRECT room by creator and friend, active only
        return roomJpaRepository.findByCreatorIdAndFriendIdAndTypeAndIsDeletedFalse(
                creatorId, friendId, RoomEntity.RoomType.DIRECT)
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

    @Override
    public boolean existsByCreatorAndFriendId(UUID creatorId, UUID friendId) {
        // Check for DIRECT room existence between two users (all states)
        return roomJpaRepository.existsByCreatorIdAndFriendIdAndType(
            creatorId,
            friendId,
            RoomEntity.RoomType.DIRECT
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
}