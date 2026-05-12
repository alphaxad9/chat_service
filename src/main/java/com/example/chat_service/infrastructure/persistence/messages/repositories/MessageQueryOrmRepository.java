package com.example.chat_service.infrastructure.persistence.messages.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.repositories.MessageQueryRepository;
import com.example.chat_service.infrastructure.persistence.messages.MessageEntity;
import com.example.chat_service.infrastructure.persistence.messages.MessageMapper;
import com.example.chat_service.infrastructure.persistence.messages.jpa.MessageQueryJpaRepository;

/**
 * JPA/Hibernate implementation of {@link MessageQueryRepository}.
 *
 * <p>Handles read-side operations for Message value objects using Spring Data JPA.
 * Leverages {@link MessageQueryJpaRepository} for persistence queries and {@link MessageMapper}
 * for entity → domain conversion.</p>
 *
 * <p><strong>CQRS read-side:</strong> Returns immutable {@link Message} domain objects
 * (not aggregates) optimized for query responses. All methods automatically filter
 * to active messages only ({@code isDeleted = false}).</p>
 *
 * <p><strong>Projection handling:</strong> {@code MessageSummary} projections are created
 * in plain Java code by mapping fetched {@code MessageEntity} instances. This avoids
 * JPQL constructor expressions and keeps all queries as pure ORM method derivations.</p>
 *
 * <p><strong>Transaction management:</strong> Methods are {@code @Transactional(readOnly = true)}
 * to optimize database access patterns and signal intent to the persistence layer.</p>
 */
@Repository
@Transactional(readOnly = true)
public class MessageQueryOrmRepository implements MessageQueryRepository {

    private final MessageQueryJpaRepository messageQueryJpaRepository;

    public MessageQueryOrmRepository(MessageQueryJpaRepository messageQueryJpaRepository) {
        this.messageQueryJpaRepository = messageQueryJpaRepository;
    }

    // ── Single Entity Queries (Active Messages Only) ──────────────────

    @Override
    public Optional<Message> findById(UUID messageId) {
        return messageQueryJpaRepository.findByIdAndIsDeletedFalse(messageId)
            .map(MessageMapper::entityToDomain);
    }

    @Override
    public Optional<Message> findByIdIncludingDeleted(UUID messageId) {
        // Base findById from JpaRepository returns all messages; caller decides how to handle
        return messageQueryJpaRepository.findById(messageId)
            .map(MessageMapper::entityToDomain);
    }

    @Override
    public boolean isActiveMessage(UUID messageId) {
        return messageQueryJpaRepository.existsByIdAndIsDeletedFalse(messageId);
    }

    // ── Latest Message Queries (Active Messages Only) ─────────────────

    @Override
    public Optional<Message> findLatestActiveByRoomId(UUID roomId) {
        return messageQueryJpaRepository.findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(roomId)
            .map(MessageMapper::entityToDomain);
    }

    @Override
    public Map<UUID, Message> findLatestActiveByRoomIds(Collection<UUID> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Fetch latest active message per room using native DISTINCT ON query
        List<MessageEntity> entities = messageQueryJpaRepository
            .findLatestActiveByRoomIds(roomIds);
        
        // Group by roomId and keep only the latest message per room
        return entities.stream()
            .collect(Collectors.toMap(
                MessageEntity::getRoomId,
                MessageMapper::entityToDomain,
                // If duplicate roomIds exist (shouldn't with DISTINCT ON), keep first
                (existing, replacement) -> existing
            ));
    }

    @Override
    public Map<UUID, MessageSummary> findLatestActiveSummariesByRoomIds(Collection<UUID> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Fetch latest active message per room using native DISTINCT ON query
        List<MessageEntity> entities = messageQueryJpaRepository
            .findLatestActiveByRoomIds(roomIds);
        
        // Project to MessageSummary and group by roomId
        return entities.stream()
            .collect(Collectors.toMap(
                MessageEntity::getRoomId,
                this::entityToSummary,
                (existing, replacement) -> existing
            ));
    }

    // ── Bulk Queries by Room (Active Messages Only) ───────────────────

    @Override
    public List<Message> findAllActiveByRoomId(UUID roomId) {
        return messageQueryJpaRepository.findAllByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveByRoomIdLimited(UUID roomId, int limit) {
        return messageQueryJpaRepository.findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(roomId, limit).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveByRoomIdAfter(UUID roomId, UUID afterId, int limit) {
        if (afterId == null) {
            return findActiveByRoomIdLimited(roomId, limit);
        }
        
        // Load cursor message to get its createdAt
        Optional<MessageEntity> cursorOpt = messageQueryJpaRepository.findById(afterId);
        if (cursorOpt.isEmpty()) {
            return Collections.emptyList();
        }
        
        LocalDateTime cursorTimestamp = cursorOpt.get().getCreatedAt();
        return messageQueryJpaRepository
            .findByRoomIdAndIsDeletedFalseAndCreatedAtGreaterThanOrderByCreatedAtAsc(
                roomId, cursorTimestamp, limit).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveByRoomIdBefore(UUID roomId, UUID beforeId, int limit) {
        if (beforeId == null) {
            return findActiveByRoomIdLimited(roomId, limit);
        }
        
        // Load cursor message to get its createdAt
        Optional<MessageEntity> cursorOpt = messageQueryJpaRepository.findById(beforeId);
        if (cursorOpt.isEmpty()) {
            return Collections.emptyList();
        }
        
        LocalDateTime cursorTimestamp = cursorOpt.get().getCreatedAt();
        return messageQueryJpaRepository
            .findByRoomIdAndIsDeletedFalseAndCreatedAtLessThanOrderByCreatedAtDesc(
                roomId, cursorTimestamp, limit).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countActiveByRoomId(UUID roomId) {
        return messageQueryJpaRepository.countByRoomIdAndIsDeletedFalse(roomId);
    }

    // ── Bulk Queries by Sender (Active Messages Only) ─────────────────

    @Override
    public List<Message> findAllActiveBySenderId(UUID senderId) {
        return messageQueryJpaRepository.findAllBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(senderId).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveBySenderIdAndRooms(UUID senderId, Collection<UUID> roomIds) {
        return messageQueryJpaRepository
            .findAllBySenderIdAndRoomIdInAndIsDeletedFalseOrderByCreatedAtDesc(senderId, roomIds).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countActiveBySenderId(UUID senderId) {
        return messageQueryJpaRepository.countBySenderIdAndIsDeletedFalse(senderId);
    }

    // ── Bulk Queries by Status (Active Messages Only) ─────────────────

    @Override
    public List<Message> findActiveByStatus(Message.Status status) {
        MessageEntity.Status persistenceStatus = MessageEntity.Status.fromDomain(status);
        return messageQueryJpaRepository
            .findAllByStatusAndIsDeletedFalseOrderByCreatedAtDesc(persistenceStatus).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveByRoomAndStatus(UUID roomId, Message.Status status) {
        MessageEntity.Status persistenceStatus = MessageEntity.Status.fromDomain(status);
        return messageQueryJpaRepository
            .findAllByRoomIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(roomId, persistenceStatus).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveSentOlderThan(LocalDateTime olderThan) {
        return messageQueryJpaRepository
            .findAllByStatusAndIsDeletedFalseAndCreatedAtBeforeOrderByCreatedAtAsc(
                MessageEntity.Status.SENT, olderThan).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Reply Chain Queries (Active Messages Only) ────────────────────

    @Override
    public List<Message> findActiveRepliesTo(UUID parentId) {
        return messageQueryJpaRepository.findAllByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveRepliesToLimited(UUID parentId, int limit) {
        return messageQueryJpaRepository.findTopByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId, limit).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasActiveReplies(UUID messageId) {
        return messageQueryJpaRepository.existsByParentIdAndIsDeletedFalse(messageId);
    }

    @Override
    public long countActiveRepliesTo(UUID parentId) {
        return messageQueryJpaRepository.countByParentIdAndIsDeletedFalse(parentId);
    }

    // ── Bulk Lookup Queries (Active Messages Only) ───────────────────

    @Override
    public List<Message> findActiveByIds(Collection<UUID> messageIds) {
        return messageQueryJpaRepository.findAllByIdInAndIsDeletedFalse(messageIds).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveByRoomIds(Collection<UUID> roomIds) {
        return messageQueryJpaRepository.findAllByRoomIdInAndIsDeletedFalseOrderByCreatedAtDesc(roomIds).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Activity/Time-Based Queries (Active Messages Only) ────────────

    @Override
    public List<Message> findActiveCreatedSince(LocalDateTime sinceTimestamp, int limit) {
        return messageQueryJpaRepository
            .findByIsDeletedFalseAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(sinceTimestamp, limit).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveUpdatedSince(LocalDateTime sinceTimestamp, int limit) {
        return messageQueryJpaRepository
            .findByIsDeletedFalseAndUpdatedAtGreaterThanEqualOrderByUpdatedAtDesc(sinceTimestamp, limit).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Content/Image Queries (Active Messages Only) ──────────────────

    @Override
    public List<Message> findActiveWithImagesByRoomId(UUID roomId) {
        return messageQueryJpaRepository
            .findAllByRoomIdAndImageUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(roomId).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Message> findActiveWithImagesBySenderId(UUID senderId) {
        return messageQueryJpaRepository
            .findAllBySenderIdAndImageUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(senderId).stream()
            .map(MessageMapper::entityToDomain)
            .collect(Collectors.toList());
    }

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    @Override
    public List<MessageSummary> findActiveSummariesByRoomId(UUID roomId) {
        List<MessageEntity> entities = messageQueryJpaRepository
            .findAllByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId);
        return entities.stream()
            .map(this::entityToSummary)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageSummary> findActiveSummariesByIds(Collection<UUID> messageIds) {
        List<MessageEntity> entities = messageQueryJpaRepository
            .findAllByIdInAndIsDeletedFalse(messageIds);
        return entities.stream()
            .map(this::entityToSummary)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageSummary> findActiveSummariesByRoomIdAfter(UUID roomId, UUID afterId, int limit) {
        if (afterId == null) {
            List<MessageEntity> entities = messageQueryJpaRepository
                .findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(roomId, limit);
            return entities.stream().map(this::entityToSummary).collect(Collectors.toList());
        }
        
        Optional<MessageEntity> cursorOpt = messageQueryJpaRepository.findById(afterId);
        if (cursorOpt.isEmpty()) {
            return Collections.emptyList();
        }
        
        LocalDateTime cursorTimestamp = cursorOpt.get().getCreatedAt();
        List<MessageEntity> entities = messageQueryJpaRepository
            .findByRoomIdAndIsDeletedFalseAndCreatedAtGreaterThanOrderByCreatedAtAsc(
                roomId, cursorTimestamp, limit);
        return entities.stream()
            .map(this::entityToSummary)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageSummary> findActiveReplySummariesTo(UUID parentId) {
        List<MessageEntity> entities = messageQueryJpaRepository
            .findAllByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId);
        return entities.stream()
            .map(this::entityToSummary)
            .collect(Collectors.toList());
    }

    // ── Helper: Entity → MessageSummary Projection ───────────────────

    /**
     * Convert a MessageEntity to a lightweight MessageSummary for UI rendering.
     * Extracts only fields needed for message list displays.
     */
    private MessageSummary entityToSummary(MessageEntity entity) {
        // Truncate content for preview (first 200 chars)
        String content = entity.getContent();
        String preview = (content != null && content.length() > 200) 
            ? content.substring(0, 200) + "…" 
            : content;
        
        Message.Status domainStatus = entity.getStatus().toDomain();
        
        return new MessageSummary(
            entity.getId(),
            entity.getRoomId(),
            entity.getSenderId(),
            preview,
            entity.getImageUrl() != null && !entity.getImageUrl().isBlank(),  // hasImage
            entity.getParentId() != null,                                       // isReply
            entity.getParentId(),                                               // parentId
            domainStatus,
            domainStatus == Message.Status.SEEN,                                // isSeen convenience
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getSeenAt()
        );
    }
}