// chat_service/src/main/java/com/example/chat_service/application/rooms/services/impl/RoomQueryServiceImpl.java

package com.example.chat_service.application.rooms.services.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.rooms.services.RoomQueryServiceInterface;
import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.exceptions.RoomDomainError;
import com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError;
import com.example.chat_service.domain.rooms.repositories.RoomQueryRepository;

/**
 * Application-layer implementation of {@link RoomQueryServiceInterface}.
 *
 * <p>Orchestrates room query (read) operations by coordinating domain entities
 * with infrastructure query repositories. All methods run within a read-only
 * transaction boundary to optimize database access and ensure consistency.</p>
 *
 * <p><strong>Query pattern:</strong> All query methods accept IDs or filters as parameters,
 * delegate to {@link RoomQueryRepository}, apply read-side business logic if needed,
 * and return domain entities or projections. No state mutations occur.</p>
 *
 * <p><strong>CQRS read-side:</strong> This implementation focuses purely on read operations.
 * All queries automatically exclude rooms where {@code isDeleted = true} unless explicitly
 * documented, ensuring only active rooms are returned.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Query methods accept IDs/filters — service delegates to repository, applies read logic</li>
 *   <li>All queries exclude {@code isDeleted = true} rooms by default (active rooms only)</li>
 *   <li>Read-side projections and DTOs are handled via repository or mapped here</li>
 *   <li>No state mutations — this service is strictly for read operations</li>
 *   <li>All public methods are {@code @Transactional(readOnly = true)} for optimization</li>
 *   <li>Logging at DEBUG level for queries, WARN for not-found scenarios</li>
 * </ul></p>
 */
@Service
@Transactional(readOnly = true)
public class RoomQueryServiceImpl implements RoomQueryServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(RoomQueryServiceImpl.class);

    private final RoomQueryRepository roomQueryRepository;

    public RoomQueryServiceImpl(RoomQueryRepository roomQueryRepository) {
        this.roomQueryRepository = roomQueryRepository;
    }

    // ── Single Entity Queries (Active Rooms Only) ──────────────────

    @Override
    public Optional<Room> getRoomById(UUID roomId) {
        try {
            Optional<Room> result = roomQueryRepository.findById(roomId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved room by ID: room_id={}, creator_id={}, type={}, is_group={}, last_activity={}",
                    result.get().id(),
                    result.get().creatorId(),
                    result.get().type(),
                    result.get().isGroup(),
                    result.get().lastActivityAt()
                );
            } else {
                logger.debug("No active room found by ID: room_id={}", roomId);
            }
            
            return result;

        } catch (RoomNotFoundError e) {
            logger.debug("Room not found by ID: room_id={}", roomId);
            return Optional.empty();

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room by ID (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving room by ID: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public Optional<Room> getRoomByIdIncludingDeleted(UUID roomId) {
        try {
            Optional<Room> result = roomQueryRepository.findByIdIncludingDeleted(roomId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved room by ID (including deleted): room_id={}, creator_id={}, type={}, is_deleted={}",
                    result.get().id(),
                    result.get().creatorId(),
                    result.get().type(),
                    result.get().isDeleted()
                );
            } else {
                logger.debug("No room found by ID (including deleted): room_id={}", roomId);
            }
            
            return result;

        } catch (RoomNotFoundError e) {
            logger.debug("Room not found by ID (including deleted): room_id={}", roomId);
            return Optional.empty();

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room by ID including deleted (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving room by ID including deleted: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean isActiveRoom(UUID roomId) {
        try {
            boolean isActive = roomQueryRepository.isActiveRoom(roomId);
            logger.debug(
                "Active room check: room_id={}, is_active={}",
                roomId,
                isActive
            );
            return isActive;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error checking active room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Queries by Creator (Active Rooms Only) ───────────────────

    @Override
    public List<Room> getAllActiveRoomsByCreatorId(UUID creatorId) {
        try {
            List<Room> rooms = roomQueryRepository.findAllActiveByCreatorId(creatorId);
            logger.debug(
                "Retrieved {} active rooms for creator: creator_id={}",
                rooms.size(),
                creatorId
            );
            return rooms;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving active rooms by creator (creator_id={}): {}",
                creatorId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active rooms by creator: creator_id={}",
                creatorId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Room> getActiveGroupsByCreatorId(UUID creatorId) {
        try {
            List<Room> groups = roomQueryRepository.findActiveGroupsByCreatorId(creatorId);
            logger.debug(
                "Retrieved {} active GROUP rooms for creator: creator_id={}",
                groups.size(),
                creatorId
            );
            return groups;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving active groups by creator (creator_id={}): {}",
                creatorId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active groups by creator: creator_id={}",
                creatorId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Room> getActiveDirectsByCreatorId(UUID creatorId) {
        try {
            List<Room> directs = roomQueryRepository.findActiveDirectsByCreatorId(creatorId);
            logger.debug(
                "Retrieved {} active DIRECT rooms for creator: creator_id={}",
                directs.size(),
                creatorId
            );
            return directs;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving active directs by creator (creator_id={}): {}",
                creatorId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active directs by creator: creator_id={}",
                creatorId,
                e
            );
            throw e;
        }
    }

    @Override
    public long countActiveRoomsByCreatorId(UUID creatorId) {
        try {
            long count = roomQueryRepository.countActiveByCreatorId(creatorId);
            logger.debug(
                "Counted {} active rooms for creator: creator_id={}",
                count,
                creatorId
            );
            return count;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error counting active rooms by creator (creator_id={}): {}",
                creatorId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error counting active rooms by creator: creator_id={}",
                creatorId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Queries by Type (Active Rooms Only) ───────────────────

    @Override
    public List<Room> getAllActiveRoomsByType(Room.Type type) {
        try {
            List<Room> rooms = roomQueryRepository.findAllActiveByType(type);
            logger.debug(
                "Retrieved {} active rooms of type: type={}, count={}",
                rooms.size(),
                type,
                rooms.size()
            );
            return rooms;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving active rooms by type (type={}): {}",
                type,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active rooms by type: type={}",
                type,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Room> getActiveGroupsByNamePrefix(String groupNamePrefix, int limit) {
        try {
            List<Room> groups = roomQueryRepository.findActiveGroupsByNamePrefix(groupNamePrefix, limit);
            logger.debug(
                "Retrieved {} active GROUP rooms with name prefix: prefix='{}', limit={}",
                groups.size(),
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                limit
            );
            return groups;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving active groups by name prefix (prefix='{}', limit={}): {}",
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active groups by name prefix: prefix='{}', limit={}",
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public long countActiveRoomsByType(Room.Type type) {
        try {
            long count = roomQueryRepository.countActiveByType(type);
            logger.debug(
                "Counted {} active rooms of type: type={}",
                count,
                type
            );
            return count;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error counting active rooms by type (type={}): {}",
                type,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error counting active rooms by type: type={}",
                type,
                e
            );
            throw e;
        }
    }

    // ── Bulk Lookup Queries (Active Rooms Only) ───────────────────

    @Override
    public List<Room> getActiveRoomsByIds(Collection<UUID> roomIds) {
        try {
            List<Room> rooms = roomQueryRepository.findActiveByIds(roomIds);
            logger.debug(
                "Bulk retrieved {} active rooms for {} requested IDs",
                rooms.size(),
                roomIds.size()
            );
            return rooms;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error bulk retrieving rooms by IDs (requested_count={}): {}",
                roomIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving rooms by IDs: requested_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<Room> getActiveRoomsByCreatorIds(Collection<UUID> creatorIds) {
        try {
            List<Room> rooms = roomQueryRepository.findActiveByCreatorIds(creatorIds);
            logger.debug(
                "Bulk retrieved {} active rooms for {} creator IDs",
                rooms.size(),
                creatorIds.size()
            );
            return rooms;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error bulk retrieving rooms by creator IDs (creator_count={}): {}",
                creatorIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving rooms by creator IDs: creator_count={}",
                creatorIds.size(),
                e
            );
            throw e;
        }
    }

    // ── Activity-Based Queries (Active Rooms Only) ───────────────────

    @Override
    public List<Room> getActiveRoomsWithRecentActivity(java.time.LocalDateTime sinceTimestamp, int limit) {
        try {
            List<Room> rooms = roomQueryRepository.findActiveWithRecentActivity(sinceTimestamp, limit);
            logger.debug(
                "Retrieved {} active rooms with recent activity since {}: limit={}",
                rooms.size(),
                sinceTimestamp,
                limit
            );
            return rooms;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving rooms with recent activity (since={}, limit={}): {}",
                sinceTimestamp,
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving rooms with recent activity: since={}, limit={}",
                sinceTimestamp,
                limit,
                e
            );
            throw e;
        }
    }

    @Override
    public List<Room> getActiveRoomsOrderedByActivity(int limit) {
        try {
            List<Room> rooms = roomQueryRepository.findActiveOrderedByActivity(limit);
            logger.debug(
                "Retrieved {} active rooms ordered by activity: limit={}",
                rooms.size(),
                limit
            );
            return rooms;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving rooms ordered by activity (limit={}): {}",
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving rooms ordered by activity: limit={}",
                limit,
                e
            );
            throw e;
        }
    }

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    @Override
    public List<RoomSummary> getActiveRoomSummariesByCreatorId(UUID creatorId) {
        try {
            List<RoomQueryRepository.RoomSummary> repoSummaries = 
                roomQueryRepository.findActiveSummariesByCreatorId(creatorId);
            
            // Map repository projection to application-layer projection
            List<RoomSummary> summaries = repoSummaries.stream()
                .map(repo -> new RoomSummary(
                    repo.roomId(),
                    repo.creatorId(),
                    repo.type(),
                    repo.groupName(),
                    repo.description(),
                    repo.coverImageUrl(),
                    repo.profileImageUrl(),
                    repo.lastActivityAt(),
                    repo.createdAt()
                ))
                .toList();
            
            logger.debug(
                "Retrieved {} active room summaries for creator: creator_id={}",
                summaries.size(),
                creatorId
            );
            return summaries;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room summaries by creator (creator_id={}): {}",
                creatorId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving room summaries by creator: creator_id={}",
                creatorId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<RoomSummary> getActiveRoomSummariesByIds(Collection<UUID> roomIds) {
        try {
            List<RoomQueryRepository.RoomSummary> repoSummaries = 
                roomQueryRepository.findActiveSummariesByIds(roomIds);
            
            // Map repository projection to application-layer projection
            List<RoomSummary> summaries = repoSummaries.stream()
                .map(repo -> new RoomSummary(
                    repo.roomId(),
                    repo.creatorId(),
                    repo.type(),
                    repo.groupName(),
                    repo.description(),
                    repo.coverImageUrl(),
                    repo.profileImageUrl(),
                    repo.lastActivityAt(),
                    repo.createdAt()
                ))
                .toList();
            
            logger.debug(
                "Retrieved {} active room summaries for {} requested IDs",
                summaries.size(),
                roomIds.size()
            );
            return summaries;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room summaries by IDs (requested_count={}): {}",
                roomIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving room summaries by IDs: requested_count={}",
                roomIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<RoomSummary> getActiveGroupSummariesByNamePrefix(String groupNamePrefix, int limit) {
        try {
            List<RoomQueryRepository.RoomSummary> repoSummaries = 
                roomQueryRepository.findActiveGroupSummariesByNamePrefix(groupNamePrefix, limit);
            
            // Map repository projection to application-layer projection
            List<RoomSummary> summaries = repoSummaries.stream()
                .map(repo -> new RoomSummary(
                    repo.roomId(),
                    repo.creatorId(),
                    repo.type(),
                    repo.groupName(),
                    repo.description(),
                    repo.coverImageUrl(),
                    repo.profileImageUrl(),
                    repo.lastActivityAt(),
                    repo.createdAt()
                ))
                .toList();
            
            logger.debug(
                "Retrieved {} active GROUP room summaries with name prefix: prefix='{}', limit={}",
                summaries.size(),
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                limit
            );
            return summaries;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving GROUP summaries by name prefix (prefix='{}', limit={}): {}",
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                limit,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving GROUP summaries by name prefix: prefix='{}', limit={}",
                groupNamePrefix != null ? groupNamePrefix : "(all)",
                limit,
                e
            );
            throw e;
        }
    }

    // ── Read-Side Utility Queries ────────────────────────────────────

    @Override
    public boolean activeRoomExists(UUID roomId) {
        try {
            boolean exists = roomQueryRepository.findById(roomId).isPresent();
            logger.debug(
                "Active room existence check: room_id={}, exists={}",
                roomId,
                exists
            );
            return exists;

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error checking active room existence (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active room existence: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public Room.Type getRoomType(UUID roomId) {
        try {
            Optional<Room> room = roomQueryRepository.findById(roomId);
            
            if (room.isPresent()) {
                Room.Type type = room.get().type();
                logger.debug(
                    "Retrieved room type: room_id={}, type={}",
                    roomId,
                    type
                );
                return type;
            } else {
                logger.debug(
                    "No active room found for type check: room_id={}, returning null",
                    roomId
                );
                return null;
            }

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room type (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving room type: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public UUID getRoomCreatorId(UUID roomId) {
        try {
            Optional<Room> room = roomQueryRepository.findById(roomId);
            
            if (room.isPresent()) {
                UUID creatorId = room.get().creatorId();
                logger.debug(
                    "Retrieved room creator ID: room_id={}, creator_id={}",
                    roomId,
                    creatorId
                );
                return creatorId;
            } else {
                logger.debug(
                    "No active room found for creator check: room_id={}, returning null",
                    roomId
                );
                return null;
            }

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room creator ID (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving room creator ID: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public java.time.LocalDateTime getRoomLastActivityAt(UUID roomId) {
        try {
            Optional<Room> room = roomQueryRepository.findById(roomId);
            
            if (room.isPresent()) {
                java.time.LocalDateTime lastActivity = room.get().lastActivityAt();
                logger.debug(
                    "Retrieved room last activity: room_id={}, last_activity={}",
                    roomId,
                    lastActivity
                );
                return lastActivity;
            } else {
                logger.debug(
                    "No active room found for last activity check: room_id={}, returning null",
                    roomId
                );
                return null;
            }

        } catch (RoomDomainError e) {
            logger.warn(
                "Domain error retrieving room last activity (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving room last activity: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }
}