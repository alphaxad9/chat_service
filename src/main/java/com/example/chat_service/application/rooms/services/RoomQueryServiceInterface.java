// chat_service/src/main/java/com/example/chat_service/application/rooms/services/RoomQueryServiceInterface.java

package com.example.chat_service.application.rooms.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.rooms.Room;

/**
 * Application-layer interface for room query (read) operations.
 *
 * <p>Orchestrates read-side business logic and coordinates domain entities with infrastructure
 * query repositories. All methods operate on {@link Room} entities (not aggregates) optimized
 * for read operations and CQRS patterns.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Query methods accept IDs or filters — service delegates to repository, applies read-side logic</li>
 *   <li>All queries automatically exclude rooms where {@code isDeleted = true} unless explicitly documented</li>
 *   <li>Read-side projections and DTOs are defined here or delegated to repository</li>
 *   <li>No state mutations — this interface is strictly for read operations</li>
 *   <li>Transaction boundaries (if any) should be read-only and applied at implementation level</li>
 * </ul></p>
 */
public interface RoomQueryServiceInterface {

    // ── Single Entity Queries (Active Rooms Only) ──────────────────

    /**
     * Retrieve an active room by its unique ID.
     *
     * @param roomId the unique identifier of the room
     * @return the room if found and active, otherwise {@link Optional#empty()}
     *
     * <p>Automatically filters out rooms where {@code isDeleted = true}.
     * Use {@code Optional} to handle "not found or deleted" without exceptions.
     * Suitable for: loading room details, permission checks, UI room displays.</p>
     */
    Optional<Room> getRoomById(UUID roomId);

    /**
     * Retrieve a room by its unique ID, including deleted rooms.
     *
     * @param roomId the unique identifier of the room
     * @return the room if found (regardless of deletion status), otherwise {@link Optional#empty()}
     *
     * <p>Use for admin/audit operations where deleted room details are needed.
     * Most application logic should prefer {@link #getRoomById(UUID)}.</p>
     */
    Optional<Room> getRoomByIdIncludingDeleted(UUID roomId);

    /**
     * Check if a room exists and is active.
     *
     * @param roomId the unique identifier of the room
     * @return {@code true} if room exists and is active, {@code false} otherwise
     *
     * <p>Optimized boolean check — does not load full entity.
     * Returns {@code false} if room doesn't exist OR is deleted.
     * Suitable for: quick permission gates, UI conditional rendering.</p>
     */
    boolean isActiveRoom(UUID roomId);

    // ── Bulk Queries by Creator (Active Rooms Only) ───────────────────

    /**
     * Retrieve all active rooms created by a specific user.
     *
     * @param creatorId the unique identifier of the creator
     * @return list of active {@link Room} entities created by this user
     *
     * <p>Excludes all rooms where {@code isDeleted = true}.
     * Results are ordered by {@code lastActivityAt} descending (most recent first).
     * Use for: creator dashboard, "rooms I manage" UI, engagement metrics.
     * Consider pagination overloads for creators with many rooms in production.</p>
     */
    List<Room> getAllActiveRoomsByCreatorId(UUID creatorId);

    /**
     * Retrieve all active GROUP rooms created by a specific user.
     *
     * @param creatorId the unique identifier of the creator
     * @return list of active GROUP {@link Room} entities
     *
     * <p>Filtered by {@code type = GROUP} and {@code isDeleted = false}.
     * Ordered by {@code lastActivityAt} descending.
     * Use for: group management UI, creator analytics.</p>
     */
    List<Room> getActiveGroupsByCreatorId(UUID creatorId);

    /**
     * Retrieve all active DIRECT rooms created by a specific user.
     *
     * @param creatorId the unique identifier of the creator
     * @return list of active DIRECT {@link Room} entities
     *
     * <p>Filtered by {@code type = DIRECT} and {@code isDeleted = false}.
     * Ordered by {@code lastActivityAt} descending.
     * Use for: direct message list, conversation history UI.</p>
     */
    List<Room> getActiveDirectsByCreatorId(UUID creatorId);

    /**
     * Count active rooms created by a user.
     *
     * @param creatorId the unique identifier of the creator
     * @return count of active rooms (excludes deleted)
     *
     * <p>Efficient count query — does not load full entities.
     * Use for: creator stats, UI badges, capacity checks.</p>
     */
    long countActiveRoomsByCreatorId(UUID creatorId);

    // ── Bulk Queries by Type (Active Rooms Only) ───────────────────

    /**
     * Retrieve all active rooms of a specific type.
     *
     * @param type the type of rooms to load (GROUP or DIRECT)
     * @return list of active {@link Room} entities of the given type
     *
     * <p>Excludes deleted rooms. Results ordered by {@code lastActivityAt} descending.
     * Use for: system-wide room listings, feature rollouts by type.
     * Consider pagination for large datasets in production.</p>
     */
    List<Room> getAllActiveRoomsByType(Room.Type type);

    /**
     * Retrieve active GROUP rooms with optional name prefix filtering.
     *
     * @param groupNamePrefix optional prefix to filter group names (case-insensitive), or null for all
     * @param limit maximum number of results to return (for pagination)
     * @return list of active GROUP {@link Room} entities matching the filter
     *
     * <p>Excludes deleted rooms. Results ordered by {@code lastActivityAt} descending.
     * Use for: group discovery, search functionality, admin listings.</p>
     */
    List<Room> getActiveGroupsByNamePrefix(String groupNamePrefix, int limit);

    /**
     * Count active rooms of a specific type.
     *
     * @param type the type of rooms to count (GROUP or DIRECT)
     * @return count of active rooms of the given type
     *
     * <p>Efficient count — does not load entities.
     * Use for: system stats, analytics dashboards.</p>
     */
    long countActiveRoomsByType(Room.Type type);

    // ── Bulk Lookup Queries (Active Rooms Only) ───────────────────

    /**
     * Bulk lookup active rooms by their IDs.
     *
     * @param roomIds collection of room identifiers to check
     * @return list of active {@link Room} entities for matching IDs
     *
     * <p>Returns only rooms that exist and are active.
     * Does not throw if some IDs are not found or deleted.
     * Order of results is not guaranteed — use a map if order matters.
     * Use for: batch operations, permission checks, presence updates.</p>
     */
    List<Room> getActiveRoomsByIds(Collection<UUID> roomIds);

    /**
     * Bulk lookup active rooms by creator IDs.
     *
     * @param creatorIds collection of creator identifiers to check
     * @return list of active {@link Room} entities created by matching users
     *
     * <p>Returns only active rooms whose creator is in the provided collection.
     * Ordered by {@code lastActivityAt} descending.
     * Use for: multi-creator dashboards, org-level room listings.</p>
     */
    List<Room> getActiveRoomsByCreatorIds(Collection<UUID> creatorIds);

    // ── Activity-Based Queries (Active Rooms Only) ───────────────────

    /**
     * Retrieve active rooms with recent activity.
     *
     * @param sinceTimestamp only return rooms with {@code lastActivityAt >= sinceTimestamp}
     * @param limit maximum number of results to return
     * @return list of recently active {@link Room} entities
     *
     * <p>Excludes deleted rooms. Results ordered by {@code lastActivityAt} descending.
     * Use for: "recent conversations" feed, push notification targeting,
     * activity-based caching strategies.</p>
     */
    List<Room> getActiveRoomsWithRecentActivity(LocalDateTime sinceTimestamp, int limit);

    /**
     * Retrieve active rooms ordered by last activity (global feed).
     *
     * @param limit maximum number of results to return
     * @return list of active {@link Room} entities ordered by activity
     *
     * <p>Excludes deleted rooms. Primary query for "all recent rooms" feed.
     * Consider adding cursor-based pagination for production use.</p>
     */
    List<Room> getActiveRoomsOrderedByActivity(int limit);

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    /**
     * Fetch minimal room info for active rooms by creator.
     *
     * @param creatorId the unique identifier of the creator
     * @return list of {@link RoomSummary} records (lightweight DTO)
     *
     * <p>Optimized for UI lists: only fetches essential display fields.
     * Avoids loading full entity state when only list rendering is needed.
     * Implementations may use database projections or cached read models.</p>
     */
    List<RoomSummary> getActiveRoomSummariesByCreatorId(UUID creatorId);

    /**
     * Fetch minimal room info for active rooms by IDs.
     *
     * @param roomIds collection of room identifiers
     * @return list of {@link RoomSummary} records for matching active rooms
     *
     * <p>Same optimization as {@link #getActiveRoomSummariesByCreatorId(UUID)}
     * but for arbitrary room ID lookups. Useful for batch UI rendering.</p>
     */
    List<RoomSummary> getActiveRoomSummariesByIds(Collection<UUID> roomIds);

    /**
     * Fetch minimal info for active GROUP rooms with name prefix.
     *
     * @param groupNamePrefix optional prefix to filter (case-insensitive), or null for all
     * @param limit maximum results to return
     * @return list of {@link RoomSummary} records for matching active GROUP rooms
     *
     * <p>Optimized for search/discovery UIs. Only fetches fields needed
     * for room cards/list items: id, name, images, activity timestamp.</p>
     */
    List<RoomSummary> getActiveGroupSummariesByNamePrefix(String groupNamePrefix, int limit);

    // ── Read-Side Utility Queries ────────────────────────────────────

    /**
     * Check if a room with the given ID exists and is active.
     *
     * @param roomId the UUID of the room
     * @return {@code true} if room exists and {@code isDeleted = false}
     *
     * <p>Fast existence check without loading full entity.
     * Use for: precondition checks, cache key validation.</p>
     */
    boolean activeRoomExists(UUID roomId);

    /**
     * Get the type of a room by its ID.
     *
     * @param roomId the UUID of the room
     * @return the room's {@link Room.Type} if active, otherwise {@code null}
     *
     * <p>Convenience method for quick type checks without loading full entity.
     * Returns {@code null} if room doesn't exist or is deleted.
     * Suitable for: permission gates, UI conditional rendering, routing logic.</p>
     */
    Room.Type getRoomType(UUID roomId);

    /**
     * Get the creator ID of a room by its ID.
     *
     * @param roomId the UUID of the room
     * @return the creator's UUID if room is active, otherwise {@code null}
     *
     * <p>Convenience method for ownership checks without loading full entity.
     * Returns {@code null} if room doesn't exist or is deleted.
     * Suitable for: permission validation, admin action authorization.</p>
     */
    UUID getRoomCreatorId(UUID roomId);

    /**
     * Get the last activity timestamp of a room.
     *
     * @param roomId the UUID of the room
     * @return the {@link LocalDateTime} of last activity if room is active, otherwise {@code null}
     *
     * <p>Convenience method for sorting/ordering without loading full entity.
     * Returns {@code null} if room doesn't exist or is deleted.
     * Suitable for: feed ordering, "recently active" indicators.</p>
     */
    LocalDateTime getRoomLastActivityAt(UUID roomId);

    // ── Nested DTO for Lightweight Projections ───────────────────────

    /**
     * Lightweight read model for room list displays.
     * Contains only fields needed for UI rendering or quick checks.
     *
     * <p>This record mirrors {@link com.example.chat_service.domain.rooms.repositories.RoomQueryRepository.RoomSummary}
     * to keep application-layer contracts explicit and decoupled from infrastructure.</p>
     */
    record RoomSummary(
        UUID roomId,
        UUID creatorId,
        Room.Type type,
        String groupName,           // null for DIRECT rooms
        String description,         // may be null
        String coverImageUrl,       // may be null
        String profileImageUrl,     // may be null
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt
    ) {}
}