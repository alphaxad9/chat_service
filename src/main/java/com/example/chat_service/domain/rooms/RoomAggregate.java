// chat_service/src/main/java/com/example/chat_service/domain/rooms/RoomAggregate.java
package com.example.chat_service.domain.rooms;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ← Imports for exceptions in sub-package
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomEntityError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomGroupNameError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomDescriptionError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomImageError;
import com.example.chat_service.domain.rooms.exceptions.InvalidRoomCreatorError;
import com.example.chat_service.domain.rooms.exceptions.RoomOperationNotAllowedError;
import com.example.chat_service.domain.rooms.exceptions.RoomStateTransitionError;
import com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedError;
import com.example.chat_service.domain.rooms.exceptions.RoomUnauthorizedErrorWithNoId;
import static java.util.Objects.requireNonNull;

/**
 * Aggregate root for managing the lifecycle and state of a Room.
 * Enforces business rules, coordinates state transitions, guards operations,
 * and validates ownership for user-initiated actions (especially deletion).
 */
public final class RoomAggregate {

    private Room room; // Mutable reference to current state; Room itself is immutable

    private RoomAggregate(Room room) {
        this.room = requireNonNull(room, "room cannot be null");
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public Room room() {
        return room;
    }

    // ── Factory Methods ──────────────────────────────────────────────

    /**
     * Create an aggregate from an existing Room entity (e.g., loaded from repository).
     */
    public static RoomAggregate fromEntity(Room room) {
        return new RoomAggregate(room);
    }

    /**
     * Create a new GROUP room aggregate with validation.
     * Fails fast on invalid input before entity creation.
     */
    public static RoomAggregate createNewGroup(
            UUID id,
            UUID creatorId,
            String groupName,
            String description,
            String coverImageUrl,
            String profileImageUrl,
            LocalDateTime createdAt
    ) {
        // Validate required fields
        if (id == null) {
            throw new InvalidRoomEntityError(null, creatorId, Room.Type.GROUP.name(), "Room ID cannot be null");
        }
        if (creatorId == null) {
            throw new InvalidRoomEntityError(id, null, Room.Type.GROUP.name(), "Creator ID cannot be null");
        }
        if (groupName == null || groupName.isBlank()) {
            throw new InvalidRoomGroupNameError(id, groupName, groupName != null ? groupName.length() : null, "Group name is required");
        }
        if (groupName.length() > 100) {
            throw new InvalidRoomGroupNameError(id, groupName, groupName.length(), "Group name cannot exceed 100 characters");
        }
        if (description != null && description.length() > 500) {
            throw new InvalidRoomDescriptionError(id, description.length(), 500, "Description cannot exceed 500 characters");
        }
        if (coverImageUrl != null && coverImageUrl.isBlank()) {
            throw new InvalidRoomImageError(id, "cover", coverImageUrl, "Cover image URL cannot be blank if provided");
        }
        if (profileImageUrl != null && profileImageUrl.isBlank()) {
            throw new InvalidRoomImageError(id, "profile", profileImageUrl, "Profile image URL cannot be blank if provided");
        }
        Room newRoom = Room.createGroup(id, creatorId, groupName, description, coverImageUrl, profileImageUrl);
        return new RoomAggregate(newRoom);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static RoomAggregate createNewGroup(
            UUID id,
            UUID creatorId,
            String groupName,
            String description,
            String coverImageUrl,
            String profileImageUrl
    ) {
        return createNewGroup(id, creatorId, groupName, description, coverImageUrl, profileImageUrl, null);
    }

    /**
     * Create a new DIRECT message room aggregate.
     */
    public static RoomAggregate createNewDirect(
            UUID id,
            UUID creatorId,
            UUID friendId,
            LocalDateTime createdAt
    ) {
        if (id == null) {
            throw new InvalidRoomEntityError(null, creatorId, Room.Type.DIRECT.name(), "Room ID cannot be null");
        }
        if (creatorId == null) {
            throw new InvalidRoomEntityError(id, null, Room.Type.DIRECT.name(), "Creator ID cannot be null");
        }
        if (friendId == null) {
            throw new InvalidRoomEntityError(id, creatorId, Room.Type.DIRECT.name(), "friendId cannot be null for DIRECT room");
        }

        Room newRoom = Room.createDirect(id, creatorId, friendId);
        return new RoomAggregate(newRoom);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static RoomAggregate createNewDirect(UUID id, UUID creatorId, UUID friendId) {
        return createNewDirect(id, creatorId, friendId, null);
    }

    // ── Business Operations ──────────────────────────────────────────

    /**
     * Transfer room ownership to a new creator.
     * Application layer should verify caller has admin/ownership privileges before calling.
     * @param newCreatorId ID of the user receiving ownership
     * @param requesterId ID of the user requesting the transfer (must be current creator)
     */
    public RoomAggregate transferOwnership(UUID newCreatorId, UUID requesterId) {
        ensureActive("transfer_ownership");
        ensureCreator(requesterId, "transfer_ownership");
        
        if (newCreatorId == null) {
            throw new InvalidRoomCreatorError(
                room.id(),
                room.creatorId(),
                null,
                "New creator ID cannot be null"
            );
        }
        if (newCreatorId.equals(room.creatorId())) {
            // No-op: same creator
            return this;
        }
        
        this.room = room.withCreatorId(newCreatorId);
        return this;
    }

    /**
     * Update the last activity timestamp (call when new message/member action occurs).
     * System operation - no ownership check required.
     */
    public RoomAggregate updateLastActivity() {
        ensureActive("update_last_activity");
        this.room = room.updateLastActivity();
        return this;
    }

    /**
     * Update group name (only valid for GROUP rooms).
     * Application layer should verify caller has admin privileges before calling.
     */
    public RoomAggregate withGroupName(String newGroupName) {
        ensureActive("update_group_name");
        
        if (room.type() != Room.Type.GROUP) {
            throw new RoomOperationNotAllowedError(
                room.id(),
                "update_group_name",
                "groupName can only be updated for GROUP rooms, not " + room.type()
            );
        }
        if (newGroupName == null || newGroupName.isBlank()) {
            throw new InvalidRoomGroupNameError(
                room.id(),
                newGroupName,
                newGroupName != null ? newGroupName.length() : null,
                "Group name cannot be empty"
            );
        }
        if (newGroupName.length() > 100) {
            throw new InvalidRoomGroupNameError(
                room.id(),
                newGroupName,
                newGroupName.length(),
                "Group name cannot exceed 100 characters"
            );
        }
        
        this.room = room.withGroupName(newGroupName);
        return this;
    }

    /**
     * Update room description (only valid for GROUP rooms, optional, max 500 chars).
     * Application layer should verify caller has admin privileges before calling.
     */
    public RoomAggregate withDescription(String newDescription) {
        ensureActive("update_description");
        
        if (room.type() != Room.Type.GROUP) {
            throw new RoomOperationNotAllowedError(
                room.id(),
                "update_description",
                "description can only be updated for GROUP rooms, not " + room.type()
            );
        }
        if (newDescription != null && newDescription.length() > 500) {
            throw new InvalidRoomDescriptionError(
                room.id(),
                newDescription.length(),
                500,
                "Description cannot exceed 500 characters"
            );
        }
        
        this.room = room.withDescription(newDescription);
        return this;
    }

    /**
     * Update or set cover image URL / background image (only valid for GROUP rooms).
     * Pass null to remove the cover image.
     * Application layer should verify caller has admin privileges before calling.
     */
    public RoomAggregate withCoverImage(String newCoverImageUrl) {
        ensureActive("update_cover_image");
        
        if (room.type() != Room.Type.GROUP) {
            throw new RoomOperationNotAllowedError(
                room.id(),
                "update_cover_image",
                "coverImageUrl can only be updated for GROUP rooms, not " + room.type()
            );
        }
        if (newCoverImageUrl != null && newCoverImageUrl.isBlank()) {
            throw new InvalidRoomImageError(
                room.id(),
                "cover",
                newCoverImageUrl,
                "Cover image URL cannot be blank if provided"
            );
        }
        
        this.room = room.withCoverImage(newCoverImageUrl);
        return this;
    }

    /**
     * Update or set profile image URL (only valid for GROUP rooms).
     * Pass null to remove the profile image.
     * Application layer should verify caller has admin privileges before calling.
     */
    public RoomAggregate withProfileImage(String newProfileImageUrl) {
        ensureActive("update_profile_image");
        
        if (room.type() != Room.Type.GROUP) {
            throw new RoomOperationNotAllowedError(
                room.id(),
                "update_profile_image",
                "profileImageUrl can only be updated for GROUP rooms, not " + room.type()
            );
        }
        if (newProfileImageUrl != null && newProfileImageUrl.isBlank()) {
            throw new InvalidRoomImageError(
                room.id(),
                "profile",
                newProfileImageUrl,
                "Profile image URL cannot be blank if provided"
            );
        }
        
        this.room = room.withProfileImage(newProfileImageUrl);
        return this;
    }

    /**
     * Soft-delete the room.
     * REQUIRES: requesterId must match the room's creatorId.
     * @param requesterId ID of the user attempting to delete the room
     */
    public RoomAggregate delete(UUID requesterId) {
        ensureActive("delete");
        ensureCreator(requesterId, "delete");
        
        if (room.isDeleted()) {
            throw new RoomStateTransitionError(
                room.id(),
                "active",
                "deleted",
                "Room is already deleted"
            );
        }
        
        this.room = room.toggleDeletion();
        return this;
    }

    /**
     * Restore a soft-deleted room.
     * REQUIRES: requesterId must match the room's creatorId.
     * @param requesterId ID of the user attempting to restore the room
     */
    public RoomAggregate restore(UUID requesterId) {
        ensureInactive("restore");
        ensureCreator(requesterId, "restore");
        
        if (!room.isDeleted()) {
            throw new RoomStateTransitionError(
                room.id(),
                "deleted",
                "active",
                "Room is not deleted"
            );
        }
        
        this.room = room.toggleDeletion();
        return this;
    }

    /**
     * Update only the updatedAt timestamp (metadata refresh without state change).
     * Requires ownership for user-initiated touches.
     * @param requesterId ID of the user performing the touch
     */
    public RoomAggregate touch(UUID requesterId) {
        ensureActive("touch");
        ensureCreator(requesterId, "touch");
        
        this.room = room.touch();
        return this;
    }

    /**
     * Internal touch for system use (no ownership check).
     * Use sparingly - prefer explicit requesterId version for audit trails.
     */
    public RoomAggregate touchInternal() {
        ensureActive("touch_internal");
        this.room = room.touch();
        return this;
    }

    // ── State Queries (delegated to Room) ────────────────────────────

    public boolean isActive() {
        return room.isActive();
    }

    public boolean isGroup() {
        return room.isGroup();
    }

    public boolean isDirect() {
        return room.isDirect();
    }

    public boolean hasCoverImage() {
        return room.hasCoverImage();
    }

    public boolean hasProfileImage() {
        return room.hasProfileImage();
    }

    public boolean hasDescription() {
        return room.hasDescription();
    }

    public UUID id() { return room.id(); }
    public UUID creatorId() { return room.creatorId(); }
    public UUID friendId() { return room.friendId(); }
    public Room.Type type() { return room.type(); }
    public LocalDateTime lastActivityAt() { return room.lastActivityAt(); }
    public String groupName() { return room.groupName(); }
    public String description() { return room.description(); }
    public String coverImageUrl() { return room.coverImageUrl(); }
    public String profileImageUrl() { return room.profileImageUrl(); }
    public LocalDateTime createdAt() { return room.createdAt(); }
    public boolean isDeleted() { return room.isDeleted(); }

    // ── Helper Methods ───────────────────────────────────────────────

    /**
     * Verify that the requester is the creator/owner of this room.
     * Throws RoomUnauthorizedError if IDs don't match.
     * @param requesterId ID of the user attempting the operation
     * @param operation Name of the operation for error context
     */
    private void ensureCreator(UUID requesterId, String operation) {
        if (requesterId == null) {
            throw new RoomUnauthorizedErrorWithNoId(
                null,
                operation,
                "Requester ID cannot be null for creator check"
            );
        }
        if (!requesterId.equals(room.creatorId())) {
            throw new RoomUnauthorizedError(
                room.id(),
                requesterId,
                operation,
                "User " + requesterId + " is not the creator of room " + room.id() + " and cannot perform '" + operation + "'"
            );
        }
    }

    private void ensureActive(String operation) {
        if (!room.isActive()) {
            throw new RoomOperationNotAllowedError(
                room.id(),
                operation,
                "Room is deleted or inactive"
            );
        }
    }

    private void ensureInactive(String operation) {
        if (room.isActive()) {
            throw new RoomOperationNotAllowedError(
                room.id(),
                operation,
                "Room is active; this operation requires a deleted room"
            );
        }
    }

    // ── Standard Object Methods ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomAggregate that)) return false;
        return Objects.equals(room.id(), that.room.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(room.id());
    }

    @Override
    public String toString() {
        return "RoomAggregate{" +
                "id=" + room.id() +
                ", creatorId=" + room.creatorId() +
                ", friendId=" + room.friendId() +
                ", type=" + room.type() +
                ", isGroup=" + isGroup() +
                ", groupName='" + room.groupName() + '\'' +
                ", hasCoverImage=" + hasCoverImage() +
                ", hasProfileImage=" + hasProfileImage() +
                ", lastActivityAt=" + room.lastActivityAt() +
                ", isActive=" + isActive() +
                '}';
    }
}