// chat_service/src/main/java/com/example/chat_service/application/rooms/services/RoomCommandServiceInterface.java

package com.example.chat_service.application.rooms.services;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.chat_service.domain.rooms.Room;
import com.example.chat_service.domain.rooms.RoomAggregate;

/**
 * Application-layer interface for room command (write) operations.
 *
 * <p>Orchestrates business logic and coordinates domain aggregates with infrastructure
 * repositories. All methods operate on {@link RoomAggregate} to preserve domain invariants.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Command methods accept IDs — service loads aggregate, applies business logic, persists result</li>
 *   <li>Only {@code createGroupRoom} and {@code createDirectRoom} accept pre-built aggregates (for initial construction)</li>
 *   <li>Validation and business rules live in the domain (aggregate), not here</li>
 *   <li>Infrastructure concerns (persistence, caching) are delegated to repositories</li>
 *   <li>Transaction boundaries should be applied at the implementation level</li>
 *   <li>Authorization checks (e.g., caller is creator/admin) should be performed before calling protected operations</li>
 * </ul></p>
 */
public interface RoomCommandServiceInterface {

    // ── Core Lifecycle Commands ────────────────────────────────────────

    /**
     * Create a new GROUP room aggregate and persist it.
     *
     * @param aggregate the validated room aggregate to create (must be GROUP type)
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomEntityError
     *         if required fields (id, creatorId, groupName) are null or invalid
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomAlreadyExistsError
     *         if a room with the given ID already exists
     */
    RoomAggregate createGroupRoom(RoomAggregate aggregate);

    /**
     * Create a new DIRECT message room aggregate and persist it.
     *
     * @param aggregate the validated room aggregate to create (must be DIRECT type)
     * @return the persisted aggregate (with assigned timestamps, etc.)
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomEntityError
     *         if required fields (id, creatorId, otherParticipantId) are null or invalid
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomAlreadyExistsError
     *         if a room with the given ID already exists
     */
    RoomAggregate createDirectRoom(RoomAggregate aggregate);

    /**
     * Soft-delete a room (creator-initiated action).
     *
     * <p>Service loads aggregate by ID, verifies ownership via requesterId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param roomId the UUID of the room to delete
     * @param requesterId ID of the user attempting to delete (must match room's creatorId)
     * @return the updated aggregate with {@code isDeleted = true}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedError
     *         if requesterId does not match the room's creatorId
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is already deleted or inactive
     */
    RoomAggregate deleteRoom(UUID roomId, UUID requesterId);

    /**
     * Restore a soft-deleted room (creator-initiated action).
     *
     * <p>Service loads aggregate by ID, verifies ownership via requesterId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param roomId the UUID of the room to restore
     * @param requesterId ID of the user attempting to restore (must match room's creatorId)
     * @return the updated aggregate with {@code isDeleted = false}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedError
     *         if requesterId does not match the room's creatorId
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomStateTransitionError
     *         if the room is not in a deleted state
     */
    RoomAggregate restoreRoom(UUID roomId, UUID requesterId);

    // ── Ownership Management Commands ──────────────────────────────────

    /**
     * Transfer room ownership to a new creator.
     *
     * <p>Service loads aggregate by ID, verifies current ownership via requesterId,
     * applies domain logic, and persists the updated state.
     * Authorization checks should be performed before calling.</p>
     *
     * @param roomId the UUID of the room to update
     * @param newCreatorId ID of the user receiving ownership
     * @param requesterId ID of the current creator requesting the transfer
     * @return the updated aggregate with refreshed {@code creatorId} and {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedError
     *         if requesterId does not match the room's creatorId
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomCreatorError
     *         if newCreatorId is null
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is inactive or deleted
     */
    RoomAggregate transferOwnership(UUID roomId, UUID newCreatorId, UUID requesterId);

    // ── Group Metadata Update Commands (GROUP rooms only) ──────────────

    /**
     * Update the group name for a GROUP room.
     *
     * <p>Service loads aggregate by ID, verifies caller privileges, applies domain logic,
     * and persists the updated state. Only valid for GROUP type rooms.</p>
     *
     * @param roomId the UUID of the room to update
     * @param newGroupName the new group name (1-100 chars, non-blank)
     * @param requesterId ID of the user performing the update (should have admin privileges)
     * @return the updated aggregate with refreshed {@code groupName} and {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is not a GROUP type, is inactive, or caller lacks privileges
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomGroupNameError
     *         if newGroupName is null, blank, or exceeds 100 characters
     */
    RoomAggregate updateGroupName(UUID roomId, String newGroupName, UUID requesterId);

    /**
     * Update the description for a GROUP room.
     *
     * <p>Service loads aggregate by ID, verifies caller privileges, applies domain logic,
     * and persists the updated state. Only valid for GROUP type rooms. Description is optional.</p>
     *
     * @param roomId the UUID of the room to update
     * @param newDescription the new description (max 500 chars) or null to clear
     * @param requesterId ID of the user performing the update (should have admin privileges)
     * @return the updated aggregate with refreshed {@code description} and {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is not a GROUP type, is inactive, or caller lacks privileges
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomDescriptionError
     *         if newDescription exceeds 500 characters
     */
    RoomAggregate updateDescription(UUID roomId, String newDescription, UUID requesterId);

    /**
     * Update the cover image URL for a GROUP room.
     *
     * <p>Service loads aggregate by ID, verifies caller privileges, applies domain logic,
     * and persists the updated state. Only valid for GROUP type rooms. Pass null to remove.</p>
     *
     * @param roomId the UUID of the room to update
     * @param newCoverImageUrl the new cover image URL/path or null to clear
     * @param requesterId ID of the user performing the update (should have admin privileges)
     * @return the updated aggregate with refreshed {@code coverImageUrl} and {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is not a GROUP type, is inactive, or caller lacks privileges
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomImageError
     *         if newCoverImageUrl is blank (but not null)
     */
    RoomAggregate updateCoverImage(UUID roomId, String newCoverImageUrl, UUID requesterId);

    /**
     * Update the profile image URL for a GROUP room.
     *
     * <p>Service loads aggregate by ID, verifies caller privileges, applies domain logic,
     * and persists the updated state. Only valid for GROUP type rooms. Pass null to remove.</p>
     *
     * @param roomId the UUID of the room to update
     * @param newProfileImageUrl the new profile image URL/path or null to clear
     * @param requesterId ID of the user performing the update (should have admin privileges)
     * @return the updated aggregate with refreshed {@code profileImageUrl} and {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is not a GROUP type, is inactive, or caller lacks privileges
     * @throws com.example.chat_service.domain.rooms.exceptions.InvalidRoomImageError
     *         if newProfileImageUrl is blank (but not null)
     */
    RoomAggregate updateProfileImage(UUID roomId, String newProfileImageUrl, UUID requesterId);

    // ── Activity & Utility Commands ────────────────────────────────────

    /**
     * Update the last activity timestamp for a room.
     *
     * <p>System operation typically called when a new message or member action occurs.
     * Does not require ownership verification. Service loads aggregate by ID,
     * applies domain logic, and persists the updated state.</p>
     *
     * @param roomId the UUID of the room to update
     * @return the updated aggregate with refreshed {@code lastActivityAt} and {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is inactive or deleted
     */
    RoomAggregate updateLastActivity(UUID roomId);

    /**
     * Touch the aggregate to refresh its {@code updatedAt} timestamp.
     *
     * <p>Useful for cache invalidation, presence tracking, or forcing persistence
     * without changing business-relevant state. Requires ownership for audit trails.
     * Service loads aggregate by ID, verifies ownership via requesterId, applies domain logic,
     * and persists the updated state.</p>
     *
     * @param roomId the UUID of the room to touch
     * @param requesterId ID of the user performing the touch (must match room's creatorId)
     * @return the updated aggregate with refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedError
     *         if requesterId does not match the room's creatorId
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is inactive or deleted
     */
    RoomAggregate touch(UUID roomId, UUID requesterId);

    /**
     * Internal touch for system use (no ownership check).
     *
     * <p>Use sparingly — prefer explicit requesterId version for audit trails.
     * Typically used by background jobs or system maintenance tasks.
     * Service loads aggregate by ID, applies domain logic, and persists the updated state.</p>
     *
     * @param roomId the UUID of the room to touch
     * @return the updated aggregate with refreshed {@code updatedAt}
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError
     *         if the room is inactive or deleted
     */
    RoomAggregate touchInternal(UUID roomId);

    // ── Query Support Methods (for command orchestration) ──────────────

    /**
     * Load a room aggregate by its unique ID for mutation.
     *
     * @param roomId the UUID of the room to load
     * @return the loaded aggregate ready for business operations
     * @throws com.example.chat_service.domain.rooms.exceptions.RoomNotFoundError
     *         if no room exists with the given ID
     */
    RoomAggregate loadAggregate(UUID roomId);

    /**
     * Load a room aggregate by its unique ID, returning optional.
     *
     * <p>Use this when room existence is uncertain and you want to avoid
     * exception handling for the "not found" case.</p>
     *
     * @param roomId the UUID of the room to load
     * @return {@link Optional} containing the aggregate if found, empty otherwise
     */
    Optional<RoomAggregate> loadAggregateOptional(UUID roomId);

    /**
     * Check if a room aggregate exists by ID (fast existence check).
     *
     * @param roomId the UUID of the room
     * @return {@code true} if a room exists with the given ID
     */
    boolean aggregateExists(UUID roomId);

    /**
     * Check if a room exists with the given creator ID and type.
     *
     * @param creatorId the UUID of the creator
     * @param type the type of room to check (GROUP or DIRECT)
     * @return {@code true} if a matching room exists
     *
     * <p>Useful for preventing duplicate room creation or validating join requests
     * without loading full aggregate state.</p>
     */
    boolean aggregateExistsByCreatorAndType(UUID creatorId, Room.Type type);

    // ── Bulk Read Operations (for command orchestration & read models) ─

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