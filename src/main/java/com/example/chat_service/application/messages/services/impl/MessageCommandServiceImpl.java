package com.example.chat_service.application.messages.services.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.messages.services.MessageCommandServiceInterface;
import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.MessageAggregate;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageContentError;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageEntityError;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageImageError;
import com.example.chat_service.domain.messages.exceptions.InvalidMessageParentError;
import com.example.chat_service.domain.messages.exceptions.MessageAlreadyExistsError;
import com.example.chat_service.domain.messages.exceptions.MessageDomainError;
import com.example.chat_service.domain.messages.exceptions.MessageNotFoundError;
import com.example.chat_service.domain.messages.exceptions.MessageOperationNotAllowedError;
import com.example.chat_service.domain.messages.exceptions.MessageStateTransitionError;
import com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedError;
import com.example.chat_service.domain.messages.exceptions.MessageUnauthorizedErrorWithNoId;
import com.example.chat_service.domain.messages.repositories.MessageCommandRepository;

/**
 * Application-layer implementation of {@link MessageCommandServiceInterface}.
 *
 * <p>Orchestrates message command (write) operations by coordinating domain aggregates
 * with infrastructure repositories. All methods run within a transaction boundary
 * to ensure consistency.</p>
 *
 * <p><strong>Command pattern:</strong> All command methods (except {@code createMessage}
 * variants) accept IDs as parameters, load the aggregate via repository,
 * apply domain logic, then persist the updated state. This ensures a consistent load-act-save flow.</p>
 *
 * <p><strong>No event publishing:</strong> This implementation focuses purely on
 * command orchestration. Event emission (outbox, Kafka, etc.) should be added
 * in a separate layer or via domain events when the infrastructure is ready.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Command methods accept IDs — service loads aggregate, applies business logic, persists result</li>
 *   <li>Only {@code createMessage} variants accept pre-built aggregates (for initial construction)</li>
 *   <li>Business rules and validation live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence) are delegated to {@link MessageCommandRepository}</li>
 *   <li>All public methods are {@code @Transactional} for atomicity</li>
 *   <li>Authorization checks (sender/receiver validation) are enforced by domain aggregates</li>
 * </ul></p>
 */
@Service
@Transactional
public class MessageCommandServiceImpl implements MessageCommandServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MessageCommandServiceImpl.class);

    private final MessageCommandRepository messageCommandRepository;

    public MessageCommandServiceImpl(MessageCommandRepository messageCommandRepository) {
        this.messageCommandRepository = messageCommandRepository;
    }

    // ── Core Lifecycle Commands ────────────────────────────────────────

    @Override
    public MessageAggregate createMessage(MessageAggregate aggregate) {
        try {
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully created message (message_id={}, room_id={}, sender_id={}, content_length={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().content().length()
            );
            return aggregate;

        } catch (MessageAlreadyExistsError e) {
            logger.warn(
                "Message creation failed: message already exists (message_id={}, room_id={}, sender_id={})",
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageEntityError e) {
            logger.warn(
                "Message creation failed: invalid entity data (reason={}, message_id={}, room_id={}, sender_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageContentError e) {
            logger.warn(
                "Message creation failed: invalid content (reason={}, message_id={}, length={}, max={})",
                e.getReason(),
                e.getMessageId(),
                e.getProvidedLength(),
                e.getMaxLength()
            );
            throw e;

        } catch (InvalidMessageImageError e) {
            logger.warn(
                "Message creation failed: invalid image URL (reason={}, message_id={}, url='{}')",
                e.getReason(),
                e.getMessageId(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (InvalidMessageParentError e) {
            logger.warn(
                "Message creation failed: invalid parent reference (reason={}, message_id={}, parent_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getParentId()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Message creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating message (message_id={})",
                aggregate != null && aggregate.message() != null ? aggregate.message().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public MessageAggregate createMessageWithImage(MessageAggregate aggregate) {
        try {
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully created message with image (message_id={}, room_id={}, sender_id={}, has_image={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().hasImage()
            );
            return aggregate;

        } catch (MessageAlreadyExistsError e) {
            logger.warn(
                "Message creation failed: message already exists (message_id={}, room_id={}, sender_id={})",
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageEntityError e) {
            logger.warn(
                "Message creation failed: invalid entity data (reason={}, message_id={}, room_id={}, sender_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageImageError e) {
            logger.warn(
                "Message creation failed: invalid image URL (reason={}, message_id={}, url='{}')",
                e.getReason(),
                e.getMessageId(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Message creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating message with image (message_id={})",
                aggregate != null && aggregate.message() != null ? aggregate.message().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public MessageAggregate createReplyMessage(MessageAggregate aggregate) {
        try {
            // Validate parent exists before saving
            if (aggregate.message().parentId() != null && 
                !messageCommandRepository.exists(aggregate.message().parentId())) {
                throw new MessageNotFoundError(aggregate.message().parentId(), "Parent message not found for reply");
            }
            
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully created reply message (message_id={}, room_id={}, sender_id={}, parent_id={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().parentId()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn(
                "Reply creation failed: parent message not found (message_id={}, parent_id={})",
                aggregate != null && aggregate.message() != null ? aggregate.message().id() : "unknown",
                aggregate != null && aggregate.message() != null ? aggregate.message().parentId() : "unknown"
            );
            throw e;

        } catch (MessageAlreadyExistsError e) {
            logger.warn(
                "Reply creation failed: message already exists (message_id={}, room_id={}, sender_id={})",
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageEntityError e) {
            logger.warn(
                "Reply creation failed: invalid entity data (reason={}, message_id={}, room_id={}, sender_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageParentError e) {
            logger.warn(
                "Reply creation failed: invalid parent reference (reason={}, message_id={}, parent_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getParentId()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Reply creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating reply message (message_id={})",
                aggregate != null && aggregate.message() != null ? aggregate.message().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public MessageAggregate createReplyMessageWithImage(MessageAggregate aggregate) {
        try {
            // Validate parent exists before saving
            if (aggregate.message().parentId() != null && 
                !messageCommandRepository.exists(aggregate.message().parentId())) {
                throw new MessageNotFoundError(aggregate.message().parentId(), "Parent message not found for reply");
            }
            
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully created reply message with image (message_id={}, room_id={}, sender_id={}, parent_id={}, has_image={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().parentId(),
                aggregate.message().hasImage()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn(
                "Reply creation failed: parent message not found (message_id={}, parent_id={})",
                aggregate != null && aggregate.message() != null ? aggregate.message().id() : "unknown",
                aggregate != null && aggregate.message() != null ? aggregate.message().parentId() : "unknown"
            );
            throw e;

        } catch (MessageAlreadyExistsError e) {
            logger.warn(
                "Reply creation failed: message already exists (message_id={}, room_id={}, sender_id={})",
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageEntityError e) {
            logger.warn(
                "Reply creation failed: invalid entity data (reason={}, message_id={}, room_id={}, sender_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getRoomId(),
                e.getSenderId()
            );
            throw e;

        } catch (InvalidMessageImageError e) {
            logger.warn(
                "Reply creation failed: invalid image URL (reason={}, message_id={}, url='{}')",
                e.getReason(),
                e.getMessageId(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (InvalidMessageParentError e) {
            logger.warn(
                "Reply creation failed: invalid parent reference (reason={}, message_id={}, parent_id={})",
                e.getReason(),
                e.getMessageId(),
                e.getParentId()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Reply creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating reply message with image (message_id={})",
                aggregate != null && aggregate.message() != null ? aggregate.message().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public MessageAggregate deleteMessage(UUID messageId, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.delete(actorId);
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully deleted message (message_id={}, room_id={}, sender_id={}, actor_id={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                actorId
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for delete operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Delete operation unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Delete operation unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageStateTransitionError e) {
            logger.warn(
                "Delete operation failed: invalid state transition (message_id={}, current={}, target={}, reason={})",
                e.getMessageId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Delete operation not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Delete operation domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during delete operation: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public MessageAggregate restoreMessage(UUID messageId, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.restore(actorId);
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully restored message (message_id={}, room_id={}, sender_id={}, actor_id={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                actorId
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for restore operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Restore operation unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Restore operation unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageStateTransitionError e) {
            logger.warn(
                "Restore operation failed: invalid state transition (message_id={}, current={}, target={}, reason={})",
                e.getMessageId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Restore operation not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Restore operation domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during restore operation: message_id={}", messageId, e);
            throw e;
        }
    }

    // ── Delivery Status Commands (receiver-initiated) ──────────────────

    @Override
    public MessageAggregate markAsReceived(UUID messageId, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.markAsReceived(actorId);
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully marked message as RECEIVED (message_id={}, room_id={}, receiver_id={}, status={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                actorId,
                aggregate.message().status()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for markAsReceived operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Mark as received unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Mark as received unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageStateTransitionError e) {
            logger.warn(
                "Mark as received failed: invalid state transition (message_id={}, current={}, target={}, reason={})",
                e.getMessageId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Mark as received not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Mark as received domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during markAsReceived operation: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public MessageAggregate markAsSeen(UUID messageId, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.markAsSeen(actorId);
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully marked message as SEEN (message_id={}, room_id={}, receiver_id={}, status={}, seen_at={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                actorId,
                aggregate.message().status(),
                aggregate.message().seenAt()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for markAsSeen operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Mark as seen unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Mark as seen unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageStateTransitionError e) {
            logger.warn(
                "Mark as seen failed: invalid state transition (message_id={}, current={}, target={}, reason={})",
                e.getMessageId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Mark as seen not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Mark as seen domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during markAsSeen operation: message_id={}", messageId, e);
            throw e;
        }
    }

    // ── Content & Media Update Commands (sender-initiated) ─────────────

    @Override
    public MessageAggregate updateContent(UUID messageId, String newContent, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.withContent(newContent, actorId);
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated message content (message_id={}, room_id={}, sender_id={}, content_length={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().content().length()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for updateContent operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Update content unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Update content unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Update content not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (InvalidMessageContentError e) {
            logger.warn(
                "Update content failed: invalid content (reason={}, message_id={}, length={}, max={})",
                e.getReason(),
                e.getMessageId(),
                e.getProvidedLength(),
                e.getMaxLength()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Update content domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateContent operation: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public MessageAggregate updateImage(UUID messageId, String newImageUrl, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.withImage(newImageUrl, actorId);
            messageCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated message image (message_id={}, room_id={}, sender_id={}, has_image={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().hasImage()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for updateImage operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Update image unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Update image unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Update image not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (InvalidMessageImageError e) {
            logger.warn(
                "Update image failed: invalid image URL (reason={}, message_id={}, url='{}')",
                e.getReason(),
                e.getMessageId(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Update image domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateImage operation: message_id={}", messageId, e);
            throw e;
        }
    }

    // ── Activity & Utility Commands ────────────────────────────────────

    @Override
    public MessageAggregate touch(UUID messageId, UUID actorId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.touch(actorId);
            messageCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched message (message_id={}, room_id={}, sender_id={}, updated_at={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().updatedAt()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for touch operation: message_id={}", messageId);
            throw e;

        } catch (MessageUnauthorizedError e) {
            logger.warn(
                "Touch unauthorized: message_id={}, actor_id={}, operation={}",
                e.getMessageId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Touch unauthorized (no message ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Touch not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Touch domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during touch operation: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public MessageAggregate touchInternal(UUID messageId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            aggregate.touchInternal();
            messageCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched message internally (message_id={}, room_id={}, sender_id={}, updated_at={})",
                aggregate.message().id(),
                aggregate.message().roomId(),
                aggregate.message().senderId(),
                aggregate.message().updatedAt()
            );
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message not found for touchInternal operation: message_id={}", messageId);
            throw e;

        } catch (MessageOperationNotAllowedError e) {
            logger.warn(
                "Touch internal not allowed: message_id={}, operation={}, reason={}",
                e.getMessageId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Touch internal domain error (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during touchInternal operation: message_id={}", messageId, e);
            throw e;
        }
    }

    // ── Query Support Methods (for command orchestration) ──────────────

    @Override
    public MessageAggregate loadAggregate(UUID messageId) {
        try {
            MessageAggregate aggregate = messageCommandRepository.load(messageId);
            logger.debug("Loaded message aggregate: message_id={}", messageId);
            return aggregate;

        } catch (MessageNotFoundError e) {
            logger.warn("Message aggregate not found: message_id={}", messageId);
            throw e;

        } catch (MessageDomainError e) {
            logger.warn("Domain error loading message aggregate (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading message aggregate: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public Optional<MessageAggregate> loadAggregateOptional(UUID messageId) {
        try {
            Optional<MessageAggregate> result = messageCommandRepository.loadOptional(messageId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Loaded message aggregate (optional): message_id={}, room_id={}, sender_id={}, status={}, is_active={}",
                    messageId,
                    result.get().message().roomId(),
                    result.get().message().senderId(),
                    result.get().message().status(),
                    result.get().message().isActive()
                );
            } else {
                logger.debug("No message aggregate found (optional): message_id={}", messageId);
            }
            
            return result;

        } catch (MessageDomainError e) {
            logger.warn("Domain error loading message aggregate (optional) (message_id={}): {}", messageId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading message aggregate (optional): message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public Optional<MessageAggregate> loadByRoomAndId(UUID roomId, UUID messageId) {
        try {
            Optional<MessageAggregate> result = messageCommandRepository.loadByRoomAndId(roomId, messageId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Loaded message by room+id: room_id={}, message_id={}, sender_id={}, status={}",
                    roomId,
                    messageId,
                    result.get().message().senderId(),
                    result.get().message().status()
                );
            } else {
                logger.debug(
                    "No message found by room+id: room_id={}, message_id={}",
                    roomId,
                    messageId
                );
            }
            
            return result;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error loading message by room+id: room_id={}, message_id={}, error={}",
                roomId,
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error loading message by room+id: room_id={}, message_id={}",
                roomId,
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean aggregateExists(UUID messageId) {
        try {
            boolean exists = messageCommandRepository.exists(messageId);
            logger.debug("Existence check: message_id={}, exists={}", messageId, exists);
            return exists;

        } catch (Exception e) {
            logger.error("Unexpected error checking message existence: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public boolean aggregateExistsInRoom(UUID roomId, UUID messageId) {
        try {
            boolean exists = messageCommandRepository.existsInRoom(roomId, messageId);
            logger.debug(
                "Existence check in room: room_id={}, message_id={}, exists={}",
                roomId,
                messageId,
                exists
            );
            return exists;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking message existence in room: room_id={}, message_id={}",
                roomId,
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean aggregateExistsByRoomAndSender(UUID roomId, UUID senderId) {
        try {
            boolean exists = messageCommandRepository.existsByRoomAndSender(roomId, senderId);
            logger.debug(
                "Existence check by room+sender: room_id={}, sender_id={}, exists={}",
                roomId,
                senderId,
                exists
            );
            return exists;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking message existence by room+sender: room_id={}, sender_id={}",
                roomId,
                senderId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Read Operations (for command orchestration & read models) ─

    @Override
    public List<MessageAggregate> bulkLoadByRoomId(UUID roomId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadByRoomId(roomId);
            logger.debug(
                "Bulk loaded {} message aggregates for room: room_id={}",
                aggregates.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading messages by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomId(UUID roomId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByRoomId(roomId);
            logger.debug(
                "Bulk loaded {} active message aggregates for room: room_id={}",
                aggregates.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active messages by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomIdLimited(UUID roomId, int limit) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByRoomIdLimited(roomId, limit);
            logger.debug(
                "Bulk loaded {} active message aggregates for room (limit={}): room_id={}",
                aggregates.size(),
                limit,
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active messages by room (limited): room_id={}, limit={}",
                roomId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomIdAfter(UUID roomId, UUID afterId, int limit) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByRoomIdAfter(roomId, afterId, limit);
            logger.debug(
                "Bulk loaded {} active message aggregates for room after cursor (limit={}): room_id={}, after_id={}",
                aggregates.size(),
                limit,
                roomId,
                afterId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active messages by room (cursor): room_id={}, after_id={}, limit={}",
                roomId,
                afterId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadBySenderId(UUID senderId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadBySenderId(senderId);
            logger.debug(
                "Bulk loaded {} message aggregates for sender: sender_id={}",
                aggregates.size(),
                senderId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading messages by sender: sender_id={}", senderId, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveBySenderId(UUID senderId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveBySenderId(senderId);
            logger.debug(
                "Bulk loaded {} active message aggregates for sender: sender_id={}",
                aggregates.size(),
                senderId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active messages by sender: sender_id={}", senderId, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveBySenderIdAndRooms(UUID senderId, Collection<UUID> roomIds) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveBySenderIdAndRooms(senderId, roomIds);
            logger.debug(
                "Bulk loaded {} active message aggregates for sender in {} rooms: sender_id={}",
                aggregates.size(),
                roomIds.size(),
                senderId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active messages by sender+rooms: sender_id={}, room_count={}",
                senderId,
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadByStatus(Message.Status status) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadByStatus(status);
            logger.debug(
                "Bulk loaded {} message aggregates by status: status={}, count={}",
                aggregates.size(),
                status,
                aggregates.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading messages by status: status={}", status, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByStatus(Message.Status status) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByStatus(status);
            logger.debug(
                "Bulk loaded {} active message aggregates by status: status={}, count={}",
                aggregates.size(),
                status,
                aggregates.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active messages by status: status={}", status, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveSentOlderThan(LocalDateTime olderThan) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveSentOlderThan(olderThan);
            logger.debug(
                "Bulk loaded {} active SENT messages older than threshold: older_than={}, count={}",
                aggregates.size(),
                olderThan,
                aggregates.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active SENT messages older than threshold: older_than={}",
                olderThan,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadRepliesTo(UUID parentId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadRepliesTo(parentId);
            logger.debug(
                "Bulk loaded {} reply message aggregates for parent: parent_id={}",
                aggregates.size(),
                parentId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading replies: parent_id={}", parentId, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveRepliesTo(UUID parentId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveRepliesTo(parentId);
            logger.debug(
                "Bulk loaded {} active reply message aggregates for parent: parent_id={}",
                aggregates.size(),
                parentId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active replies: parent_id={}", parentId, e);
            throw e;
        }
    }

    @Override
    public boolean hasReplies(UUID messageId) {
        try {
            boolean hasReplies = messageCommandRepository.hasReplies(messageId);
            logger.debug("Reply existence check: message_id={}, has_replies={}", messageId, hasReplies);
            return hasReplies;

        } catch (Exception e) {
            logger.error("Unexpected error checking reply existence: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadByIds(Collection<UUID> messageIds) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadByIds(messageIds);
            logger.debug(
                "Bulk loaded {} message aggregates for {} requested IDs",
                aggregates.size(),
                messageIds.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading messages by IDs: requested_count={}",
                messageIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByIds(Collection<UUID> messageIds) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByIds(messageIds);
            logger.debug(
                "Bulk loaded {} active message aggregates for {} requested IDs",
                aggregates.size(),
                messageIds.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active messages by IDs: requested_count={}",
                messageIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveByRoomAndSender(UUID roomId, UUID senderId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByRoomAndSender(roomId, senderId);
            logger.debug(
                "Bulk loaded {} active message aggregates for sender in room: room_id={}, sender_id={}",
                aggregates.size(),
                roomId,
                senderId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active messages by room+sender: room_id={}, sender_id={}",
                roomId,
                senderId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveWithImagesByRoomId(UUID roomId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveWithImagesByRoomId(roomId);
            logger.debug(
                "Bulk loaded {} active messages with images for room: room_id={}",
                aggregates.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active messages with images: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageAggregate> bulkLoadActiveRepliesByRoomId(UUID roomId) {
        try {
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveRepliesByRoomId(roomId);
            logger.debug(
                "Bulk loaded {} active reply messages for room: room_id={}",
                aggregates.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active replies by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Command Operations ────────────────────────────────────────

    @Override
    public int bulkDeleteOldMessagesInRoom(UUID roomId, LocalDateTime olderThan, UUID actorId) {
        try {
            int deletedCount = messageCommandRepository.bulkDeleteOldMessagesInRoom(roomId, olderThan, actorId);
            logger.info(
                "Bulk deleted {} old messages in room: room_id={}, older_than={}, actor_id={}",
                deletedCount,
                roomId,
                olderThan,
                actorId
            );
            return deletedCount;

        } catch (Exception e) {
            logger.error(
                "Unexpected error during bulk delete old messages: room_id={}, older_than={}, actor_id={}",
                roomId,
                olderThan,
                actorId,
                e
            );
            throw e;
        }
    }

    @Override
    public int bulkUpdateStatus(Collection<UUID> messageIds, Message.Status newStatus, UUID actorId) {
        try {
            int updatedCount = messageCommandRepository.bulkUpdateStatus(messageIds, newStatus, actorId);
            logger.info(
                "Bulk updated status for {} messages: target_status={}, actor_id={}, requested_count={}",
                updatedCount,
                newStatus,
                actorId,
                messageIds.size()
            );
            return updatedCount;

        } catch (Exception e) {
            logger.error(
                "Unexpected error during bulk status update: target_status={}, actor_id={}, requested_count={}",
                newStatus,
                actorId,
                messageIds.size(),
                e
            );
            throw e;
        }
    }
    @Override
    public int bulkMarkAsReceivedInRoom(UUID roomId, UUID actorId) {
        try {
            // Load all active messages in the room
            List<MessageAggregate> aggregates = messageCommandRepository.bulkLoadActiveByRoomId(roomId);
            logger.debug(
                "Loaded {} active messages for bulk markAsReceived: room_id={}, actor_id={}",
                aggregates.size(),
                roomId,
                actorId
            );

            int successCount = 0;
            int skippedCount = 0;

            for (MessageAggregate aggregate : aggregates) {
                try {
                    // Apply domain logic to mark as received
                    // Domain aggregate handles authorization: only receiver (not sender) can mark as received
                    aggregate.markAsReceived(actorId);
                    
                    // Persist the updated aggregate
                    messageCommandRepository.save(aggregate);
                    
                    successCount++;
                    logger.trace(
                        "Successfully marked message as RECEIVED (bulk): message_id={}, room_id={}, actor_id={}, status={}",
                        aggregate.message().id(),
                        roomId,
                        actorId,
                        aggregate.message().status()
                    );

                } catch (MessageUnauthorizedError e) {
                    // Expected: actor is sender, not receiver — skip silently
                    logger.trace(
                        "Skipped message (unauthorized - actor is sender): message_id={}, room_id={}, actor_id={}",
                        e.getMessageId(),
                        roomId,
                        actorId
                    );
                    skippedCount++;

                } catch (MessageStateTransitionError e) {
                    // Expected: message already in RECEIVED/SEEN state — skip silently
                    logger.trace(
                        "Skipped message (invalid state transition): message_id={}, room_id={}, current={}, target={}, reason={}",
                        e.getMessageId(),
                        roomId,
                        e.getCurrentState(),
                        e.getTargetState(),
                        e.getReason()
                    );
                    skippedCount++;

                } catch (MessageOperationNotAllowedError e) {
                    // Expected: message deleted or inactive — skip silently
                    logger.trace(
                        "Skipped message (operation not allowed): message_id={}, room_id={}, operation={}, reason={}",
                        e.getMessageId(),
                        roomId,
                        e.getOperation(),
                        e.getReason()
                    );
                    skippedCount++;

                } catch (MessageDomainError e) {
                    // Log but continue processing other messages
                    logger.warn(
                        "Domain error marking message as received (bulk, non-blocking): message_id={}, room_id={}, actor_id={}, error={}",
                        aggregate.message().id(),
                        roomId,
                        actorId,
                        e.getMessage()
                    );
                    skippedCount++;

                } catch (Exception e) {
                    // Log but continue processing other messages
                    logger.warn(
                        "Unexpected error marking message as received (bulk, non-blocking): message_id={}, room_id={}, actor_id={}, error={}",
                        aggregate.message().id(),
                        roomId,
                        actorId,
                        e.getMessage()
                    );
                    skippedCount++;
                }
            }

            logger.info(
                "Bulk markAsReceived completed: room_id={}, actor_id={}, success={}, skipped={}, total={}",
                roomId,
                actorId,
                successCount,
                skippedCount,
                aggregates.size()
            );
            return successCount;

        } catch (Exception e) {
            logger.error(
                "Unexpected error during bulkMarkAsReceivedInRoom: room_id={}, actor_id={}",
                roomId,
                actorId,
                e
            );
            throw e;
        }
    }
}