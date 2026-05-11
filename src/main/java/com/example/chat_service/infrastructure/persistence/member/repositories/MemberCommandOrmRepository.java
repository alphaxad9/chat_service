// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/repositories/MemberCommandOrmRepository.java
package com.example.chat_service.infrastructure.persistence.members.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.domain.members.exceptions.InvalidMemberEntityError;
import com.example.chat_service.domain.members.exceptions.MemberNotFoundError;
import com.example.chat_service.domain.members.repositories.MemberCommandRepository;
import com.example.chat_service.infrastructure.persistence.members.MemberEntity;
import com.example.chat_service.infrastructure.persistence.members.MemberMapper;
import com.example.chat_service.infrastructure.persistence.members.jpa.MemberCommandJpaRepository;

/**
 * JPA/Hibernate implementation of {@link MemberCommandRepository}.
 *
 * <p>Handles write-side operations for Member aggregates using Spring Data JPA.
 * Leverages {@link MemberCommandJpaRepository} for persistence and {@link MemberMapper}
 * for domain ↔ entity conversion.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isLeft} is handled
 * explicitly via method names (e.g., {@code ...AndIsLeftFalse()}) rather than
 * {@code @SQLRestriction}. This allows the same repository to support both
 * active-only and all-members queries using pure ORM derivation.</p>
 *
 * <p><strong>Unique constraint:</strong> The {@code (user_id, room_id)} unique
 * constraint is enforced at the database level. Application logic should check
 * {@code existsByUserAndRoom()} before creation to avoid constraint violations.</p>
 *
 * <p><strong>Transaction management:</strong> All methods run within a transaction
 * via class-level {@code @Transactional}. Rollback occurs automatically on
 * unchecked exceptions, preserving aggregate consistency.</p>
 */
@Repository
@Transactional
public class MemberCommandOrmRepository implements MemberCommandRepository {

    private final MemberCommandJpaRepository memberJpaRepository;

    public MemberCommandOrmRepository(MemberCommandJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public void save(MemberAggregate aggregate) {
        MemberEntity entity = MemberMapper.aggregateToEntity(aggregate);
        
        try {
            // JPA merge pattern: save handles both insert and update
            // If entity with ID exists → UPDATE; otherwise → INSERT
            memberJpaRepository.save(entity);
            
        } catch (DataIntegrityViolationException e) {
            // Map database constraint violations to domain exceptions
            String errorMsg = e.getRootCause() != null 
                ? e.getRootCause().getMessage().toLowerCase() 
                : e.getMessage().toLowerCase();
            
            // Check for unique constraint violation on (user_id, room_id)
            if (errorMsg.contains("uk_members_user_room") || 
                (errorMsg.contains("user_id") && errorMsg.contains("room_id") && errorMsg.contains("unique"))) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    aggregate.member().userId(),
                    aggregate.member().roomId(),
                    "Database constraint violated: user can only have one membership per room"
                );
            }
            
            // Check for NOT NULL constraints on required fields
            if (errorMsg.contains("user_id") && errorMsg.contains("null")) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    null,
                    aggregate.member().roomId(),
                    "Database constraint violated: user_id cannot be null"
                );
            }
            if (errorMsg.contains("room_id") && errorMsg.contains("null")) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    aggregate.member().userId(),
                    null,
                    "Database constraint violated: room_id cannot be null"
                );
            }
            if (errorMsg.contains("status") && errorMsg.contains("null")) {
                throw new InvalidMemberEntityError(
                    aggregate.member().id(),
                    aggregate.member().userId(),
                    aggregate.member().roomId(),
                    "Database constraint violated: status cannot be null"
                );
            }
            
            // Re-throw as generic integrity error if no specific mapping
            throw new DataIntegrityViolationException(
                "Failed to persist member " + aggregate.member().id() + ": " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public MemberAggregate load(UUID memberId) {
        try {
            // Load by ID regardless of isLeft status (caller decides if they want active-only)
            MemberEntity entity = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundError(
                    memberId,
                    null,
                    null,
                    "Member not found"
                ));
            
            return MemberMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            MemberNotFoundError notFound = new MemberNotFoundError(
                memberId,
                null,
                null,
                "Member not found"
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public MemberAggregate loadByUserAndRoom(UUID userId, UUID roomId) {
        try {
            // Use active-only query (isLeft = false) for domain operations
            MemberEntity entity = memberJpaRepository.findByUserIdAndRoomIdAndIsLeftFalse(userId, roomId)
                .orElseThrow(() -> new MemberNotFoundError(
                    null,
                    userId,
                    roomId,
                    "No active membership found for user " + userId + " in room " + roomId
                ));
            
            return MemberMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            MemberNotFoundError notFound = new MemberNotFoundError(
                null,
                userId,
                roomId,
                "No active membership found for user " + userId + " in room " + roomId
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public Optional<MemberAggregate> loadByUserAndRoomOptional(UUID userId, UUID roomId) {
        // Use active-only query (isLeft = false) for domain operations
        return memberJpaRepository.findByUserIdAndRoomIdAndIsLeftFalse(userId, roomId)
            .map(MemberMapper::entityToAggregate);
    }

    @Override
    public boolean exists(UUID memberId) {
        // Check existence regardless of isLeft status
        return memberJpaRepository.existsById(memberId);
    }

    @Override
    public boolean existsByUserAndRoom(UUID userId, UUID roomId) {
        // Returns true if user has an ACTIVE membership in the room
        return memberJpaRepository.existsByUserIdAndRoomIdAndIsLeftFalse(userId, roomId);
    }

    // ── Bulk Load Operations ───────────────────────────────────────────

    @Override
    public List<MemberAggregate> bulkLoadByRoomId(UUID roomId) {
        // Load ALL members in room (including left) for admin/audit operations
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomId(roomId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByRoomId(UUID roomId) {
        // Load only active members for common read patterns
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomIdAndIsLeftFalse(roomId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadByUserId(UUID userId) {
        // Load ALL memberships for user (including left) for history/export
        List<MemberEntity> entities = memberJpaRepository.findAllByUserId(userId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByUserId(UUID userId) {
        // Load only active memberships for UI displays
        List<MemberEntity> entities = memberJpaRepository.findAllByUserIdAndIsLeftFalse(userId);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        // Bulk lookup including left members for batch validation
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomIdAndUserIdIn(roomId, userIds);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        // Bulk lookup of active members only for messaging/presence features
        List<MemberEntity> entities = memberJpaRepository.findAllByRoomIdAndUserIdInAndIsLeftFalse(roomId, userIds);
        return entities.stream()
            .map(MemberMapper::entityToAggregate)
            .collect(Collectors.toList());
    }
}