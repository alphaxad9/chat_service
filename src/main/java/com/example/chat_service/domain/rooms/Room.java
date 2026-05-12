// chat_service/src/main/java/com/example/chat_service/domain/rooms/Room.java
package com.example.chat_service.domain.rooms;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing a Room aggregate.
 * Manages room configuration, group metadata, images, and activity tracking.
 * 
 * <p>Constructor is public to allow infrastructure mapping from persistence layer.
 * Validation is enforced in constructor, so instantiation is always safe.</p>
 */
public final class Room {

    private final UUID id;                    // Room ID (primary key)
    private final UUID creatorId;             // Reference to room creator (transferable)
    private final Type type;                  // Room type: GROUP or DIRECT
    private final UUID friendId;              // Reference to other participant (required only for DIRECT rooms)
    private final LocalDateTime lastActivityAt; // Last interaction timestamp for ordering
    
    // Optional images (nullable) - used for display in UI
    private final String coverImageUrl;       // URL/path to cover image (nullable)
    private final String profileImageUrl;     // URL/path to profile/avatar image (nullable)
    
    // Group-specific fields
    private final String groupName;           // Required for GROUP type, null for DIRECT
    private final String description;         // Optional description (max 500 chars)
    
    // Metadata
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final boolean isDeleted;          // Soft delete flag

    // ── Type Enum ─────────────────────────────────────────────────────
    public enum Type {
        GROUP,
        DIRECT
    }

    // ── Constructor with validation ──────────────────────────────────
    /**
     * Public constructor for domain creation and infrastructure mapping.
     * All arguments are validated to ensure domain invariants.
     */
    public Room(UUID id, UUID creatorId, Type type, UUID friendId,
                String groupName, String description,
                String coverImageUrl, String profileImageUrl,
                LocalDateTime lastActivityAt,
                LocalDateTime createdAt, LocalDateTime updatedAt, boolean isDeleted) {
        
        // Validate required fields
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (creatorId == null) throw new IllegalArgumentException("creatorId cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (lastActivityAt == null) throw new IllegalArgumentException("lastActivityAt cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");

        // DIRECT room invariant: friendId is required for DIRECT rooms
        if (type == Type.DIRECT) {
            if (friendId == null) {
                throw new IllegalArgumentException("friendId is required for DIRECT rooms");
            }
        } else {
            // GROUP rooms should not have friendId set
            if (friendId != null) {
                throw new IllegalArgumentException("friendId should be null for GROUP rooms");
            }
        }

        // Group-specific invariant: groupName is required and non-empty for GROUP rooms
        if (type == Type.GROUP) {
            if (groupName == null || groupName.isBlank()) {
                throw new IllegalArgumentException("groupName is required for GROUP rooms");
            }
            if (groupName.length() > 100) {
                throw new IllegalArgumentException("groupName cannot exceed 100 characters");
            }
        }

        // Description validation (if provided)
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("description cannot exceed 500 characters");
        }

        // Image URL validation (if provided, must not be blank)
        if (coverImageUrl != null && coverImageUrl.isBlank()) {
            throw new IllegalArgumentException("coverImageUrl cannot be blank if provided");
        }
        if (profileImageUrl != null && profileImageUrl.isBlank()) {
            throw new IllegalArgumentException("profileImageUrl cannot be blank if provided");
        }

        this.id = id;
        this.creatorId = creatorId;
        this.type = type;
        this.friendId = friendId;
        this.groupName = groupName;  // null allowed for DIRECT rooms
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.profileImageUrl = profileImageUrl;
        this.lastActivityAt = lastActivityAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    // ── Factory Methods ─────────────────────────────────────────────
    
    /**
     * Create a new GROUP room.
     * @param groupName required name for the group (max 100 chars)
     * @param description optional description (max 500 chars)
     * @param coverImageUrl optional cover image URL
     * @param profileImageUrl optional profile/avatar image URL
     */
    public static Room createGroup(UUID id, UUID creatorId, String groupName, String description,
                                   String coverImageUrl, String profileImageUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new Room(id, creatorId, Type.GROUP, null,
                       groupName, description, coverImageUrl, profileImageUrl, 
                       now, now, now, false);
    }

    /**
     * Create a new DIRECT message room (no group metadata, no images).
     */
    public static Room createDirect(UUID id, UUID creatorId, UUID friendId) {
        LocalDateTime now = LocalDateTime.now();
        String metaDescription = "Direct conversation";
        return new Room(id, creatorId, Type.DIRECT, friendId,
                       null, metaDescription, null, null, now, now, now, false);
    }

    // ── Getters (no setters - immutable) ───────────────────────────
    public UUID id() { return id; }
    public UUID creatorId() { return creatorId; }
    public Type type() { return type; }
    public UUID friendId() { return friendId; }
    public LocalDateTime lastActivityAt() { return lastActivityAt; }
    public String coverImageUrl() { return coverImageUrl; }
    public String profileImageUrl() { return profileImageUrl; }
    public String groupName() { return groupName; }
    public String description() { return description; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public boolean isDeleted() { return isDeleted; }

    // ── State Queries ──────────────────────────────────────────────
    public boolean isActive() {
        return !isDeleted;
    }

    public boolean isGroup() {
        return type == Type.GROUP;
    }

    public boolean isDirect() {
        return type == Type.DIRECT;
    }

    public boolean hasCoverImage() {
        return coverImageUrl != null && !coverImageUrl.isBlank();
    }

    public boolean hasProfileImage() {
        return profileImageUrl != null && !profileImageUrl.isBlank();
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    // ── State Transformers (return new instance) ───────────────────
    
    /**
     * Transfer room ownership to a new creator.
     * Use when original creator account is deleted or ownership is reassigned.
     */
    public Room withCreatorId(UUID newCreatorId) {
        if (newCreatorId == null) {
            throw new IllegalArgumentException("newCreatorId cannot be null");
        }
        if (Objects.equals(this.creatorId, newCreatorId)) {
            return this; // No change needed
        }
        return new Room(this.id, newCreatorId, this.type, this.friendId,
                       this.groupName, this.description,
                       this.coverImageUrl, this.profileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Update the last activity timestamp (call when new message/member action occurs).
     */
    public Room updateLastActivity() {
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       this.groupName, this.description,
                       this.coverImageUrl, this.profileImageUrl,
                       LocalDateTime.now(), this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    // ── Group Metadata & Image Transformers (GROUP rooms only) ─────────

    /**
     * Update group name (only valid for GROUP rooms).
     * @param newGroupName the new name (required, 1-100 chars, non-blank)
     * @return new Room instance with updated name and timestamp
     * @throws IllegalStateException if called on a DIRECT room
     * @throws IllegalArgumentException if name is invalid
     */
    public Room withGroupName(String newGroupName) {
        if (type != Type.GROUP) {
            throw new IllegalStateException("groupName can only be updated for GROUP rooms");
        }
        if (newGroupName == null || newGroupName.isBlank()) {
            throw new IllegalArgumentException("groupName cannot be empty");
        }
        if (newGroupName.length() > 100) {
            throw new IllegalArgumentException("groupName cannot exceed 100 characters");
        }
        if (Objects.equals(this.groupName, newGroupName)) {
            return this; // No change needed
        }
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       newGroupName, this.description,
                       this.coverImageUrl, this.profileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Update room description (only valid for GROUP rooms).
     * @param newDescription the new description (optional, max 500 chars) or null to clear
     * @return new Room instance with updated description and timestamp
     * @throws IllegalStateException if called on a DIRECT room
     * @throws IllegalArgumentException if description exceeds 500 chars
     */
    public Room withDescription(String newDescription) {
        if (type != Type.GROUP) {
            throw new IllegalStateException("description can only be updated for GROUP rooms");
        }
        if (newDescription != null && newDescription.length() > 500) {
            throw new IllegalArgumentException("description cannot exceed 500 characters");
        }
        if (Objects.equals(this.description, newDescription)) {
            return this; // No change needed
        }
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       this.groupName, newDescription,
                       this.coverImageUrl, this.profileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Update or set profile image URL (only valid for GROUP rooms).
     * Pass null to remove the profile image.
     * @param newProfileImageUrl the new image URL/path or null to clear
     * @return new Room instance with updated profile image and timestamp
     * @throws IllegalStateException if called on a DIRECT room
     * @throws IllegalArgumentException if URL is blank (but not null)
     */
    public Room withProfileImage(String newProfileImageUrl) {
        if (type != Type.GROUP) {
            throw new IllegalStateException("profileImageUrl can only be updated for GROUP rooms");
        }
        if (newProfileImageUrl != null && newProfileImageUrl.isBlank()) {
            throw new IllegalArgumentException("profileImageUrl cannot be blank if provided");
        }
        if (Objects.equals(this.profileImageUrl, newProfileImageUrl)) {
            return this; // No change needed
        }
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       this.groupName, this.description,
                       this.coverImageUrl, newProfileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Update or set cover image URL / background image (only valid for GROUP rooms).
     * Pass null to remove the cover image.
     * @param newCoverImageUrl the new image URL/path or null to clear
     * @return new Room instance with updated cover image and timestamp
     * @throws IllegalStateException if called on a DIRECT room
     * @throws IllegalArgumentException if URL is blank (but not null)
     */
    public Room withCoverImage(String newCoverImageUrl) {
        if (type != Type.GROUP) {
            throw new IllegalStateException("coverImageUrl can only be updated for GROUP rooms");
        }
        if (newCoverImageUrl != null && newCoverImageUrl.isBlank()) {
            throw new IllegalArgumentException("coverImageUrl cannot be blank if provided");
        }
        if (Objects.equals(this.coverImageUrl, newCoverImageUrl)) {
            return this; // No change needed
        }
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       this.groupName, this.description,
                       newCoverImageUrl, this.profileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    /**
     * Soft-delete or restore the room.
     */
    public Room toggleDeletion() {
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       this.groupName, this.description,
                       this.coverImageUrl, this.profileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), !this.isDeleted);
    }

    /**
     * Update only the updatedAt timestamp (metadata refresh without state change).
     */
    public Room touch() {
        return new Room(this.id, this.creatorId, this.type, this.friendId,
                       this.groupName, this.description,
                       this.coverImageUrl, this.profileImageUrl,
                       this.lastActivityAt, this.createdAt, LocalDateTime.now(), this.isDeleted);
    }

    // ── Standard Object Methods ────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", creatorId=" + creatorId +
                ", type=" + type +
                ", friendId=" + friendId +
                ", isGroup=" + isGroup() +
                ", groupName='" + groupName + '\'' +
                ", hasCoverImage=" + hasCoverImage() +
                ", hasProfileImage=" + hasProfileImage() +
                ", lastActivityAt=" + lastActivityAt +
                ", isActive=" + isActive() +
                '}';
    }
}