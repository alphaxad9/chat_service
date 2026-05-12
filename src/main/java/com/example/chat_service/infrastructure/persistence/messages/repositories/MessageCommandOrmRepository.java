package com.example.chat_service.infrastructure.persistence.messages.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.MessageAggregate;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError;
import com.example.chat_service.domain.messages.exceptions.MessageAlreadyExistsError;
import com.example.chat_service.domain.messages.exceptions.MessageNotFoundError;
import com.example.chat_service.domain.messages.repositories.MessageCommandRepository;
import com.example.chat_service.infrastructure.persistence.messages.MessageEntity;
import com.example.chat_service.infrastructure.persistence.messages.MessageMapper;
import com.example.chat_service.infrastructure.persistence.messages.jpa.MessageCommandJpaRepository;

/**
 * JPA/Hibernate implementation of {@link MessageCommandRepository}.
 *
 * <p>Handles write-side operations for Message aggregates using Spring Data JPA.
 * Leverages {@link MessageCommandJpaRepository} for persistence and {@link MessageMapper}
 * for domain ↔ entity conversion.</p>
 *
 * <p><strong>Soft-delete handling:</strong> Filtering by {@code isDeleted} is handled
 * explicitly via method names. Methods without {@code AndIsDeletedFalse} suffix
 * return all messages (active + deleted); methods with the suffix return active-only.</p>
 *
 * <p><strong>Transaction management:</strong> All methods run within a transaction
 * via class-level {@code @Transactional}. Rollback occurs automatically on
 * unchecked exceptions, preserving aggregate consistency.</p>
 *
 * <p><strong>Exception mapping:</strong> Database constraint violations are
 * translated to domain-specific exceptions for clean error handling in application layer.</p>
 *
 * <p><strong>Immutable domain pattern:</strong> This repository respects the immutable
 * nature of the Message domain model. State mutations are performed via domain methods
 * (e.g., {@code withStatus()}, {@code toggleDeletion()}) which return new instances,
 * then persisted via the mapper. Direct entity setters are not used outside the entity's package.</p>
 */
@Repository
@Transactional
public class MessageCommandOrmRepository implements MessageCommandRepository {

    private final MessageCommandJpaRepository messageJpaRepository;

    public MessageCommandOrmRepository(MessageCommandJpaRepository messageJpaRepository) {
        this.messageJpaRepository = messageJpaRepository;
    }

    @Override
    public void save(MessageAggregate aggregate) {
        MessageEntity entity = MessageMapper.aggregateToEntity(aggregate);
        
        try {
            // JPA merge pattern: save handles both insert and update
            messageJpaRepository.save(entity);
            
        } catch (DataIntegrityViolationException e) {
            String errorMsg = e.getRootCause() != null 
                ? e.getRootCause().getMessage().toLowerCase() 
                : e.getMessage().toLowerCase();
            
            // Check for duplicate message ID (rare with UUIDs, but possible in tests)
            if (errorMsg.contains("messages_pkey") || 
                (errorMsg.contains("duplicate") && errorMsg.contains("key") && errorMsg.contains("messages"))) {
                throw new MessageAlreadyExistsError(
                    aggregate.message().id(),
                    aggregate.message().roomId(),
                    aggregate.message().senderId(),
                    "Message with ID " + aggregate.message().id() + " already exists"
                );
            }
            
            // Check for NOT NULL constraints on required fields
            if (errorMsg.contains("room_id") && errorMsg.contains("null")) {
                throw new InvalidMessageEntityError(
                    aggregate.message().id(),
                    null,
                    aggregate.message().senderId(),
                    "Database constraint violated: room_id cannot be null"
                );
            }
            if (errorMsg.contains("sender_id") && errorMsg.contains("null")) {
                throw new InvalidMessageEntityError(
                    aggregate.message().id(),
                    aggregate.message().roomId(),
                    null,
                    "Database constraint violated: sender_id cannot be null"
                );
            }
            if (errorMsg.contains("content") && errorMsg.contains("null")) {
                throw new InvalidMessageEntityError(
                    aggregate.message().id(),
                    aggregate.message().roomId(),
                    aggregate.message().senderId(),
                    "Database constraint violated: content cannot be null"
                );
            }
            if (errorMsg.contains("status") && errorMsg.contains("null")) {
                throw new InvalidMessageEntityError(
                    aggregate.message().id(),
                    aggregate.message().roomId(),
                    aggregate.message().senderId(),
                    "Database constraint violated: status cannot be null"
                );
            }
            
            // Re-throw as generic integrity error if no specific mapping
            throw new DataIntegrityViolationException(
                "Failed to persist message " + aggregate.message().id() + ": " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void bulkSave(Collection<MessageAggregate> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return;
        }
        
        List<MessageEntity> entities = aggregates.stream()
            .map(MessageMapper::aggregateToEntity)
            .collect(Collectors.toList());
        
        try {
            messageJpaRepository.saveAll(entities);
        } catch (DataIntegrityViolationException e) {
            // For bulk operations, re-throw with context about which aggregate failed
            throw new DataIntegrityViolationException(
                "Failed to persist batch of " + aggregates.size() + " messages: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public MessageAggregate load(UUID messageId) {
        try {
            // Load by ID and filter to active-only for command operations
            MessageEntity entity = messageJpaRepository.findById(messageId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new MessageNotFoundError(
                    messageId,
                    "Message not found or already deleted"
                ));
            
            return MessageMapper.entityToAggregate(entity);
            
        } catch (EmptyResultDataAccessException e) {
            MessageNotFoundError notFound = new MessageNotFoundError(
                messageId,
                "Message not found"
            );
            notFound.initCause(e);
            throw notFound;
        }
    }

    @Override
    public Optional<MessageAggregate> loadOptional(UUID messageId) {
        // Base findById returns all messages; caller decides how to handle deleted
        return messageJpaRepository.findById(messageId)
            .map(MessageMapper::entityToAggregate);
    }

    @Override
    public Optional<MessageAggregate> loadByRoomAndId(UUID roomId, UUID messageId) {
        // Load by ID and validate room membership
        return messageJpaRepository.findById(messageId)
            .filter(e -> !e.isDeleted() && e.getRoomId().equals(roomId))
            .map(MessageMapper::entityToAggregate);
    }

    @Override
    public boolean exists(UUID messageId) {
        // Base existsById checks all messages (active + deleted)
        return messageJpaRepository.existsById(messageId);
    }

    @Override
    public boolean existsInRoom(UUID roomId, UUID messageId) {
        // Check message-room relationship regardless of deletion status
        return messageJpaRepository.existsByIdAndRoomId(messageId, roomId);
    }

    @Override
    public boolean existsByRoomAndSender(UUID roomId, UUID senderId) {
        // Check if sender has any messages in room (all states)
        return messageJpaRepository.existsByRoomIdAndSenderId(roomId, senderId);
    }

    // ── Query by Room ──────────────────────────────────────────────────

    @Override
    public List<MessageAggregate> bulkLoadByRoomId(UUID roomId) {
        // Base method returns ALL messages in room (active + deleted)
        List<MessageEntity> entities = messageJpaRepository.findAllByRoomIdOrderByCreatedAtAsc(roomId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomId(UUID roomId) {
        // Active-only variant with ascending order for chat history
        List<MessageEntity> entities = messageJpaRepository.findAllByRoomIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomIdLimited(UUID roomId, int limit) {
        // Active-only, limited, most recent first for previews
        List<MessageEntity> entities = messageJpaRepository.findTopByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(roomId, limit);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomIdAfter(UUID roomId, UUID afterId, int limit) {
        // Cursor-based pagination: load messages newer than cursor
        if (afterId == null) {
            // If no cursor, fall back to limited recent messages
            return bulkLoadActiveByRoomIdLimited(roomId, limit);
        }
        
        // Load the cursor message to get its createdAt timestamp
        MessageEntity cursorEntity = messageJpaRepository.findById(afterId)
            .orElseThrow(() -> new MessageNotFoundError(afterId, "Cursor message not found"));
        
        List<MessageEntity> entities = messageJpaRepository
            .findByRoomIdAndIsDeletedFalseAndCreatedAtGreaterThanOrderByCreatedAtAsc(
                roomId, cursorEntity.getCreatedAt(), limit);
        
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    // ── Query by Sender ────────────────────────────────────────────────

    @Override
    public List<MessageAggregate> bulkLoadBySenderId(UUID senderId) {
        // Base method returns ALL messages by sender (active + deleted)
        List<MessageEntity> entities = messageJpaRepository.findAllBySenderIdOrderByCreatedAtDesc(senderId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveBySenderId(UUID senderId) {
        // Active-only variant ordered by creation descending
        List<MessageEntity> entities = messageJpaRepository.findAllBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(senderId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveBySenderIdAndRooms(UUID senderId, Collection<UUID> roomIds) {
        // Active-only, filtered by specific rooms
        List<MessageEntity> entities = messageJpaRepository
            .findAllBySenderIdAndRoomIdInAndIsDeletedFalseOrderByCreatedAtDesc(senderId, roomIds);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    // ── Query by Status ────────────────────────────────────────────────

    @Override
    public List<MessageAggregate> bulkLoadByStatus(Message.Status status) {
        // Base method returns ALL messages with status (active + deleted)
        MessageEntity.Status persistenceStatus = MessageEntity.Status.fromDomain(status);
        List<MessageEntity> entities = messageJpaRepository.findAllByStatusOrderByCreatedAtDesc(persistenceStatus);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByStatus(Message.Status status) {
        // Active-only variant ordered by creation descending
        MessageEntity.Status persistenceStatus = MessageEntity.Status.fromDomain(status);
        List<MessageEntity> entities = messageJpaRepository
            .findAllByStatusAndIsDeletedFalseOrderByCreatedAtDesc(persistenceStatus);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveSentOlderThan(LocalDateTime olderThan) {
        // Active SENT messages older than threshold for retry jobs
        List<MessageEntity> entities = messageJpaRepository
            .findAllByStatusAndIsDeletedFalseAndCreatedAtBeforeOrderByCreatedAtAsc(
                MessageEntity.Status.SENT, olderThan);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    // ── Query by Reply Chain ───────────────────────────────────────────

    @Override
    public List<MessageAggregate> bulkLoadRepliesTo(UUID parentId) {
        // Base method returns ALL replies (active + deleted)
        List<MessageEntity> entities = messageJpaRepository.findAllByParentIdOrderByCreatedAtAsc(parentId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveRepliesTo(UUID parentId) {
        // Active-only replies ordered by creation ascending
        List<MessageEntity> entities = messageJpaRepository
            .findAllByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasReplies(UUID messageId) {
        // Active-only existence check for UI badges
        return messageJpaRepository.existsByParentIdAndIsDeletedFalse(messageId);
    }

    // ── Bulk Load by IDs ───────────────────────────────────────────────

    @Override
    public List<MessageAggregate> bulkLoadByIds(Collection<UUID> messageIds) {
        // Base method returns ALL matching messages (active + deleted)
        List<MessageEntity> entities = messageJpaRepository.findAllByIdIn(messageIds);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByIds(Collection<UUID> messageIds) {
        // Active-only variant
        List<MessageEntity> entities = messageJpaRepository.findAllByIdInAndIsDeletedFalse(messageIds);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    // ── Bulk Operations by Room + Sender ───────────────────────────────

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomAndSender(UUID roomId, UUID senderId) {
        // Active-only, filtered by room and sender
        List<MessageEntity> entities = messageJpaRepository
            .findAllByRoomIdAndSenderIdAndIsDeletedFalseOrderByCreatedAtAsc(roomId, senderId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveWithImagesByRoomId(UUID roomId) {
        // Active-only messages with image attachments
        List<MessageEntity> entities = messageJpaRepository
            .findAllByRoomIdAndImageUrlIsNotNullAndIsDeletedFalseOrderByCreatedAtDesc(roomId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveRepliesByRoomId(UUID roomId) {
        // Active-only reply messages in a room
        List<MessageEntity> entities = messageJpaRepository
            .findAllByRoomIdAndParentIdIsNotNullAndIsDeletedFalseOrderByCreatedAtAsc(roomId);
        return entities.stream()
            .map(MessageMapper::entityToAggregate)
            .collect(Collectors.toList());
    }

    // ── Cleanup / Maintenance ──────────────────────────────────────────

    @Override
    public int bulkDeleteOldMessagesInRoom(UUID roomId, LocalDateTime olderThan, UUID actorId) {
        // Load eligible messages, apply domain mutation via aggregate, then persist
        List<MessageEntity> entities = messageJpaRepository
            .findAllByRoomIdAndIsDeletedFalseAndCreatedAtBefore(roomId, olderThan);
        
        int count = 0;
        for (MessageEntity entity : entities) {
            // Convert to aggregate for domain-level mutation
            MessageAggregate aggregate = MessageMapper.entityToAggregate(entity);
            
            // Authorization check placeholder: only sender or room admin can delete
            // if (!authorizationService.canDeleteMessage(actorId, aggregate.id())) {
            //     continue;
            // }
            
            // Apply soft-delete via domain method (returns new immutable instance)
            aggregate.delete(actorId);
            
            // Persist the mutated aggregate
            save(aggregate);
            count++;
        }
        return count;
    }

    @Override
    public int bulkUpdateStatus(Collection<UUID> messageIds, Message.Status newStatus, UUID actorId) {
        // Load messages, apply domain-level status transitions, then persist
        List<MessageEntity> entities = messageJpaRepository.findAllByIdIn(messageIds);
        
        int count = 0;
        for (MessageEntity entity : entities) {
            // Skip deleted messages
            if (entity.isDeleted()) {
                continue;
            }
            
            // Convert to aggregate for domain-level mutation
            MessageAggregate aggregate = MessageMapper.entityToAggregate(entity);
            
            // Authorization: only receiver can mark as RECEIVED/SEEN
            // if ((newStatus == Message.Status.RECEIVED || newStatus == Message.Status.SEEN) &&
            //     !authorizationService.isMessageReceiver(actorId, aggregate.id())) {
            //     continue;
            // }
            
            try {
                // Apply status transition via domain method (validates transitions)
                if (newStatus == Message.Status.RECEIVED) {
                    aggregate.markAsReceived(actorId);
                } else if (newStatus == Message.Status.SEEN) {
                    aggregate.markAsSeen(actorId);
                }
                // SENT status doesn't need explicit transition (already default)
                
                // Persist the mutated aggregate
                save(aggregate);
                count++;
                
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Skip messages where transition is invalid (per domain rules)
                // Log if needed: logger.debug("Skipping invalid status transition for message {}: {}", entity.getId(), e.getMessage());
                continue;
            }
        }
        return count;
    }
}