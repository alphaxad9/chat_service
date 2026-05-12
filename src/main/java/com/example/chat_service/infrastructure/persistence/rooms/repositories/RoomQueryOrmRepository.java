// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/repositories/RoomQueryOrmRepository.java
package com.example.chat_service.infrastructure.persistence.rooms.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.repositories.RoomQueryRepository;
import com.example.chat_service.infrastructure.persistence.rooms.RoomEntity;
import com.example.chat_service.infrastructure.persistence.rooms.RoomMapper;
import com.example.chat_service.infrastructure.persistence.rooms.jpa.RoomQueryJpaRepository;

/**
 * JPA/Hibernate implementation of {@link RoomQueryRepository}.
 *
 * <p>Handles read-side operations for Room value objects using Spring Data JPA.
 * Leverages {@link RoomQueryJpaRepository} for persistence queries and {@link RoomMapper}
 * for entity → domain conversion.</p>
 *
 * <p><strong>CQRS read-side:</strong> Returns immutable {@link Room} domain objects
 * (not aggregates) optimized for query responses. All methods automatically filter
 * to active rooms only ({@code isDeleted = false}).</p>
 *
 * <p><strong>Projection handling:</strong> {@code RoomSummary} projections are created
 * in plain Java code by mapping fetched {@code RoomEntity} instances. This avoids
 * JPQL constructor expressions and keeps all queries as pure ORM method derivations.</p>
 *
 * <p><strong>Transaction management:</strong> Methods are {@code @Transactional(readOnly = true)}
 * to optimize database access patterns and signal intent to the persistence layer.</p>
 */
@Repository
@Transactional(readOnly = true)
public class RoomQueryOrmRepository implements RoomQueryRepository {

    private final RoomQueryJpaRepository roomQueryJpaRepository;

    public RoomQueryOrmRepository(RoomQueryJpaRepository roomQueryJpaRepository) {
        this.roomQueryJpaRepository = roomQueryJpaRepository;
    }

    // ── Single Entity Queries (Active Rooms Only) ──────────────────

    @Override
    public Optional<Room> findById(UUID roomId) {
        return roomQueryJpaRepository.findByIdAndIsDeletedFalse(roomId)
            .map(RoomMapper::entityToDomain);
    }

    @Override
    public Optional<Room> findByIdIncludingDeleted(UUID roomId) {
        // Base findById from JpaRepository returns all rooms; caller decides how to handle
        return roomQueryJpaRepository.findById(roomId)
            .map(RoomMapper::entityToDomain);
    }

    @Override
    public boolean isActiveRoom(UUID roomId) {
        return roomQueryJpaRepository.existsByIdAndIsDeletedFalse(roomId);
    }

    // ── Bulk Queries by Creator (Active Rooms Only) ───────────────────

    @Override
    public List<Room> findAllActiveByCreatorId(UUID creatorId) {
        return roomQueryJpaRepository.findAllByCreatorIdAndIsDeletedFalseOrderByLastActivityAtDesc(creatorId).stream()
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Room> findActiveGroupsByCreatorId(UUID creatorId) {
        return roomQueryJpaRepository.findAllByCreatorIdAndTypeAndIsDeletedFalseOrderByLastActivityAtDesc(
                creatorId, RoomEntity.RoomType.GROUP).stream()
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Room> findActiveDirectsByCreatorId(UUID creatorId) {
        return roomQueryJpaRepository.findAllByCreatorIdAndTypeAndIsDeletedFalseOrderByLastActivityAtDesc(
                creatorId, RoomEntity.RoomType.DIRECT).stream()
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countActiveByCreatorId(UUID creatorId) {
        return roomQueryJpaRepository.countByCreatorIdAndIsDeletedFalse(creatorId);
    }

    // ── Bulk Queries by Type (Active Rooms Only) ───────────────────

    @Override
    public List<Room> findAllActiveByType(Room.Type type) {
        RoomEntity.RoomType persistenceType = RoomEntity.RoomType.fromDomain(type);
        return roomQueryJpaRepository.findAllByTypeAndIsDeletedFalseOrderByLastActivityAtDesc(persistenceType).stream()
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Room> findActiveGroupsByNamePrefix(String groupNamePrefix, int limit) {
        String prefix = groupNamePrefix != null ? groupNamePrefix : "";
        List<RoomEntity> entities = roomQueryJpaRepository
            .findAllByTypeAndGroupNameStartingWithIgnoreCaseAndIsDeletedFalseOrderByLastActivityAtDesc(
                RoomEntity.RoomType.GROUP, prefix);
        // Apply limit in Java since Spring Data method-name derivation doesn't support dynamic limits
        return entities.stream()
            .limit(limit)
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countActiveByType(Room.Type type) {
        RoomEntity.RoomType persistenceType = RoomEntity.RoomType.fromDomain(type);
        return roomQueryJpaRepository.countByTypeAndIsDeletedFalse(persistenceType);
    }

    // ── Bulk Lookup Queries (Active Rooms Only) ───────────────────

    @Override
    public List<Room> findActiveByIds(Collection<UUID> roomIds) {
        return roomQueryJpaRepository.findAllByIdInAndIsDeletedFalse(roomIds).stream()
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Room> findActiveByCreatorIds(Collection<UUID> creatorIds) {
        return roomQueryJpaRepository.findAllByCreatorIdInAndIsDeletedFalseOrderByLastActivityAtDesc(creatorIds).stream()
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Activity-Based Queries (Active Rooms Only) ───────────────────

    @Override
    public List<Room> findActiveWithRecentActivity(LocalDateTime sinceTimestamp, int limit) {
        List<RoomEntity> entities = roomQueryJpaRepository
            .findAllByLastActivityAtGreaterThanEqualAndIsDeletedFalseOrderByLastActivityAtDesc(sinceTimestamp);
        // Apply limit in Java
        return entities.stream()
            .limit(limit)
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Room> findActiveOrderedByActivity(int limit) {
        List<RoomEntity> entities = roomQueryJpaRepository.findAllByIsDeletedFalseOrderByLastActivityAtDesc();
        // Apply limit in Java
        return entities.stream()
            .limit(limit)
            .map(RoomMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Projection Queries (Lightweight Read Models) ─────────────────
    // Handled in plain Java: fetch entities, then map to RoomSummary

    @Override
    public List<RoomSummary> findActiveSummariesByCreatorId(UUID creatorId) {
        // Fetch active entities using pure ORM derivation
        List<RoomEntity> entities = roomQueryJpaRepository
            .findAllByCreatorIdAndIsDeletedFalseOrderByLastActivityAtDesc(creatorId);
        // Project to RoomSummary in Java (no JPQL constructor needed)
        return entities.stream()
            .map(e -> new RoomSummary(
                e.getId(),
                e.getCreatorId(),
                e.getType().toDomain(),
                e.getGroupName(),
                e.getDescription(),
                e.getCoverImageUrl(),
                e.getProfileImageUrl(),
                e.getLastActivityAt(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomSummary> findActiveSummariesByIds(Collection<UUID> roomIds) {
        // Fetch active entities using pure ORM derivation
        List<RoomEntity> entities = roomQueryJpaRepository.findAllByIdInAndIsDeletedFalse(roomIds);
        // Project to RoomSummary in Java
        return entities.stream()
            .map(e -> new RoomSummary(
                e.getId(),
                e.getCreatorId(),
                e.getType().toDomain(),
                e.getGroupName(),
                e.getDescription(),
                e.getCoverImageUrl(),
                e.getProfileImageUrl(),
                e.getLastActivityAt(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public List<RoomSummary> findActiveGroupSummariesByNamePrefix(String groupNamePrefix, int limit) {
        String prefix = groupNamePrefix != null ? groupNamePrefix : "";
        // Fetch active GROUP entities with name prefix using pure ORM derivation
        List<RoomEntity> entities = roomQueryJpaRepository
            .findAllByTypeAndGroupNameStartingWithIgnoreCaseAndIsDeletedFalseOrderByLastActivityAtDesc(
                RoomEntity.RoomType.GROUP, prefix);
        // Apply limit and project to RoomSummary in Java
        return entities.stream()
            .limit(limit)
            .map(e -> new RoomSummary(
                e.getId(),
                e.getCreatorId(),
                e.getType().toDomain(),
                e.getGroupName(),
                e.getDescription(),
                e.getCoverImageUrl(),
                e.getProfileImageUrl(),
                e.getLastActivityAt(),
                e.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }
}