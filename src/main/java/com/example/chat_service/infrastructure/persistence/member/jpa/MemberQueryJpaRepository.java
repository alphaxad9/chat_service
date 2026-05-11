// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/jpa/MemberQueryJpaRepository.java
package com.example.chat_service.infrastructure.persistence.members.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.members.MemberEntity;

/**
 * Spring Data JPA repository for query-side (read-only) operations on {@link MemberEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code MemberQueryOrmRepository} by providing
 * optimized read operations for the CQRS query side. All methods return active members only
 * (isLeft = false) via explicit method name derivation.</p>
 *
 * <p><strong>Soft-delete handling:</strong> All methods include {@code AndIsLeftFalse}
 * in their names to ensure only active memberships are returned. No {@code @SQLRestriction}
 * or custom {@code @Query} is used — filtering is explicit and visible in method signatures.</p>
 *
 * <p><strong>Pure ORM derivation:</strong> All queries use Spring Data JPA's method-name
 * derivation. No JPQL/SQL strings — type-safe, refactor-friendly, and IDE-autocompleted.</p>
 *
 * <p><strong>Not for write operations:</strong> This repository is read-only. For command
 * operations (save, update, delete), use {@code MemberCommandJpaRepository}.</p>
 */
@Repository
public interface MemberQueryJpaRepository extends JpaRepository<MemberEntity, UUID> {

    // ── Single Entity Queries (Active Members Only) ──────────────────

    /**
     * Find an active member by ID.
     */
    Optional<MemberEntity> findByIdAndIsLeftFalse(UUID id);

    /**
     * Find an active member by user+room relationship.
     */
    Optional<MemberEntity> findByUserIdAndRoomIdAndIsLeftFalse(UUID userId, UUID roomId);

    /**
     * Check if an active membership exists for user+room.
     */
    boolean existsByUserIdAndRoomIdAndIsLeftFalse(UUID userId, UUID roomId);

    // ── Bulk Queries by Room (Active Members Only) ───────────────────

    /**
     * Load all active members in a room.
     */
    List<MemberEntity> findAllByRoomIdAndIsLeftFalse(UUID roomId);

    /**
     * Load all active ADMIN members in a room.
     */
    List<MemberEntity> findAllByRoomIdAndStatusAndIsLeftFalse(
            UUID roomId,
            MemberEntity.MemberStatus status
    );

    /**
     * Count active members in a room.
     */
    long countByRoomIdAndIsLeftFalse(UUID roomId);

    // ── Bulk Queries by User (Active Memberships Only) ───────────────

    /**
     * Load all active memberships for a user across all rooms.
     */
    List<MemberEntity> findAllByUserIdAndIsLeftFalse(UUID userId);

    /**
     * Load all active ADMIN memberships for a user.
     */
    List<MemberEntity> findAllByUserIdAndStatusAndIsLeftFalse(
            UUID userId,
            MemberEntity.MemberStatus status
    );

    /**
     * Count active rooms a user participates in.
     */
    long countByUserIdAndIsLeftFalse(UUID userId);

    // ── Batch Lookup Queries (Active Members Only) ───────────────────

    /**
     * Bulk lookup active members for multiple users in a specific room.
     */
    List<MemberEntity> findAllByRoomIdAndUserIdInAndIsLeftFalse(
            UUID roomId,
            Collection<UUID> userIds
    );

    /**
     * Bulk lookup active members by their membership IDs.
     */
    List<MemberEntity> findAllByIdInAndIsLeftFalse(Collection<UUID> ids);

    // ── Projection Support (fetch entities, project in Java) ─────────
    // Note: MemberSummary projection is handled in MemberQueryOrmRepository
    // by mapping MemberEntity → MemberSummary in plain Java code.
    // This avoids JPQL constructor expressions and keeps queries pure ORM.
}
