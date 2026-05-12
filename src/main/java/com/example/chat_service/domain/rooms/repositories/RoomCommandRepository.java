// chat_service/src/main/java/com/example/chat_service/domain/rooms/repositories/RoomCommandRepository.java
package com.example.chat_service.domain.rooms.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.RoomAggregate;
import com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError;

/**
 * Interface for write operations on room aggregates.
 *
 * <p>All methods operate on full {@link RoomAggregate} instances to preserve domain invariants.
 * Implementations are responsible for persistence, optimistic concurrency control (if used),
 * and ensuring aggregate state (including soft-delete flags) is durably stored.</p>
 *
 * <p>This is a domain-layer interface — infrastructure implementations (e.g., JPA, JDBC)
 * should reside in {@code infrastructure.persistence.rooms.repositories}.</p>
 */
public interface RoomCommandRepository {

    /**
     * Persist a room aggregate.
     *
     * <p>This method handles both creation and updates:
     * <ul>
     *   <li>If the room does not exist (new ID), it performs an INSERT.</li>
     *   <li>If it exists, it performs an UPDATE based on the room ID.</li>
     * </ul>
     *
     * <p>The aggregate must be fully validated before calling this method.
     * This includes ensuring group-name invariants for GROUP rooms, image URL formats,
     * description length limits, and that state transitions (e.g., deletion) are valid.</p>
     *
     * @param aggregate the room aggregate to persist
     */
    void save(RoomAggregate aggregate);

    /**
     * Load an existing room aggregate by its unique ID.
     *
     * @param roomId the unique identifier of the room
     * @return the loaded room aggregate
     * @throws RoomNotFoundError if no room exists with the given ID
     *
     * <p>Used before applying any update command (e.g., updateGroupName, delete, transferOwnership).</p>
     */
    RoomAggregate load(UUID roomId);

    /**
     * Load an existing room aggregate by its unique ID, returning optional.
     *
     * @param roomId the unique identifier of the room
     * @return the room aggregate if found, otherwise {@link Optional#empty()}
     *
     * <p>Use this when room existence is uncertain and you want to avoid
     * exception handling for the "not found" case.</p>
     */
    Optional<RoomAggregate> loadOptional(UUID roomId);

    /**
     * Check whether a room record exists for the given room ID.
     *
     * @param roomId the unique identifier of the room
     * @return {@code true} if a room with the given ID exists, {@code false} otherwise
     *
     * <p>Useful for fast validation before attempting to load or update.
     * Avoids loading full aggregate state when only existence matters.</p>
     */
    boolean exists(UUID roomId);

    /**
     * Check whether a room exists with the given creator ID and type.
     *
     * @param creatorId the unique identifier of the creator
     * @param type the type of room to check (GROUP or DIRECT)
     * @return {@code true} if a matching room exists, {@code false} otherwise
     *
     * <p>Useful for preventing duplicate room creation or validating join requests
     * without loading full aggregate state.</p>
     */
    boolean existsByCreatorAndType(UUID creatorId, Room.Type type);

    // ── Bulk Load Operations ───────────────────────────────────────────

    /**
     * Bulk load all room aggregates created by a specific user.
     *
     * @param creatorId the unique identifier of the creator
     * @return list of all room aggregates created by this user (including deleted rooms)
     *
     * <p>Use this when you need to iterate over all rooms owned by a user for operations like:
     * <ul>
     *   <li>Displaying a user's created rooms list</li>
     *   <li>Computing creator-level statistics</li>
     *   <li>Admin room management UI</li>
     *   <li>Handling creator account deletion or data export</li>
     * </ul>
     *
     * <p>Consider pagination for creators with many rooms in production implementations.</p>
     */
    List<RoomAggregate> bulkLoadByCreatorId(UUID creatorId);

    /**
     * Bulk load all active room aggregates created by a specific user.
     *
     * @param creatorId the unique identifier of the creator
     * @return list of active room aggregates (excludes soft-deleted rooms)
     *
     * <p>Optimized for common read patterns where only current/visible rooms matter.
     * More efficient than filtering {@link #bulkLoadByCreatorId(UUID)} results in memory.</p>
     */
    List<RoomAggregate> bulkLoadActiveByCreatorId(UUID creatorId);

    /**
     * Bulk load all room aggregates of a specific type.
     *
     * @param type the type of rooms to load (GROUP or DIRECT)
     * @return list of all room aggregates of the given type (including deleted rooms)
     *
     * <p>Use this for system-wide operations like:
     * <ul>
     *   <li>Migration scripts</li>
     *   <li>Analytics across room types</li>
     *   <li>Feature flag rollouts by room type</li>
     * </ul>
     *
     * <p>Consider pagination for large datasets in production implementations.</p>
     */
    List<RoomAggregate> bulkLoadByType(Room.Type type);

    /**
     * Bulk load all active room aggregates of a specific type.
     *
     * @param type the type of rooms to load (GROUP or DIRECT)
     * @return list of active room aggregates of the given type (excludes soft-deleted rooms)
     *
     * <p>Optimized for UI displays or business logic that only concerns active rooms.
     * More efficient than filtering {@link #bulkLoadByType(Room.Type)} results in memory.</p>
     */
    List<RoomAggregate> bulkLoadActiveByType(Room.Type type);

    /**
     * Bulk load room aggregates for multiple room IDs.
     *
     * @param roomIds collection of room identifiers to look up
     * @return list of room aggregates for matching IDs
     *
     * <p>Useful for batch operations like:
     * <ul>
     *   <li>Checking which of several rooms exist and loading their state</li>
     *   <li>Bulk room updates or deletions</li>
     *   <li>Permission checks for multiple rooms at once</li>
     * </ul>
     *
     * <p>Returns only found rooms — does not throw if some IDs are not found.</p>
     */
    List<RoomAggregate> bulkLoadByIds(Collection<UUID> roomIds);

    /**
     * Bulk load active room aggregates for multiple room IDs.
     *
     * @param roomIds collection of room identifiers to look up
     * @return list of active room aggregates for matching IDs (excludes soft-deleted)
     *
     * <p>Same as {@link #bulkLoadByIds(Collection)} but filters out deleted rooms.
     * Useful for messaging, presence, or UI features that only show active rooms.</p>
     */
    List<RoomAggregate> bulkLoadActiveByIds(Collection<UUID> roomIds);

    /**
     * Bulk load all GROUP room aggregates (with optional groupName filtering).
     *
     * @param groupNamePrefix optional prefix to filter group names (case-insensitive), or null for all
     * @return list of GROUP room aggregates matching the filter (including deleted)
     *
     * <p>Useful for admin search, discovery features, or analytics on group rooms.
     * Pass {@code null} to load all GROUP rooms regardless of name.</p>
     */
    List<RoomAggregate> bulkLoadGroupsByNamePrefix(String groupNamePrefix);

    /**
     * Bulk load active GROUP room aggregates (with optional groupName filtering).
     *
     * @param groupNamePrefix optional prefix to filter group names (case-insensitive), or null for all
     * @return list of active GROUP room aggregates matching the filter (excludes deleted)
     *
     * <p>Optimized for user-facing search/discovery where only visible groups matter.
     * More efficient than filtering {@link #bulkLoadGroupsByNamePrefix(String)} in memory.</p>
     */
    List<RoomAggregate> bulkLoadActiveGroupsByNamePrefix(String groupNamePrefix);
}