// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/posts/jpa/PostCommandJpaRepository.java

package com.example.chat_service.infrastructure.persistence.posts.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chat_service.infrastructure.persistence.posts.PostEntity;

/**
 * Spring Data JPA repository for command-side (write) operations on {@link PostEntity}.
 *
 * <p><strong>Purpose:</strong> Supports {@code PostCommandOrmRepository} by providing
 * type-safe, derived-query methods for aggregate persistence and retrieval.
 * No custom JPQL/SQL — all methods use Spring Data JPA's method-name derivation.</p>
 *
 * <p><strong>Soft-delete behavior:</strong> The {@code @SQLRestriction("is_deleted = false")}
 * annotation on {@link PostEntity} automatically filters out deleted records for all
 * methods in this interface. This ensures command operations respect domain invariants
 * without manual filter logic.</p>
 *
 * <p><strong>Not for read-side queries:</strong> This repository is optimized for
 * loading full aggregates for mutation. For read-only views, lists, or projections,
 * use a separate query-side repository when implemented.</p>
 */
@Repository
public interface PostCommandJpaRepository extends JpaRepository<PostEntity, UUID> {

    // ── Inherited Methods from JpaRepository<PostEntity, UUID> ─────────
    // These are sufficient for most command operations:
    //
    // • Optional<PostEntity> findById(UUID id)
    //   → Loads entity by ID (excludes deleted via @SQLRestriction)
    //
    // • <S extends PostEntity> S save(S entity)
    //   → INSERT if new ID, UPDATE if ID exists (JPA merge pattern)
    //
    // • boolean existsById(UUID id)
    //   → Fast existence check (excludes deleted via @SQLRestriction)
    //
    // • void deleteById(UUID id)
    //   → Not used directly; soft-delete via entity.markDeleted() + save()

    // ── Command-Specific Derived Query Methods ─────────────────────────

    /**
     * Find the most recent non-deleted post by author ID.
     *
     * <p>Uses Spring Data JPA method-name derivation:
     * {@code findFirstBy[Property]OrderBy[Property][Direction]}.</p>
     *
     * <p>Automatically excludes deleted posts via {@code @SQLRestriction}
     * on {@link PostEntity}. Used by {@code loadByAuthor()} in command repository.</p>
     *
     * @param authorId the UUID of the post author
     * @return {@link Optional} containing the most recent active post, or empty
     */
    Optional<PostEntity> findFirstByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    /**
     * Check if any non-deleted post exists for the given author.
     *
     * <p>Efficient existence check using derived query method.
     * Respects soft-delete filter via {@code @SQLRestriction}.
     * Used by {@code existsByAuthor()} in command repository.</p>
     *
     * @param authorId the UUID of the post author
     * @return {@code true} if author has at least one active post
     */
    boolean existsByAuthorId(UUID authorId);
}