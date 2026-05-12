// chat_service/src/main/java/com/example/chat_service/application/members/services/impl/MemberCommandServiceImpl.java

package com.example.chat_service.application.members.services.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.members.services.MemberCommandServiceInterface;
import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.domain.members.exceptions.InvalidMemberEntityError;
import com.example.chat_service.domain.members.exceptions.InvalidUnreadMessagesError;
import com.example.chat_service.domain.members.exceptions.MemberAlreadyExistsError;
import com.example.chat_service.domain.members.exceptions.MemberDomainError;
import com.example.chat_service.domain.members.exceptions.MemberNotFoundError;
import com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError;
import com.example.chat_service.domain.members.exceptions.MemberStateTransitionError;
import com.example.chat_service.domain.members.exceptions.MemberUnauthorizedError;
import com.example.chat_service.domain.members.exceptions.MemberUnauthorizedErrorWithNoId;
import com.example.chat_service.domain.members.repositories.MemberCommandRepository;

/**
 * Application-layer implementation of {@link MemberCommandServiceInterface}.
 *
 * <p>Orchestrates member command (write) operations by coordinating domain aggregates
 * with infrastructure repositories. All methods run within a transaction boundary
 * to ensure consistency.</p>
 *
 * <p><strong>Command pattern:</strong> All command methods (except {@code createMember})
 * accept IDs as parameters, load the aggregate via repository, apply domain logic,
 * then persist the updated state. This ensures a consistent load-act-save flow.</p>
 *
 * <p><strong>No event publishing:</strong> This implementation focuses purely on
 * command orchestration. Event emission (outbox, Kafka, etc.) should be added
 * in a separate layer or via domain events when the infrastructure is ready.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Command methods accept IDs — service loads aggregate, applies business logic, persists result</li>
 *   <li>Only {@code createMember} accepts a pre-built aggregate (for initial construction)</li>
 *   <li>Business rules and validation live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence) are delegated to {@link MemberCommandRepository}</li>
 *   <li>All public methods are {@code @Transactional} for atomicity</li>
 * </ul></p>
 */
@Service
@Transactional
public class MemberCommandServiceImpl implements MemberCommandServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MemberCommandServiceImpl.class);

    private final MemberCommandRepository memberCommandRepository;

    public MemberCommandServiceImpl(MemberCommandRepository memberCommandRepository) {
        this.memberCommandRepository = memberCommandRepository;
    }

    // ── Core Lifecycle Commands ────────────────────────────────────────

    @Override
    public MemberAggregate createMember(MemberAggregate aggregate) {
        try {
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully created member (member_id={}, user_id={}, room_id={}, status={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId(),
                aggregate.member().status()
            );
            return aggregate;

        } catch (MemberAlreadyExistsError e) {
            logger.warn(
                "Member creation failed: member already exists (member_id={}, user_id={}, room_id={})",
                e.getMemberId(),
                e.getUserId(),
                e.getRoomId()
            );
            throw e;

        } catch (InvalidMemberEntityError e) {
            logger.warn(
                "Member creation failed: invalid entity data (reason={}, member_id={}, user_id={}, room_id={})",
                e.getReason(),
                e.getMemberId(),
                e.getUserId(),
                e.getRoomId()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Member creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating member (member_id={})",
                aggregate != null && aggregate.member() != null ? aggregate.member().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public MemberAggregate leaveRoom(UUID memberId, UUID requesterId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.leave(requesterId);
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully left room (member_id={}, user_id={}, room_id={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for leave operation: member_id={}", memberId);
            throw e;

        } catch (MemberUnauthorizedError e) {
            logger.warn(
                "Leave operation unauthorized: member_id={}, actor_id={}, operation={}",
                e.getMemberId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Leave operation unauthorized (no member ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberStateTransitionError e) {
            logger.warn(
                "Leave operation failed: invalid state transition (member_id={}, current={}, target={}, reason={})",
                e.getMemberId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Leave operation not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Leave operation domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during leave operation: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public MemberAggregate removeMember(UUID memberId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.remove();
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully removed member (member_id={}, user_id={}, room_id={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for remove operation: member_id={}", memberId);
            throw e;

        } catch (MemberStateTransitionError e) {
            logger.warn(
                "Remove operation failed: invalid state transition (member_id={}, current={}, target={}, reason={})",
                e.getMemberId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Remove operation not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Remove operation domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during remove operation: member_id={}", memberId, e);
            throw e;
        }
    }

    // ── Role Management Commands ───────────────────────────────────────

    @Override
    public MemberAggregate promoteToAdmin(UUID memberId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.promote();
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully promoted member to admin (member_id={}, user_id={}, room_id={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for promote operation: member_id={}", memberId);
            throw e;

        } catch (MemberStateTransitionError e) {
            logger.warn(
                "Promote operation failed: invalid state transition (member_id={}, current={}, target={}, reason={})",
                e.getMemberId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Promote operation not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Promote operation domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during promote operation: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public MemberAggregate demoteToUser(UUID memberId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.demote();
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully demoted member to user (member_id={}, user_id={}, room_id={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for demote operation: member_id={}", memberId);
            throw e;

        } catch (MemberStateTransitionError e) {
            logger.warn(
                "Demote operation failed: invalid state transition (member_id={}, current={}, target={}, reason={})",
                e.getMemberId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Demote operation not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Demote operation domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during demote operation: member_id={}", memberId, e);
            throw e;
        }
    }

    // ── Unread Messages Commands ───────────────────────────────────────

    @Override
    public MemberAggregate addUnreadMessages(UUID memberId, int amount) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.addUnreadMessages(amount);
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully added unread messages (member_id={}, user_id={}, room_id={}, added={}, new_total={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId(),
                amount,
                aggregate.member().unreadMessages()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for addUnreadMessages operation: member_id={}", memberId);
            throw e;

        } catch (InvalidUnreadMessagesError e) {
            logger.warn(
                "Add unread messages failed: invalid increment (member_id={}, current={}, attempted={}, reason={})",
                e.getMemberId(),
                e.getCurrentValue(),
                e.getIncrementValue(),
                e.getReason()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Add unread messages not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Add unread messages domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during addUnreadMessages operation: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public MemberAggregate markAllRead(UUID memberId, UUID requesterId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.markAllRead(requesterId);
            memberCommandRepository.save(aggregate);

            logger.info(
                "Successfully marked all messages as read (member_id={}, user_id={}, room_id={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for markAllRead operation: member_id={}", memberId);
            throw e;

        } catch (MemberUnauthorizedError e) {
            logger.warn(
                "Mark all read unauthorized: member_id={}, actor_id={}, operation={}",
                e.getMemberId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Mark all read unauthorized (no member ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Mark all read not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Mark all read domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during markAllRead operation: member_id={}", memberId, e);
            throw e;
        }
    }

    // ── Utility Commands ───────────────────────────────────────────────

    @Override
    public MemberAggregate touch(UUID memberId, UUID requesterId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.touch(requesterId);
            memberCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched member (member_id={}, user_id={}, room_id={}, updated_at={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId(),
                aggregate.member().updatedAt()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for touch operation: member_id={}", memberId);
            throw e;

        } catch (MemberUnauthorizedError e) {
            logger.warn(
                "Touch unauthorized: member_id={}, actor_id={}, operation={}",
                e.getMemberId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Touch unauthorized (no member ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Touch not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Touch domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during touch operation: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public MemberAggregate touchInternal(UUID memberId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            aggregate.touchInternal();
            memberCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched member internally (member_id={}, user_id={}, room_id={}, updated_at={})",
                aggregate.member().id(),
                aggregate.member().userId(),
                aggregate.member().roomId(),
                aggregate.member().updatedAt()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member not found for touchInternal operation: member_id={}", memberId);
            throw e;

        } catch (MemberOperationNotAllowedError e) {
            logger.warn(
                "Touch internal not allowed: member_id={}, operation={}, reason={}",
                e.getMemberId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Touch internal domain error (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during touchInternal operation: member_id={}", memberId, e);
            throw e;
        }
    }

    // ── Query Support Methods (for command orchestration) ──────────────

    @Override
    public MemberAggregate loadAggregate(UUID memberId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.load(memberId);
            logger.debug("Loaded member aggregate: member_id={}", memberId);
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member aggregate not found: member_id={}", memberId);
            throw e;

        } catch (MemberDomainError e) {
            logger.warn("Domain error loading member aggregate (member_id={}): {}", memberId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading member aggregate: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public MemberAggregate loadAggregateByUserAndRoom(UUID userId, UUID roomId) {
        try {
            MemberAggregate aggregate = memberCommandRepository.loadByUserAndRoom(userId, roomId);
            logger.debug(
                "Loaded member aggregate by user+room: user_id={}, room_id={}, member_id={}",
                userId,
                roomId,
                aggregate.member().id()
            );
            return aggregate;

        } catch (MemberNotFoundError e) {
            logger.warn("Member aggregate not found by user+room: user_id={}, room_id={}", userId, roomId);
            throw e;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error loading member by user+room (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error loading member by user+room: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public Optional<MemberAggregate> loadAggregateByUserAndRoomOptional(UUID userId, UUID roomId) {
        try {
            Optional<MemberAggregate> result = memberCommandRepository.loadByUserAndRoomOptional(userId, roomId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Loaded member aggregate by user+room (optional): user_id={}, room_id={}, member_id={}",
                    userId,
                    roomId,
                    result.get().member().id()
                );
            } else {
                logger.debug(
                    "No member aggregate found by user+room (optional): user_id={}, room_id={}",
                    userId,
                    roomId
                );
            }
            
            return result;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error loading member by user+room (optional) (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error loading member by user+room (optional): user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean aggregateExists(UUID memberId) {
        try {
            boolean exists = memberCommandRepository.exists(memberId);
            logger.debug("Existence check: member_id={}, exists={}", memberId, exists);
            return exists;

        } catch (Exception e) {
            logger.error("Unexpected error checking member existence: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public boolean aggregateExistsByUserAndRoom(UUID userId, UUID roomId) {
        try {
            boolean exists = memberCommandRepository.existsByUserAndRoom(userId, roomId);
            logger.debug(
                "Existence check by user+room: user_id={}, room_id={}, exists={}",
                userId,
                roomId,
                exists
            );
            return exists;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking member existence by user+room: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Read Operations (for command orchestration & read models) ─

    @Override
    public List<MemberAggregate> bulkLoadByRoomId(UUID roomId) {
        try {
            List<MemberAggregate> aggregates = memberCommandRepository.bulkLoadByRoomId(roomId);
            logger.debug(
                "Bulk loaded {} member aggregates for room: room_id={}",
                aggregates.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading members by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByRoomId(UUID roomId) {
        try {
            List<MemberAggregate> aggregates = memberCommandRepository.bulkLoadActiveByRoomId(roomId);
            logger.debug(
                "Bulk loaded {} active member aggregates for room: room_id={}",
                aggregates.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active members by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public List<MemberAggregate> bulkLoadByUserId(UUID userId) {
        try {
            List<MemberAggregate> aggregates = memberCommandRepository.bulkLoadByUserId(userId);
            logger.debug(
                "Bulk loaded {} member aggregates for user: user_id={}",
                aggregates.size(),
                userId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading members by user: user_id={}", userId, e);
            throw e;
        }
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByUserId(UUID userId) {
        try {
            List<MemberAggregate> aggregates = memberCommandRepository.bulkLoadActiveByUserId(userId);
            logger.debug(
                "Bulk loaded {} active member aggregates for user: user_id={}",
                aggregates.size(),
                userId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active members by user: user_id={}", userId, e);
            throw e;
        }
    }

    @Override
    public List<MemberAggregate> bulkLoadByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        try {
            List<MemberAggregate> aggregates = memberCommandRepository.bulkLoadByUserIdsInRoom(userIds, roomId);
            logger.debug(
                "Bulk loaded {} member aggregates for {} users in room: room_id={}",
                aggregates.size(),
                userIds.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading members by user IDs in room: room_id={}, user_count={}",
                roomId,
                userIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<MemberAggregate> bulkLoadActiveByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        try {
            List<MemberAggregate> aggregates = memberCommandRepository.bulkLoadActiveByUserIdsInRoom(userIds, roomId);
            logger.debug(
                "Bulk loaded {} active member aggregates for {} users in room: room_id={}",
                aggregates.size(),
                userIds.size(),
                roomId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active members by user IDs in room: room_id={}, user_count={}",
                roomId,
                userIds.size(),
                e
            );
            throw e;
        }
    }
}