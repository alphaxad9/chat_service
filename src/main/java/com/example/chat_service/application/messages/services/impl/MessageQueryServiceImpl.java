// chat_service/src/main/java/com/example/chat_service/application/messages/services/impl/MessageQueryServiceImpl.java

package com.example.chat_service.application.messages.services.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.messages.services.MessageQueryServiceInterface;
import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.domain.messages.exceptions.MessageDomainError;
import com.example.chat_service.domain.messages.exceptions.MessageNotFoundError;
import com.example.chat_service.domain.messages.repositories.MessageQueryRepository;

/**
 * Application-layer implementation of {@link MessageQueryServiceInterface}.
 *
 * <p>Orchestrates message query (read) operations by coordinating domain entities
 * with infrastructure query repositories. All methods run within a read-only
 * transaction boundary to optimize database access and ensure consistency.</p>
 *
 * <p><strong>Query pattern:</strong> All query methods accept IDs or filters as parameters,
 * delegate to {@link MessageQueryRepository}, apply read-side business logic if needed,
 * and return domain entities or projections. No state mutations occur.</p>
 *
 * <p><strong>CQRS read-side:</strong> This implementation focuses purely on read operations.
 * All queries automatically exclude messages where {@code isDeleted = true} unless explicitly
 * documented, ensuring only active messages are returned.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Query methods accept IDs/filters — service delegates to repository, applies read logic</li>
 *   <li>All queries exclude {@code isDeleted = true} messages by default (active messages only)</li>
 *   <li>Read-side projections and DTOs are handled via repository or mapped here</li>
 *   <li>No state mutations — this service is strictly for read operations</li>
 *   <li>All public methods are {@code @Transactional(readOnly = true)} for optimization</li>
 *   <li>Logging at DEBUG level for queries, WARN for not-found scenarios</li>
 * </ul></p>
 */
@Service
@Transactional(readOnly = true)
public class MessageQueryServiceImpl implements MessageQueryServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueryServiceImpl.class);

    private final MessageQueryRepository messageQueryRepository;

    public MessageQueryServiceImpl(MessageQueryRepository messageQueryRepository) {
        this.messageQueryRepository = messageQueryRepository;
    }

    // ── Single Entity Queries (Active Messages Only) ──────────────────

    @Override
    public Optional<Message> getMessageById(UUID messageId) {
        try {
            Optional<Message> result = messageQueryRepository.findById(messageId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved message by ID: message_id={}, room_id={}, sender_id={}, status={}, has_image={}, is_reply={}",
                    result.get().id(),
                    result.get().roomId(),
                    result.get().senderId(),
                    result.get().status(),
                    result.get().hasImage(),
                    result.get().isReply()
                );
            } else {
                logger.debug("No active message found by ID: message_id={}", messageId);
            }
            
            return result;

        } catch (MessageNotFoundError e) {
            logger.debug("Message not found by ID: message_id={}", messageId);
            return Optional.empty();

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message by ID (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving message by ID: message_id={}", messageId, e);
            throw e;
        }
    }

    @Override
    public Optional<Message> getMessageByIdIncludingDeleted(UUID messageId) {
        try {
            Optional<Message> result = messageQueryRepository.findByIdIncludingDeleted(messageId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved message by ID (including deleted): message_id={}, room_id={}, sender_id={}, is_deleted={}",
                    result.get().id(),
                    result.get().roomId(),
                    result.get().senderId(),
                    result.get().isDeleted()
                );
            } else {
                logger.debug("No message found by ID (including deleted): message_id={}", messageId);
            }
            
            return result;

        } catch (MessageNotFoundError e) {
            logger.debug("Message not found by ID (including deleted): message_id={}", messageId);
            return Optional.empty();

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message by ID including deleted (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message by ID including deleted: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean isActiveMessage(UUID messageId) {
        try {
            boolean isActive = messageQueryRepository.isActiveMessage(messageId);
            logger.debug(
                "Active message check: message_id={}, is_active={}",
                messageId,
                isActive
            );
            return isActive;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error checking active message (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active message: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    // ── Latest Message Queries (Active Messages Only) ─────────────────

    @Override
    public Optional<Message> getLatestActiveMessageByRoomId(UUID roomId) {
        try {
            Optional<Message> result = messageQueryRepository.findLatestActiveByRoomId(roomId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved latest active message for room: room_id={}, message_id={}, sender_id={}, created_at={}",
                    roomId,
                    result.get().id(),
                    result.get().senderId(),
                    result.get().createdAt()
                );
            } else {
                logger.debug("No active messages found for room: room_id={}", roomId);
            }
            
            return result;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving latest message by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving latest message by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public Map<UUID, Message> getLatestActiveMessagesByRoomIds(Collection<UUID> roomIds) {
        try {
            Map<UUID, Message> result = messageQueryRepository.findLatestActiveByRoomIds(roomIds);
            logger.debug(
                "Bulk retrieved latest active messages: requested_rooms={}, returned_messages={}",
                roomIds.size(),
                result.size()
            );
            return result;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error bulk retrieving latest messages by rooms (requested_count={}): {}",
                roomIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving latest messages by rooms: requested_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public Map<UUID, MessageSummary> getLatestActiveMessageSummariesByRoomIds(Collection<UUID> roomIds) {
        try {
            Map<UUID, MessageQueryRepository.MessageSummary> repoSummaries = 
                messageQueryRepository.findLatestActiveSummariesByRoomIds(roomIds);
            
            // Map repository projection to application-layer projection
            Map<UUID, MessageSummary> result = repoSummaries.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> mapToMessageSummary(entry.getValue())
                ));
            
            logger.debug(
                "Bulk retrieved latest active message summaries: requested_rooms={}, returned_summaries={}",
                roomIds.size(),
                result.size()
            );
            return result;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error bulk retrieving latest message summaries by rooms (requested_count={}): {}",
                roomIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving latest message summaries by rooms: requested_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    // ── Bulk Queries by Room (Active Messages Only) ───────────────────

    @Override
    public List<Message> getAllActiveMessagesByRoomId(UUID roomId) {
        try {
            List<Message> messages = messageQueryRepository.findAllActiveByRoomId(roomId);
            logger.debug(
                "Retrieved {} active messages for room: room_id={}",
                messages.size(),
                roomId
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesByRoomIdLimited(UUID roomId, int limit) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByRoomIdLimited(roomId, limit);
            logger.debug(
                "Retrieved {} active messages for room (limited): room_id={}, limit={}",
                messages.size(),
                roomId,
                limit
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving limited active messages by room (room_id={}, limit={}): {}",
                roomId,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving limited active messages by room: room_id={}, limit={}",
                roomId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesByRoomIdAfter(UUID roomId, UUID afterId, int limit) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByRoomIdAfter(roomId, afterId, limit);
            logger.debug(
                "Retrieved {} active messages for room after cursor: room_id={}, after_id={}, limit={}",
                messages.size(),
                roomId,
                afterId,
                limit
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by room after cursor (room_id={}, after_id={}, limit={}): {}",
                roomId,
                afterId,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by room after cursor: room_id={}, after_id={}, limit={}",
                roomId,
                afterId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesByRoomIdBefore(UUID roomId, UUID beforeId, int limit) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByRoomIdBefore(roomId, beforeId, limit);
            logger.debug(
                "Retrieved {} active messages for room before cursor: room_id={}, before_id={}, limit={}",
                messages.size(),
                roomId,
                beforeId,
                limit
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by room before cursor (room_id={}, before_id={}, limit={}): {}",
                roomId,
                beforeId,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by room before cursor: room_id={}, before_id={}, limit={}",
                roomId,
                beforeId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public long countActiveMessagesByRoomId(UUID roomId) {
        try {
            long count = messageQueryRepository.countActiveByRoomId(roomId);
            logger.debug(
                "Counted {} active messages for room: room_id={}",
                count,
                roomId
            );
            return count;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error counting active messages by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error counting active messages by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Queries by Sender (Active Messages Only) ─────────────────

    @Override
    public List<Message> getAllActiveMessagesBySenderId(UUID senderId) {
        try {
            List<Message> messages = messageQueryRepository.findAllActiveBySenderId(senderId);
            logger.debug(
                "Retrieved {} active messages for sender: sender_id={}",
                messages.size(),
                senderId
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by sender (sender_id={}): {}",
                senderId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by sender: sender_id={}",
                senderId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesBySenderIdAndRooms(UUID senderId, Collection<UUID> roomIds) {
        try {
            List<Message> messages = messageQueryRepository.findActiveBySenderIdAndRooms(senderId, roomIds);
            logger.debug(
                "Retrieved {} active messages for sender in {} rooms: sender_id={}",
                messages.size(),
                roomIds.size(),
                senderId
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by sender and rooms (sender_id={}, room_count={}): {}",
                senderId,
                roomIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by sender and rooms: sender_id={}, room_count={}",
                senderId,
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public long countActiveMessagesBySenderId(UUID senderId) {
        try {
            long count = messageQueryRepository.countActiveBySenderId(senderId);
            logger.debug(
                "Counted {} active messages for sender: sender_id={}",
                count,
                senderId
            );
            return count;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error counting active messages by sender (sender_id={}): {}",
                senderId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error counting active messages by sender: sender_id={}",
                senderId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Queries by Status (Active Messages Only) ─────────────────

    @Override
    public List<Message> getActiveMessagesByStatus(Message.Status status) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByStatus(status);
            logger.debug(
                "Retrieved {} active messages with status: status={}, count={}",
                messages.size(),
                status,
                messages.size()
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by status (status={}): {}",
                status,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by status: status={}",
                status,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesByRoomAndStatus(UUID roomId, Message.Status status) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByRoomAndStatus(roomId, status);
            logger.debug(
                "Retrieved {} active messages for room with status: room_id={}, status={}",
                messages.size(),
                roomId,
                status
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages by room and status (room_id={}, status={}): {}",
                roomId,
                status,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages by room and status: room_id={}, status={}",
                roomId,
                status,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveSentMessagesOlderThan(LocalDateTime olderThan) {
        try {
            List<Message> messages = messageQueryRepository.findActiveSentOlderThan(olderThan);
            logger.debug(
                "Retrieved {} active SENT messages older than threshold: older_than={}, count={}",
                messages.size(),
                olderThan,
                messages.size()
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active SENT messages older than threshold (older_than={}): {}",
                olderThan,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active SENT messages older than threshold: older_than={}",
                olderThan,
                e
            );
            throw e;
        }
    }

    // ── Reply Chain Queries (Active Messages Only) ────────────────────

    @Override
    public List<Message> getActiveRepliesTo(UUID parentId) {
        try {
            List<Message> replies = messageQueryRepository.findActiveRepliesTo(parentId);
            logger.debug(
                "Retrieved {} active replies for parent message: parent_id={}",
                replies.size(),
                parentId
            );
            return replies;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active replies to parent (parent_id={}): {}",
                parentId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active replies to parent: parent_id={}",
                parentId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveRepliesToLimited(UUID parentId, int limit) {
        try {
            List<Message> replies = messageQueryRepository.findActiveRepliesToLimited(parentId, limit);
            logger.debug(
                "Retrieved {} active replies for parent message (limited): parent_id={}, limit={}",
                replies.size(),
                parentId,
                limit
            );
            return replies;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving limited active replies to parent (parent_id={}, limit={}): {}",
                parentId,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving limited active replies to parent: parent_id={}, limit={}",
                parentId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean hasActiveReplies(UUID messageId) {
        try {
            boolean hasReplies = messageQueryRepository.hasActiveReplies(messageId);
            logger.debug(
                "Active replies check: message_id={}, has_replies={}",
                messageId,
                hasReplies
            );
            return hasReplies;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error checking active replies (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active replies: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public long countActiveRepliesTo(UUID parentId) {
        try {
            long count = messageQueryRepository.countActiveRepliesTo(parentId);
            logger.debug(
                "Counted {} active replies for parent message: parent_id={}",
                count,
                parentId
            );
            return count;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error counting active replies to parent (parent_id={}): {}",
                parentId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error counting active replies to parent: parent_id={}",
                parentId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Lookup Queries (Active Messages Only) ───────────────────

    @Override
    public List<Message> getActiveMessagesByIds(Collection<UUID> messageIds) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByIds(messageIds);
            logger.debug(
                "Bulk retrieved {} active messages for {} requested IDs",
                messages.size(),
                messageIds.size()
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error bulk retrieving messages by IDs (requested_count={}): {}",
                messageIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving messages by IDs: requested_count={}",
                messageIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesByRoomIds(Collection<UUID> roomIds) {
        try {
            List<Message> messages = messageQueryRepository.findActiveByRoomIds(roomIds);
            logger.debug(
                "Bulk retrieved {} active messages for {} room IDs",
                messages.size(),
                roomIds.size()
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error bulk retrieving messages by room IDs (room_count={}): {}",
                roomIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving messages by room IDs: room_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    // ── Activity/Time-Based Queries (Active Messages Only) ────────────

    @Override
    public List<Message> getActiveMessagesCreatedSince(LocalDateTime sinceTimestamp, int limit) {
        try {
            List<Message> messages = messageQueryRepository.findActiveCreatedSince(sinceTimestamp, limit);
            logger.debug(
                "Retrieved {} active messages created since {}: limit={}",
                messages.size(),
                sinceTimestamp,
                limit
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages created since (since={}, limit={}): {}",
                sinceTimestamp,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages created since: since={}, limit={}",
                sinceTimestamp,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesUpdatedSince(LocalDateTime sinceTimestamp, int limit) {
        try {
            List<Message> messages = messageQueryRepository.findActiveUpdatedSince(sinceTimestamp, limit);
            logger.debug(
                "Retrieved {} active messages updated since {}: limit={}",
                messages.size(),
                sinceTimestamp,
                limit
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages updated since (since={}, limit={}): {}",
                sinceTimestamp,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages updated since: since={}, limit={}",
                sinceTimestamp,
                limit,
                e
            );
            throw e;
        }
    }

    // ── Content/Image Queries (Active Messages Only) ──────────────────

    @Override
    public List<Message> getActiveMessagesWithImagesByRoomId(UUID roomId) {
        try {
            List<Message> messages = messageQueryRepository.findActiveWithImagesByRoomId(roomId);
            logger.debug(
                "Retrieved {} active messages with images for room: room_id={}",
                messages.size(),
                roomId
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages with images by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages with images by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Message> getActiveMessagesWithImagesBySenderId(UUID senderId) {
        try {
            List<Message> messages = messageQueryRepository.findActiveWithImagesBySenderId(senderId);
            logger.debug(
                "Retrieved {} active messages with images for sender: sender_id={}",
                messages.size(),
                senderId
            );
            return messages;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving active messages with images by sender (sender_id={}): {}",
                senderId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active messages with images by sender: sender_id={}",
                senderId,
                e
            );
            throw e;
        }
    }

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    @Override
    public List<MessageSummary> getActiveMessageSummariesByRoomId(UUID roomId) {
        try {
            List<MessageQueryRepository.MessageSummary> repoSummaries = 
                messageQueryRepository.findActiveSummariesByRoomId(roomId);
            
            // Map repository projection to application-layer projection
            List<MessageSummary> summaries = repoSummaries.stream()
                .map(this::mapToMessageSummary)
                .toList();
            
            logger.debug(
                "Retrieved {} active message summaries for room: room_id={}",
                summaries.size(),
                roomId
            );
            return summaries;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message summaries by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message summaries by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageSummary> getActiveMessageSummariesByIds(Collection<UUID> messageIds) {
        try {
            List<MessageQueryRepository.MessageSummary> repoSummaries = 
                messageQueryRepository.findActiveSummariesByIds(messageIds);
            
            // Map repository projection to application-layer projection
            List<MessageSummary> summaries = repoSummaries.stream()
                .map(this::mapToMessageSummary)
                .toList();
            
            logger.debug(
                "Retrieved {} active message summaries for {} requested IDs",
                summaries.size(),
                messageIds.size()
            );
            return summaries;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message summaries by IDs (requested_count={}): {}",
                messageIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message summaries by IDs: requested_count={}",
                messageIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageSummary> getActiveMessageSummariesByRoomIdAfter(UUID roomId, UUID afterId, int limit) {
        try {
            List<MessageQueryRepository.MessageSummary> repoSummaries = 
                messageQueryRepository.findActiveSummariesByRoomIdAfter(roomId, afterId, limit);
            
            // Map repository projection to application-layer projection
            List<MessageSummary> summaries = repoSummaries.stream()
                .map(this::mapToMessageSummary)
                .toList();
            
            logger.debug(
                "Retrieved {} active message summaries for room after cursor: room_id={}, after_id={}, limit={}",
                summaries.size(),
                roomId,
                afterId,
                limit
            );
            return summaries;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message summaries by room after cursor (room_id={}, after_id={}, limit={}): {}",
                roomId,
                afterId,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message summaries by room after cursor: room_id={}, after_id={}, limit={}",
                roomId,
                afterId,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MessageSummary> getActiveReplySummariesTo(UUID parentId) {
        try {
            List<MessageQueryRepository.MessageSummary> repoSummaries = 
                messageQueryRepository.findActiveReplySummariesTo(parentId);
            
            // Map repository projection to application-layer projection
            List<MessageSummary> summaries = repoSummaries.stream()
                .map(this::mapToMessageSummary)
                .toList();
            
            logger.debug(
                "Retrieved {} active reply summaries for parent message: parent_id={}",
                summaries.size(),
                parentId
            );
            return summaries;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving reply summaries to parent (parent_id={}): {}",
                parentId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving reply summaries to parent: parent_id={}",
                parentId,
                e
            );
            throw e;
        }
    }

    // ── Read-Side Utility Queries ────────────────────────────────────

    @Override
    public boolean activeMessageExists(UUID messageId) {
        try {
            boolean exists = messageQueryRepository.findById(messageId).isPresent();
            logger.debug(
                "Active message existence check: message_id={}, exists={}",
                messageId,
                exists
            );
            return exists;

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error checking active message existence (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active message existence: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public UUID getMessageSenderId(UUID messageId) {
        try {
            Optional<Message> message = messageQueryRepository.findById(messageId);
            
            if (message.isPresent()) {
                UUID senderId = message.get().senderId();
                logger.debug(
                    "Retrieved message sender ID: message_id={}, sender_id={}",
                    messageId,
                    senderId
                );
                return senderId;
            } else {
                logger.debug(
                    "No active message found for sender check: message_id={}, returning null",
                    messageId
                );
                return null;
            }

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message sender ID (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message sender ID: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public UUID getMessageRoomId(UUID messageId) {
        try {
            Optional<Message> message = messageQueryRepository.findById(messageId);
            
            if (message.isPresent()) {
                UUID roomId = message.get().roomId();
                logger.debug(
                    "Retrieved message room ID: message_id={}, room_id={}",
                    messageId,
                    roomId
                );
                return roomId;
            } else {
                logger.debug(
                    "No active message found for room check: message_id={}, returning null",
                    messageId
                );
                return null;
            }

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message room ID (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message room ID: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public Message.Status getMessageStatus(UUID messageId) {
        try {
            Optional<Message> message = messageQueryRepository.findById(messageId);
            
            if (message.isPresent()) {
                Message.Status status = message.get().status();
                logger.debug(
                    "Retrieved message status: message_id={}, status={}",
                    messageId,
                    status
                );
                return status;
            } else {
                logger.debug(
                    "No active message found for status check: message_id={}, returning null",
                    messageId
                );
                return null;
            }

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message status (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message status: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    @Override
    public LocalDateTime getMessageCreatedAt(UUID messageId) {
        try {
            Optional<Message> message = messageQueryRepository.findById(messageId);
            
            if (message.isPresent()) {
                LocalDateTime createdAt = message.get().createdAt();
                logger.debug(
                    "Retrieved message creation timestamp: message_id={}, created_at={}",
                    messageId,
                    createdAt
                );
                return createdAt;
            } else {
                logger.debug(
                    "No active message found for creation timestamp check: message_id={}, returning null",
                    messageId
                );
                return null;
            }

        } catch (MessageDomainError e) {
            logger.warn(
                "Domain error retrieving message creation timestamp (message_id={}): {}",
                messageId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving message creation timestamp: message_id={}",
                messageId,
                e
            );
            throw e;
        }
    }

    // ── Private Helper Methods ───────────────────────────────────────

    /**
     * Maps repository-level MessageSummary to application-layer MessageSummary.
     * Ensures application-layer contracts remain decoupled from infrastructure.
     */
    private MessageSummary mapToMessageSummary(MessageQueryRepository.MessageSummary repo) {
        return new MessageSummary(
            repo.messageId(),
            repo.roomId(),
            repo.senderId(),
            repo.contentPreview(),
            repo.hasImage(),
            repo.isReply(),
            repo.parentId(),
            repo.status(),
            repo.isSeen(),
            repo.createdAt(),
            repo.updatedAt(),
            repo.seenAt()
        );
    }
}