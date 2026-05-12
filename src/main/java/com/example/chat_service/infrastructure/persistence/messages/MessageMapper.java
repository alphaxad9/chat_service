package com.example.chat_service.infrastructure.persistence.messages;

import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.MessageAggregate;

/**
 * Handles mapping between domain aggregates, JPA entities, and domain value objects.
 *
 * <ul>
 *   <li>{@link #aggregateToEntity(MessageAggregate)}: MessageAggregate → MessageEntity (for persistence)</li>
 *   <li>{@link #entityToAggregate(MessageEntity)}: MessageEntity → MessageAggregate (for command loading)</li>
 *   <li>{@link #entityToDomain(MessageEntity)}: MessageEntity → Message (for query responses)</li>
 * </ul>
 *
 * <p>Keeps domain logic pure by isolating persistence concerns in infrastructure layer.
 * All methods are static utilities — no state, no dependencies.</p>
 */
public final class MessageMapper {

    // Prevent instantiation
    private MessageMapper() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Convert a write-side MessageAggregate into a JPA-persistable entity.
     * Used by command repositories to save aggregate state after business operations.
     *
     * @param aggregate the domain aggregate containing current state
     * @return JPA entity ready for persistence
     */
    public static MessageEntity aggregateToEntity(MessageAggregate aggregate) {
        Message message = aggregate.message();
        
        return new MessageEntity(
            message.id(),
            message.roomId(),
            message.senderId(),
            message.content(),
            message.imageUrl(),
            message.parentId(),
            MessageEntity.Status.fromDomain(message.status()),
            message.seenAt(),
            message.createdAt(),
            message.updatedAt(),
            message.isDeleted()
        );
        // Note: createdAt/updatedAt/isDeleted are managed by:
        // - @CreationTimestamp / @UpdateTimestamp annotations
        // - Repository methods for soft-delete (markDeleted/restore)
        // If loading existing entity, use entityToAggregate then save (JPA merge pattern)
    }

    /**
     * Reconstruct a write-side MessageAggregate from a JPA entity.
     * Used by command repository's load() method to hydrate aggregate for mutation.
     *
     * @param entity the persisted JPA entity
     * @return MessageAggregate ready for business operations
     */
    public static MessageAggregate entityToAggregate(MessageEntity entity) {
        Message domain = entityToDomain(entity);
        return MessageAggregate.fromEntity(domain);
    }

    /**
     * Convert a JPA entity into the immutable domain value object.
     * Used exclusively by query services to return clean, serializable data.
     *
     * @param entity the persisted JPA entity
     * @return immutable Message domain object
     */
    public static Message entityToDomain(MessageEntity entity) {
        // Use Message constructor directly since we're mapping from trusted persistence layer
        // Validation already enforced at domain creation time
        return new Message(
            entity.getId(),
            entity.getRoomId(),
            entity.getSenderId(),
            entity.getContent(),
            entity.getImageUrl(),
            entity.getParentId(),
            entity.getStatus().toDomain(),
            entity.getSeenAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.isDeleted()
        );
    }

    /**
     * Convenience: Convert domain Message directly to entity (bypassing aggregate).
     * Useful for read-model sync or event-sourcing projections.
     *
     * @param domain the immutable Message value object
     * @return JPA entity ready for persistence
     */
    public static MessageEntity domainToEntity(Message domain) {
        return new MessageEntity(
            domain.id(),
            domain.roomId(),
            domain.senderId(),
            domain.content(),
            domain.imageUrl(),
            domain.parentId(),
            MessageEntity.Status.fromDomain(domain.status()),
            domain.seenAt(),
            domain.createdAt(),
            domain.updatedAt(),
            domain.isDeleted()
        );
    }
}