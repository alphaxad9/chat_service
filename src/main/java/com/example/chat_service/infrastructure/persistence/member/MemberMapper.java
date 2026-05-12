// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/MemberMapper.java
package com.example.chat_service.infrastructure.persistence.member;

import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.members.MemberAggregate;

/**
 * Handles mapping between domain aggregates, JPA entities, and domain value objects.
 *
 * <ul>
 *   <li>{@link #aggregateToEntity(MemberAggregate)}: MemberAggregate → MemberEntity (for persistence)</li>
 *   <li>{@link #entityToAggregate(MemberEntity)}: MemberEntity → MemberAggregate (for command loading)</li>
 *   <li>{@link #entityToDomain(MemberEntity)}: MemberEntity → Member (for query responses)</li>
 * </ul>
 *
 * <p>Keeps domain logic pure by isolating persistence concerns in infrastructure layer.
 * All methods are static utilities — no state, no dependencies.</p>
 */
public final class MemberMapper {

    // Prevent instantiation
    private MemberMapper() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Convert a write-side MemberAggregate into a JPA-persistable entity.
     * Used by command repositories to save aggregate state after business operations.
     *
     * @param aggregate the domain aggregate containing current state
     * @return JPA entity ready for persistence
     */
    public static MemberEntity aggregateToEntity(MemberAggregate aggregate) {
        Member member = aggregate.member();
        
        return new MemberEntity(
            member.id(),
            member.userId(),
            member.roomId(),
            MemberEntity.MemberStatus.fromDomain(member.status()),
            member.unreadMessages(),
            member.joinedAt(),
            member.updatedAt(),
            member.isLeft()
        );
        // Note: joinedAt/updatedAt/isLeft are managed by:
        // - @CreationTimestamp / @UpdateTimestamp annotations
        // - Repository methods for soft-delete (markLeft/restore)
        // If loading existing entity, use entityToAggregate then save (JPA merge pattern)
    }

    /**
     * Reconstruct a write-side MemberAggregate from a JPA entity.
     * Used by command repository's load() method to hydrate aggregate for mutation.
     *
     * @param entity the persisted JPA entity
     * @return MemberAggregate ready for business operations
     */
    public static MemberAggregate entityToAggregate(MemberEntity entity) {
        Member domain = entityToDomain(entity);
        return MemberAggregate.fromEntity(domain);
    }

    /**
     * Convert a JPA entity into the immutable domain value object.
     * Used exclusively by query services to return clean, serializable data.
     *
     * @param entity the persisted JPA entity
     * @return immutable Member domain object
     */
    public static Member entityToDomain(MemberEntity entity) {
        // Use Member constructor directly since we're mapping from trusted persistence layer
        // Validation already enforced at domain creation time
        return new Member(
            entity.getId(),
            entity.getUserId(),
            entity.getRoomId(),
            entity.getStatus().toDomain(),
            entity.getUnreadMessages(),
            entity.getJoinedAt(),
            entity.getUpdatedAt(),
            entity.isLeft()
        );
    }

    /**
     * Convenience: Convert domain Member directly to entity (bypassing aggregate).
     * Useful for read-model sync or event-sourcing projections.
     *
     * @param domain the immutable Member value object
     * @return JPA entity ready for persistence
     */
    public static MemberEntity domainToEntity(Member domain) {
        return new MemberEntity(
            domain.id(),
            domain.userId(),
            domain.roomId(),
            MemberEntity.MemberStatus.fromDomain(domain.status()),
            domain.unreadMessages(),
            domain.joinedAt(),
            domain.updatedAt(),
            domain.isLeft()
        );
    }
}