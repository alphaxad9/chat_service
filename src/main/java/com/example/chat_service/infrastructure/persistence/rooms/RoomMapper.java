// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/RoomMapper.java
package com.example.chat_service.infrastructure.persistence.rooms;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.RoomAggregate;

/**
 * Handles mapping between domain aggregates, JPA entities, and domain value objects.
 *
 * <ul>
 *   <li>{@link #aggregateToEntity(RoomAggregate)}: RoomAggregate → RoomEntity (for persistence)</li>
 *   <li>{@link #entityToAggregate(RoomEntity)}: RoomEntity → RoomAggregate (for command loading)</li>
 *   <li>{@link #entityToDomain(RoomEntity)}: RoomEntity → Room (for query responses)</li>
 * </ul>
 *
 * <p>Keeps domain logic pure by isolating persistence concerns in infrastructure layer.
 * All methods are static utilities — no state, no dependencies.</p>
 */
public final class RoomMapper {

    // Prevent instantiation
    private RoomMapper() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Convert a write-side RoomAggregate into a JPA-persistable entity.
     * Used by command repositories to save aggregate state after business operations.
     *
     * @param aggregate the domain aggregate containing current state
     * @return JPA entity ready for persistence
     */
    public static RoomEntity aggregateToEntity(RoomAggregate aggregate) {
        Room room = aggregate.room();
        
        return new RoomEntity(
            room.id(),
            room.creatorId(),
            RoomEntity.RoomType.fromDomain(room.type()),
            room.groupName(),
            room.description(),
            room.coverImageUrl(),
            room.profileImageUrl(),
            room.lastActivityAt(),
            room.createdAt(),
            room.updatedAt(),
            room.isDeleted()
        );
        // Note: createdAt/updatedAt/isDeleted are managed by:
        // - @CreationTimestamp / @UpdateTimestamp annotations
        // - Repository methods for soft-delete (markDeleted/restore)
        // If loading existing entity, use entityToAggregate then save (JPA merge pattern)
    }

    /**
     * Reconstruct a write-side RoomAggregate from a JPA entity.
     * Used by command repository's load() method to hydrate aggregate for mutation.
     *
     * @param entity the persisted JPA entity
     * @return RoomAggregate ready for business operations
     */
    public static RoomAggregate entityToAggregate(RoomEntity entity) {
        Room domain = entityToDomain(entity);
        return RoomAggregate.fromEntity(domain);
    }

    /**
     * Convert a JPA entity into the immutable domain value object.
     * Used exclusively by query services to return clean, serializable data.
     *
     * @param entity the persisted JPA entity
     * @return immutable Room domain object
     */
    public static Room entityToDomain(RoomEntity entity) {
        // Use Room constructor directly since we're mapping from trusted persistence layer
        // Validation already enforced at domain creation time
        return new Room(
            entity.getId(),
            entity.getCreatorId(),
            entity.getType().toDomain(),
            entity.getGroupName(),
            entity.getDescription(),
            entity.getCoverImageUrl(),
            entity.getProfileImageUrl(),
            entity.getLastActivityAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.isDeleted()
        );
    }

    /**
     * Convenience: Convert domain Room directly to entity (bypassing aggregate).
     * Useful for read-model sync or event-sourcing projections.
     *
     * @param domain the immutable Room value object
     * @return JPA entity ready for persistence
     */
    public static RoomEntity domainToEntity(Room domain) {
        return new RoomEntity(
            domain.id(),
            domain.creatorId(),
            RoomEntity.RoomType.fromDomain(domain.type()),
            domain.groupName(),
            domain.description(),
            domain.coverImageUrl(),
            domain.profileImageUrl(),
            domain.lastActivityAt(),
            domain.createdAt(),
            domain.updatedAt(),
            domain.isDeleted()
        );
    }
}