
// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/repositories/MemberQueryOrmRepository.java
package com.example.chat_service.infrastructure.persistence.member.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.members.repositories.MemberQueryRepository;
import com.example.chat_service.infrastructure.persistence.member.MemberEntity;
import com.example.chat_service.infrastructure.persistence.member.MemberMapper;
import com.example.chat_service.infrastructure.persistence.member.jpa.MemberQueryJpaRepository;

/**
 * JPA/Hibernate implementation of {@link MemberQueryRepository}.
 *
 * <p>Handles read-side operations for Member value objects using Spring Data JPA.
 * Leverages {@link MemberQueryJpaRepository} for persistence queries and {@link MemberMapper}
 * for entity → domain conversion.</p>
 *
 * <p><strong>CQRS read-side:</strong> Returns immutable {@link Member} domain objects
 * (not aggregates) optimized for query responses. All methods automatically filter
 * to active members only ({@code isLeft = false}).</p>
 *
 * <p><strong>Projection handling:</strong> {@code MemberSummary} projections are created
 * in plain Java code by mapping fetched {@code MemberEntity} instances. This avoids
 * JPQL constructor expressions and keeps all queries as pure ORM method derivations.</p>
 *
 * <p><strong>Transaction management:</strong> Methods are {@code @Transactional(readOnly = true)}
 * to optimize database access patterns and signal intent to the persistence layer.</p>
 */
@Repository
@Transactional(readOnly = true)
public class MemberQueryOrmRepository implements MemberQueryRepository {

    private final MemberQueryJpaRepository memberQueryJpaRepository;

    public MemberQueryOrmRepository(MemberQueryJpaRepository memberQueryJpaRepository) {
        this.memberQueryJpaRepository = memberQueryJpaRepository;
    }

    // ── Single Entity Queries (Active Members Only) ──────────────────

    @Override
    public Optional<Member> findById(UUID memberId) {
        return memberQueryJpaRepository.findByIdAndIsLeftFalse(memberId)
            .map(MemberMapper::entityToDomain);
    }

    @Override
    public Optional<Member> findByUserIdAndRoomId(UUID userId, UUID roomId) {
        return memberQueryJpaRepository.findByUserIdAndRoomIdAndIsLeftFalse(userId, roomId)
            .map(MemberMapper::entityToDomain);
    }

    @Override
    public boolean isActiveMember(UUID userId, UUID roomId) {
        return memberQueryJpaRepository.existsByUserIdAndRoomIdAndIsLeftFalse(userId, roomId);
    }

    // ── Bulk Queries by Room (Active Members Only) ───────────────────

    @Override
    public List<Member> findAllActiveByRoomId(UUID roomId) {
        return memberQueryJpaRepository.findAllByRoomIdAndIsLeftFalse(roomId).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Member> findActiveAdminsByRoomId(UUID roomId) {
        return memberQueryJpaRepository.findAllByRoomIdAndStatusAndIsLeftFalse(
                roomId, MemberEntity.MemberStatus.ADMIN).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Member> findActiveUsersByRoomId(UUID roomId) {
        return memberQueryJpaRepository.findAllByRoomIdAndStatusAndIsLeftFalse(
                roomId, MemberEntity.MemberStatus.USER).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countActiveByRoomId(UUID roomId) {
        return memberQueryJpaRepository.countByRoomIdAndIsLeftFalse(roomId);
    }

    // ── Bulk Queries by User (Active Memberships Only) ───────────────

    @Override
    public List<Member> findAllActiveByUserId(UUID userId) {
        return memberQueryJpaRepository.findAllByUserIdAndIsLeftFalse(userId).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Member> findActiveAdminMembershipsByUserId(UUID userId) {
        return memberQueryJpaRepository.findAllByUserIdAndStatusAndIsLeftFalse(
                userId, MemberEntity.MemberStatus.ADMIN).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countActiveByUserId(UUID userId) {
        return memberQueryJpaRepository.countByUserIdAndIsLeftFalse(userId);
    }

    // ── Batch Lookup Queries (Active Members Only) ───────────────────

    @Override
    public List<Member> findActiveByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        return memberQueryJpaRepository.findAllByRoomIdAndUserIdInAndIsLeftFalse(roomId, userIds).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Member> findActiveByIds(Collection<UUID> memberIds) {
        return memberQueryJpaRepository.findAllByIdInAndIsLeftFalse(memberIds).stream()
            .map(MemberMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Projection Queries (Lightweight Read Models) ─────────────────
    // Handled in plain Java: fetch entities, then map to MemberSummary

    @Override
    public List<MemberSummary> findActiveSummariesByRoomId(UUID roomId) {
        // Fetch active entities using pure ORM derivation
        List<MemberEntity> entities = memberQueryJpaRepository.findAllByRoomIdAndIsLeftFalse(roomId);
        // Project to MemberSummary in Java (no JPQL constructor needed)
        return entities.stream()
            .map(e -> new MemberSummary(
                e.getId(),
                e.getUserId(),
                e.getRoomId(),
                e.getStatus().toDomain(),
                e.getJoinedAt()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberSummary> findActiveSummariesByUserId(UUID userId) {
        // Fetch active entities using pure ORM derivation
        List<MemberEntity> entities = memberQueryJpaRepository.findAllByUserIdAndIsLeftFalse(userId);
        // Project to MemberSummary in Java (no JPQL constructor needed)
        return entities.stream()
            .map(e -> new MemberSummary(
                e.getId(),
                e.getUserId(),
                e.getRoomId(),
                e.getStatus().toDomain(),
                e.getJoinedAt()
            ))
            .collect(Collectors.toList());
    }
}