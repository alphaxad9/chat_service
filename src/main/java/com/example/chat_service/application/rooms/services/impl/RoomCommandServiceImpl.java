// chat_service/src/main/java/com/example/chat_service/application/rooms/services/impl/RoomCommandServiceImpl.java

package com.example.chat_service.application.rooms.services.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.rooms.services.RoomCommandServiceInterface;
import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.RoomAggregate;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomCreatorError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomDescriptionError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomEntityError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomGroupNameError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomImageError;
import com.example.chat_service.domain.rooms.exceptions.RoomAlreadyExistsError;
import com.example.chat_service.domain.rooms.exceptions.RoomDomainError;
import com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError;
import com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError;
import com.example.chat_service.domain.rooms.exceptions.RoomStateTransitionError;
import com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedError;
import com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedErrorWithNoId;
import com.example.chat_service.domain.rooms.repositories.RoomCommandRepository;

/**
 * Application-layer implementation of {@link RoomCommandServiceInterface}.
 *
 * <p>Orchestrates room command (write) operations by coordinating domain aggregates
 * with infrastructure repositories. All methods run within a transaction boundary
 * to ensure consistency.</p>
 *
 * <p><strong>Command pattern:</strong> All command methods (except {@code createGroupRoom}
 * and {@code createDirectRoom}) accept IDs as parameters, load the aggregate via repository,
 * apply domain logic, then persist the updated state. This ensures a consistent load-act-save flow.</p>
 *
 * <p><strong>No event publishing:</strong> This implementation focuses purely on
 * command orchestration. Event emission (outbox, Kafka, etc.) should be added
 * in a separate layer or via domain events when the infrastructure is ready.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Command methods accept IDs — service loads aggregate, applies business logic, persists result</li>
 *   <li>Only {@code createGroupRoom} and {@code createDirectRoom} accept pre-built aggregates (for initial construction)</li>
 *   <li>Business rules and validation live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence) are delegated to {@link RoomCommandRepository}</li>
 *   <li>All public methods are {@code @Transactional} for atomicity</li>
 *   <li>Authorization checks (creator/admin) are enforced by domain aggregates</li>
 * </ul></p>
 */
@Service
@Transactional
public class RoomCommandServiceImpl implements RoomCommandServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(RoomCommandServiceImpl.class);

    private final RoomCommandRepository roomCommandRepository;

    public RoomCommandServiceImpl(RoomCommandRepository roomCommandRepository) {
        this.roomCommandRepository = roomCommandRepository;
    }

    // ── Core Lifecycle Commands ────────────────────────────────────────

    @Override
    public RoomAggregate createGroupRoom(RoomAggregate aggregate) {
        try {
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully created GROUP room (room_id={}, creator_id={}, group_name='{}')",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().groupName()
            );
            return aggregate;

        } catch (RoomAlreadyExistsError e) {
            logger.warn(
                "Room creation failed: room already exists (room_id={}, creator_id={}, type={})",
                e.getRoomId(),
                e.getCreatorId(),
                e.getRoomType()
            );
            throw e;

        } catch (InvalidRoomEntityError e) {
            logger.warn(
                "Room creation failed: invalid entity data (reason={}, room_id={}, creator_id={}, type={})",
                e.getReason(),
                e.getRoomId(),
                e.getCreatorId(),
                e.getRoomType()
            );
            throw e;

        } catch (InvalidRoomGroupNameError e) {
            logger.warn(
                "Room creation failed: invalid group name (reason={}, room_id={}, provided_name='{}', length={})",
                e.getReason(),
                e.getRoomId(),
                e.getProvidedName(),
                e.getProvidedLength()
            );
            throw e;

        } catch (InvalidRoomDescriptionError e) {
            logger.warn(
                "Room creation failed: invalid description (reason={}, room_id={}, length={}, max={})",
                e.getReason(),
                e.getRoomId(),
                e.getProvidedLength(),
                e.getMaxLength()
            );
            throw e;

        } catch (InvalidRoomImageError e) {
            logger.warn(
                "Room creation failed: invalid image URL (reason={}, room_id={}, image_type={}, url='{}')",
                e.getReason(),
                e.getRoomId(),
                e.getImageType(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Room creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating GROUP room (room_id={})",
                aggregate != null && aggregate.room() != null ? aggregate.room().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public RoomAggregate createDirectRoom(RoomAggregate aggregate) {
        try {
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully created DIRECT room (room_id={}, creator_id={}, type={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().type()
            );
            return aggregate;

        } catch (RoomAlreadyExistsError e) {
            logger.warn(
                "Room creation failed: room already exists (room_id={}, creator_id={}, type={})",
                e.getRoomId(),
                e.getCreatorId(),
                e.getRoomType()
            );
            throw e;

        } catch (InvalidRoomEntityError e) {
            logger.warn(
                "Room creation failed: invalid entity data (reason={}, room_id={}, creator_id={}, type={})",
                e.getReason(),
                e.getRoomId(),
                e.getCreatorId(),
                e.getRoomType()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Room creation domain error: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error creating DIRECT room (room_id={})",
                aggregate != null && aggregate.room() != null ? aggregate.room().id() : "unknown",
                e
            );
            throw e;
        }
    }

    @Override
    public RoomAggregate deleteRoom(UUID roomId, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.delete(requesterId);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully deleted room (room_id={}, creator_id={}, type={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().type()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for delete operation: room_id={}", roomId);
            throw e;

        } catch (RoomUnauthorizedError e) {
            logger.warn(
                "Delete operation unauthorized: room_id={}, actor_id={}, operation={}",
                e.getRoomId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Delete operation unauthorized (no room ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomStateTransitionError e) {
            logger.warn(
                "Delete operation failed: invalid state transition (room_id={}, current={}, target={}, reason={})",
                e.getRoomId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Delete operation not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Delete operation domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during delete operation: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public RoomAggregate restoreRoom(UUID roomId, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.restore(requesterId);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully restored room (room_id={}, creator_id={}, type={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().type()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for restore operation: room_id={}", roomId);
            throw e;

        } catch (RoomUnauthorizedError e) {
            logger.warn(
                "Restore operation unauthorized: room_id={}, actor_id={}, operation={}",
                e.getRoomId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Restore operation unauthorized (no room ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomStateTransitionError e) {
            logger.warn(
                "Restore operation failed: invalid state transition (room_id={}, current={}, target={}, reason={})",
                e.getRoomId(),
                e.getCurrentState(),
                e.getTargetState(),
                e.getReason()
            );
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Restore operation not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Restore operation domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during restore operation: room_id={}", roomId, e);
            throw e;
        }
    }

    // ── Ownership Management Commands ──────────────────────────────────

    @Override
    public RoomAggregate transferOwnership(UUID roomId, UUID newCreatorId, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.transferOwnership(newCreatorId, requesterId);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully transferred room ownership (room_id={}, old_creator_id={}, new_creator_id={})",
                aggregate.room().id(),
                requesterId,
                newCreatorId
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for transfer ownership operation: room_id={}", roomId);
            throw e;

        } catch (RoomUnauthorizedError e) {
            logger.warn(
                "Transfer ownership unauthorized: room_id={}, actor_id={}, operation={}",
                e.getRoomId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Transfer ownership unauthorized (no room ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (InvalidRoomCreatorError e) {
            logger.warn(
                "Transfer ownership failed: invalid creator (reason={}, room_id={}, current_creator={}, new_creator={})",
                e.getReason(),
                e.getRoomId(),
                e.getCurrentCreatorId(),
                e.getNewCreatorId()
            );
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Transfer ownership not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Transfer ownership domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during transfer ownership operation: room_id={}", roomId, e);
            throw e;
        }
    }

    // ── Group Metadata Update Commands (GROUP rooms only) ──────────────

    @Override
    public RoomAggregate updateGroupName(UUID roomId, String newGroupName, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.withGroupName(newGroupName);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated group name (room_id={}, creator_id={}, new_name='{}')",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().groupName()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for updateGroupName operation: room_id={}", roomId);
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Update group name not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (InvalidRoomGroupNameError e) {
            logger.warn(
                "Update group name failed: invalid name (reason={}, room_id={}, provided_name='{}', length={})",
                e.getReason(),
                e.getRoomId(),
                e.getProvidedName(),
                e.getProvidedLength()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Update group name domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateGroupName operation: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public RoomAggregate updateDescription(UUID roomId, String newDescription, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.withDescription(newDescription);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated room description (room_id={}, creator_id={}, has_description={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().hasDescription()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for updateDescription operation: room_id={}", roomId);
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Update description not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (InvalidRoomDescriptionError e) {
            logger.warn(
                "Update description failed: invalid description (reason={}, room_id={}, length={}, max={})",
                e.getReason(),
                e.getRoomId(),
                e.getProvidedLength(),
                e.getMaxLength()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Update description domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateDescription operation: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public RoomAggregate updateCoverImage(UUID roomId, String newCoverImageUrl, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.withCoverImage(newCoverImageUrl);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated cover image (room_id={}, creator_id={}, has_cover_image={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().hasCoverImage()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for updateCoverImage operation: room_id={}", roomId);
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Update cover image not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (InvalidRoomImageError e) {
            logger.warn(
                "Update cover image failed: invalid image URL (reason={}, room_id={}, image_type={}, url='{}')",
                e.getReason(),
                e.getRoomId(),
                e.getImageType(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Update cover image domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateCoverImage operation: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public RoomAggregate updateProfileImage(UUID roomId, String newProfileImageUrl, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.withProfileImage(newProfileImageUrl);
            roomCommandRepository.save(aggregate);

            logger.info(
                "Successfully updated profile image (room_id={}, creator_id={}, has_profile_image={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().hasProfileImage()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for updateProfileImage operation: room_id={}", roomId);
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Update profile image not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (InvalidRoomImageError e) {
            logger.warn(
                "Update profile image failed: invalid image URL (reason={}, room_id={}, image_type={}, url='{}')",
                e.getReason(),
                e.getRoomId(),
                e.getImageType(),
                e.getProvidedUrl()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Update profile image domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateProfileImage operation: room_id={}", roomId, e);
            throw e;
        }
    }

    // ── Activity & Utility Commands ────────────────────────────────────

    @Override
    public RoomAggregate updateLastActivity(UUID roomId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.updateLastActivity();
            roomCommandRepository.save(aggregate);

            logger.debug(
                "Successfully updated last activity (room_id={}, creator_id={}, last_activity_at={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().lastActivityAt()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for updateLastActivity operation: room_id={}", roomId);
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Update last activity not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Update last activity domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during updateLastActivity operation: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public RoomAggregate touch(UUID roomId, UUID requesterId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.touch(requesterId);
            roomCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched room (room_id={}, creator_id={}, updated_at={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().updatedAt()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for touch operation: room_id={}", roomId);
            throw e;

        } catch (RoomUnauthorizedError e) {
            logger.warn(
                "Touch unauthorized: room_id={}, actor_id={}, operation={}",
                e.getRoomId(),
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomUnauthorizedErrorWithNoId e) {
            logger.warn(
                "Touch unauthorized (no room ID): actor_id={}, operation={}",
                e.getActorId(),
                e.getOperation()
            );
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Touch not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Touch domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during touch operation: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public RoomAggregate touchInternal(UUID roomId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            aggregate.touchInternal();
            roomCommandRepository.save(aggregate);

            logger.debug(
                "Successfully touched room internally (room_id={}, creator_id={}, updated_at={})",
                aggregate.room().id(),
                aggregate.room().creatorId(),
                aggregate.room().updatedAt()
            );
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room not found for touchInternal operation: room_id={}", roomId);
            throw e;

        } catch (RoomOperationNotAllowedError e) {
            logger.warn(
                "Touch internal not allowed: room_id={}, operation={}, reason={}",
                e.getRoomId(),
                e.getOperation(),
                e.getReason()
            );
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Touch internal domain error (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error during touchInternal operation: room_id={}", roomId, e);
            throw e;
        }
    }

    // ── Query Support Methods (for command orchestration) ──────────────

    @Override
    public RoomAggregate loadAggregate(UUID roomId) {
        try {
            RoomAggregate aggregate = roomCommandRepository.load(roomId);
            logger.debug("Loaded room aggregate: room_id={}", roomId);
            return aggregate;

        } catch (RoomNotFoundError e) {
            logger.warn("Room aggregate not found: room_id={}", roomId);
            throw e;

        } catch (RoomDomainError e) {
            logger.warn("Domain error loading room aggregate (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading room aggregate: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public Optional<RoomAggregate> loadAggregateOptional(UUID roomId) {
        try {
            Optional<RoomAggregate> result = roomCommandRepository.loadOptional(roomId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Loaded room aggregate (optional): room_id={}, type={}, is_active={}",
                    roomId,
                    result.get().room().type(),
                    result.get().room().isActive()
                );
            } else {
                logger.debug("No room aggregate found (optional): room_id={}", roomId);
            }
            
            return result;

        } catch (RoomDomainError e) {
            logger.warn("Domain error loading room aggregate (optional) (room_id={}): {}", roomId, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error loading room aggregate (optional): room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public boolean aggregateExists(UUID roomId) {
        try {
            boolean exists = roomCommandRepository.exists(roomId);
            logger.debug("Existence check: room_id={}, exists={}", roomId, exists);
            return exists;

        } catch (Exception e) {
            logger.error("Unexpected error checking room existence: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public boolean aggregateExistsByCreatorAndType(UUID creatorId, Room.Type type) {
        try {
            boolean exists = roomCommandRepository.existsByCreatorAndType(creatorId, type);
            logger.debug(
                "Existence check by creator+type: creator_id={}, type={}, exists={}",
                creatorId,
                type,
                exists
            );
            return exists;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking room existence by creator+type: creator_id={}, type={}",
                creatorId,
                type,
                e
            );
            throw e;
        }
    }

    // ── Bulk Read Operations (for command orchestration & read models) ─

    @Override
    public List<RoomAggregate> bulkLoadByCreatorId(UUID creatorId) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadByCreatorId(creatorId);
            logger.debug(
                "Bulk loaded {} room aggregates for creator: creator_id={}",
                aggregates.size(),
                creatorId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading rooms by creator: creator_id={}", creatorId, e);
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveByCreatorId(UUID creatorId) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadActiveByCreatorId(creatorId);
            logger.debug(
                "Bulk loaded {} active room aggregates for creator: creator_id={}",
                aggregates.size(),
                creatorId
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active rooms by creator: creator_id={}", creatorId, e);
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadByType(Room.Type type) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadByType(type);
            logger.debug(
                "Bulk loaded {} room aggregates of type: type={}, count={}",
                aggregates.size(),
                type,
                aggregates.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading rooms by type: type={}", type, e);
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveByType(Room.Type type) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadActiveByType(type);
            logger.debug(
                "Bulk loaded {} active room aggregates of type: type={}, count={}",
                aggregates.size(),
                type,
                aggregates.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error("Unexpected error bulk loading active rooms by type: type={}", type, e);
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadByIds(Collection<UUID> roomIds) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadByIds(roomIds);
            logger.debug(
                "Bulk loaded {} room aggregates for {} requested IDs",
                aggregates.size(),
                roomIds.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading rooms by IDs: requested_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveByIds(Collection<UUID> roomIds) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadActiveByIds(roomIds);
            logger.debug(
                "Bulk loaded {} active room aggregates for {} requested IDs",
                aggregates.size(),
                roomIds.size()
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active rooms by IDs: requested_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadGroupsByNamePrefix(String groupNamePrefix) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadGroupsByNamePrefix(groupNamePrefix);
            logger.debug(
                "Bulk loaded {} GROUP room aggregates with name prefix: prefix='{}'",
                aggregates.size(),
                groupNamePrefix != null ? groupNamePrefix : "(all)"
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading GROUP rooms by name prefix: prefix='{}'",
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                e
            );
            throw e;
        }
    }

    @Override
    public List<RoomAggregate> bulkLoadActiveGroupsByNamePrefix(String groupNamePrefix) {
        try {
            List<RoomAggregate> aggregates = roomCommandRepository.bulkLoadActiveGroupsByNamePrefix(groupNamePrefix);
            logger.debug(
                "Bulk loaded {} active GROUP room aggregates with name prefix: prefix='{}'",
                aggregates.size(),
                groupNamePrefix != null ? groupNamePrefix : "(all)"
            );
            return aggregates;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk loading active GROUP rooms by name prefix: prefix='{}'",
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                e
            );
            throw e;
        }
    }
}