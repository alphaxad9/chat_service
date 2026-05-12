// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/rooms/RoomEntity.java

package com.example.chat_service.infrastructure.persistence.rooms;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;  // Hibernate 6+ replacement for @Where

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Room persistence.
 * Maps the immutable domain Room model to database schema.
 * Uses soft-delete pattern with @SQLRestriction filter for automatic query filtering.
 * 
 * <p>Enforces group-name invariant at application layer (required for GROUP type).
 * All query methods automatically filter out deleted rooms via @SQLRestriction("is_deleted = false").</p>
 * 
 * <p>Indexes are optimized for CQRS read-side queries: creator lookups, type filtering,
 * activity-based ordering, and group name prefix searches. See RoomQueryRepository
 * and RoomCommandRepository for query patterns these indexes support.</p>
 * 
 * Note: Soft-delete is handled via repository methods, not @SQLDelete, for Hibernate 7.x compatibility.
 */
@Entity
@Table(
    name = "rooms",
    indexes = {
        // ── Primary Lookup Indexes ─────────────────────────────────────
        @Index(name = "idx_rooms_creator", columnList = "creator_id"),
        @Index(name = "idx_rooms_type", columnList = "type"),
        @Index(name = "idx_rooms_last_activity", columnList = "last_activity_at"),
        @Index(name = "idx_rooms_group_name", columnList = "group_name"),
        @Index(name = "idx_rooms_is_deleted", columnList = "is_deleted"),
        @Index(name = "idx_rooms_created", columnList = "created_at"),
        @Index(name = "idx_rooms_updated", columnList = "updated_at"),
        
        // ── Composite Indexes for Common Query Patterns ────────────────
        // Active rooms by creator (most frequent: "rooms I manage")
        @Index(name = "idx_rooms_creator_active", columnList = "creator_id, is_deleted"),
        // Active rooms by creator ordered by activity (dashboard feeds)
        @Index(name = "idx_rooms_creator_activity", columnList = "creator_id, is_deleted, last_activity_at"),
        // Active rooms by type (system-wide listings)
        @Index(name = "idx_rooms_type_active", columnList = "type, is_deleted"),
        // Active rooms by type ordered by activity (global feeds)
        @Index(name = "idx_rooms_type_activity", columnList = "type, is_deleted, last_activity_at"),
        // Group name prefix search on active rooms (discovery/search)
        @Index(name = "idx_rooms_name_active", columnList = "group_name, is_deleted"),
        // Bulk lookup: multiple creators, active only
        @Index(name = "idx_rooms_creators_active", columnList = "creator_id, is_deleted, type"),
        // Bulk lookup: multiple IDs with active filter
        @Index(name = "idx_rooms_ids_active", columnList = "id, is_deleted"),
        
        // ── Advanced Composite Indexes for Complex Queries ─────────────
        // Creator + type + active (prevent duplicate room creation checks)
        @Index(name = "idx_rooms_creator_type_active", columnList = "creator_id, type, is_deleted"),
        // Activity-based queries with type filter
        @Index(name = "idx_rooms_type_activity_time", columnList = "type, is_deleted, last_activity_at DESC"),
        // Creator activity feed with type discrimination
        @Index(name = "idx_rooms_creator_type_activity", columnList = "creator_id, type, is_deleted, last_activity_at DESC"),
        // Name prefix + type + active (group discovery with type filter)
        @Index(name = "idx_rooms_name_type_active", columnList = "group_name, type, is_deleted"),
        
        // ── Covering Indexes for Projection Queries (RoomSummary) ──────
        // Minimal fields for creator dashboard list rendering
        @Index(name = "idx_rooms_creator_summary", columnList = "creator_id, is_deleted, last_activity_at, type, group_name, profile_image_url"),
        // Minimal fields for global activity feed rendering
        @Index(name = "idx_rooms_activity_summary", columnList = "is_deleted, last_activity_at DESC, type, group_name, profile_image_url, creator_id"),
        // Minimal fields for group name search results
        @Index(name = "idx_rooms_name_summary", columnList = "group_name, is_deleted, type, last_activity_at, profile_image_url, creator_id"),
        
        // ── Direct Message Room Lookup Indexes ─────────────────────────
        // Query direct rooms by creator and friend (find conversation between two users)
        @Index(name = "idx_rooms_creator_friend", columnList = "creator_id, friend_id"),
        // Active direct rooms by creator and friend (most common lookup pattern)
        @Index(name = "idx_rooms_creator_friend_active", columnList = "creator_id, friend_id, is_deleted"),
        // Active direct rooms by creator, friend, and activity (for ordered feeds)
        @Index(name = "idx_rooms_creator_friend_activity", columnList = "creator_id, friend_id, is_deleted, last_activity_at DESC"),
        // Reverse lookup: find rooms where user is the friend participant
        @Index(name = "idx_rooms_friend_creator_active", columnList = "friend_id, creator_id, is_deleted")
    }
)
// Hibernate 6+: Auto-filter deleted rooms in queries (replaces @Where)
@SQLRestriction("is_deleted = false")
public class RoomEntity {

    @Id
    @Column(columnDefinition = "UUID", updatable = false)
    private UUID id;

    @Column(name = "creator_id", nullable = false, columnDefinition = "UUID")
    private UUID creatorId;

    @Column(name = "friend_id", columnDefinition = "UUID")
    private UUID friendId;  // Required only for DIRECT rooms, null for GROUP rooms

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoomType type;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    // ── JPA Required Default Constructor ─────────────────────────────
    protected RoomEntity() {
        // For JPA/Hibernate only
    }

    // ── Constructor for Domain Mapping ───────────────────────────────
    public RoomEntity(UUID id, UUID creatorId, UUID friendId, RoomType type,
                      String groupName, String description,
                      String coverImageUrl, String profileImageUrl,
                      LocalDateTime lastActivityAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt,
                      boolean isDeleted) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.creatorId = Objects.requireNonNull(creatorId, "creatorId cannot be null");
        this.friendId = friendId;  // null allowed for GROUP rooms
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.groupName = groupName;  // null allowed for DIRECT rooms
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.profileImageUrl = profileImageUrl;
        this.lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.isDeleted = isDeleted;
    }

    // ── Getters (JPA uses field access, but getters useful for mapping) ─
    public UUID getId() { return id; }
    public UUID getCreatorId() { return creatorId; }
    public UUID getFriendId() { return friendId; }
    public RoomType getType() { return type; }
    public String getGroupName() { return groupName; }
    public String getDescription() { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return isDeleted; }

    // ── Setters for JPA/Hibernate (package-private for controlled access) ─
    void setFriendId(UUID friendId) {
        this.friendId = friendId;
    }

    void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    void setDescription(String description) {
        this.description = description;
    }

    void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = Objects.requireNonNull(lastActivityAt, "lastActivityAt cannot be null");
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    /**
     * Mark entity as deleted (soft-delete).
     * Call this in repository before save() to persist deletion.
     */
    void markDeleted() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Restore entity from deleted state.
     */
    void restore() {
        this.isDeleted = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Domain Conversion Methods ────────────────────────────────────

    /**
     * Convert domain Room → JPA Entity
     */
    public static RoomEntity fromDomain(com.example.chat_service.domain.rooms.Room domain) {
        RoomEntity entity = new RoomEntity();
        entity.id = domain.id();
        entity.creatorId = domain.creatorId();
        entity.friendId = domain.friendId();
        entity.type = RoomType.fromDomain(domain.type());
        entity.groupName = domain.groupName();
        entity.description = domain.description();
        entity.coverImageUrl = domain.coverImageUrl();
        entity.profileImageUrl = domain.profileImageUrl();
        entity.lastActivityAt = domain.lastActivityAt();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.isDeleted = domain.isDeleted();
        return entity;
    }

    /**
     * Convert JPA Entity → domain Room
     * Package-private to restrict access to infrastructure layer only.
     */
    com.example.chat_service.domain.rooms.Room toDomain() {
        return new com.example.chat_service.domain.rooms.Room(
            this.id,
            this.creatorId,
            this.type.toDomain(),
            this.friendId,
            this.groupName,
            this.description,
            this.coverImageUrl,
            this.profileImageUrl,
            this.lastActivityAt,
            this.createdAt,
            this.updatedAt,
            this.isDeleted
        );
    }

    // ── Standard Object Methods ──────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RoomEntity{" +
                "id=" + id +
                ", creatorId=" + creatorId +
                ", friendId=" + friendId +
                ", type=" + type +
                ", groupName='" + groupName + '\'' +
                ", hasCoverImage=" + (coverImageUrl != null) +
                ", hasProfileImage=" + (profileImageUrl != null) +
                ", lastActivityAt=" + lastActivityAt +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                '}';
    }

    // ── Nested Enum for Persistence Layer ────────────────────────────
    /**
     * Persistence-layer enum mirroring domain Room.Type.
     * Provides conversion methods to maintain separation of concerns.
     */
    public enum RoomType {
        GROUP,
        DIRECT;

        /**
         * Convert domain Type → persistence RoomType
         */
        public static RoomType fromDomain(com.example.chat_service.domain.rooms.Room.Type domainType) {
            if (domainType == null) return null;
            return switch (domainType) {
                case GROUP -> GROUP;
                case DIRECT -> DIRECT;
            };
        }

        /**
         * Convert persistence RoomType → domain Type
         */
        public com.example.chat_service.domain.rooms.Room.Type toDomain() {
            return switch (this) {
                case GROUP -> com.example.chat_service.domain.rooms.Room.Type.GROUP;
                case DIRECT -> com.example.chat_service.domain.rooms.Room.Type.DIRECT;
            };
        }
    }
}