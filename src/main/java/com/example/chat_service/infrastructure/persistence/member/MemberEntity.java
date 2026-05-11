// chat_service/src/main/java/com/example/chat_service/infrastructure/persistence/members/MemberEntity.java
package com.example.chat_service.infrastructure.persistence.members;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;  // Hibernate 6+ replacement for @Where

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Member persistence.
 * Maps the immutable domain Member model to database schema.
 * Uses soft-delete pattern with @SQLRestriction filter for automatic query filtering.
 * 
 * <p>Enforces unique constraint on (user_id, room_id) to guarantee one membership
 * record per user per room. All query methods automatically filter out left members
 * via @SQLRestriction("is_left = false").</p>
 * 
 * Note: Soft-delete is handled via repository methods, not @SQLDelete, for Hibernate 7.x compatibility.
 */
@Entity
@Table(
    name = "members",
    indexes = {
        // ── Primary Lookup Indexes ─────────────────────────────────────
        @Index(name = "idx_members_user_room", columnList = "user_id, room_id"),
        @Index(name = "idx_members_room", columnList = "room_id"),
        @Index(name = "idx_members_user", columnList = "user_id"),
        
        // ── Role-Based Query Indexes ───────────────────────────────────
        @Index(name = "idx_members_room_status", columnList = "room_id, status"),
        @Index(name = "idx_members_user_status", columnList = "user_id, status"),
        
        // ── Soft-Delete & Temporal Indexes ─────────────────────────────
        @Index(name = "idx_members_is_left", columnList = "is_left"),
        @Index(name = "idx_members_joined", columnList = "joined_at"),
        @Index(name = "idx_members_updated", columnList = "updated_at"),
        
        // ── Composite Indexes for Common Query Patterns ────────────────
        // Active members by room (most frequent query: room participant lists)
        @Index(name = "idx_members_room_active", columnList = "room_id, is_left"),
        // Active admins by room (moderator lookups)
        @Index(name = "idx_members_room_admin_active", columnList = "room_id, status, is_left"),
        // Active memberships by user (user's conversation list)
        @Index(name = "idx_members_user_active", columnList = "user_id, is_left"),
        // Active admin memberships by user ("rooms I manage")
        @Index(name = "idx_members_user_admin_active", columnList = "user_id, status, is_left"),
        // Bulk lookup: multiple users in a room
        @Index(name = "idx_members_room_users", columnList = "room_id, user_id, is_left")
    },
    // Enforce: one membership record per user per room (including left members)
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_members_user_room",
            columnNames = {"user_id", "room_id"}
        )
    }
)
// Hibernate 6+: Auto-filter left members in queries (replaces @Where)
@SQLRestriction("is_left = false")
public class MemberEntity {

    @Id
    @Column(columnDefinition = "UUID", updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "room_id", nullable = false, columnDefinition = "UUID")
    private UUID roomId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Column(name = "unread_messages", nullable = false)
    private int unreadMessages;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_left", nullable = false)
    private boolean isLeft = false;

    // ── JPA Required Default Constructor ─────────────────────────────
    protected MemberEntity() {
        // For JPA/Hibernate only
    }

    // ── Constructor for Domain Mapping ───────────────────────────────
    public MemberEntity(UUID id, UUID userId, UUID roomId, MemberStatus status,
                        int unreadMessages, LocalDateTime joinedAt,
                        LocalDateTime updatedAt, boolean isLeft) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.roomId = Objects.requireNonNull(roomId, "roomId cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        if (unreadMessages < 0) {
            throw new IllegalArgumentException("unreadMessages cannot be negative");
        }
        this.unreadMessages = unreadMessages;
        this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.isLeft = isLeft;
    }

    // ── Getters (JPA uses field access, but getters useful for mapping) ─
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getRoomId() { return roomId; }
    public MemberStatus getStatus() { return status; }
    public int getUnreadMessages() { return unreadMessages; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isLeft() { return isLeft; }

    // ── Setters for JPA/Hibernate (package-private for controlled access) ─
    void setStatus(MemberStatus status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    void setUnreadMessages(int unreadMessages) {
        if (unreadMessages < 0) {
            throw new IllegalArgumentException("unreadMessages cannot be negative");
        }
        this.unreadMessages = unreadMessages;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    /**
     * Mark entity as left (soft-delete for membership).
     * Call this in repository before save() to persist the departure.
     */
    void markLeft() {
        this.isLeft = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Restore entity from left state (e.g., re-join).
     */
    void restore() {
        this.isLeft = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ── Domain Conversion Methods ────────────────────────────────────

    /**
     * Convert domain Member → JPA Entity
     */
    public static MemberEntity fromDomain(com.example.chat_service.domain.members.Member domain) {
        MemberEntity entity = new MemberEntity();
        entity.id = domain.id();
        entity.userId = domain.userId();
        entity.roomId = domain.roomId();
        entity.status = MemberStatus.fromDomain(domain.status());
        entity.unreadMessages = domain.unreadMessages();
        entity.joinedAt = domain.joinedAt();
        entity.updatedAt = domain.updatedAt();
        entity.isLeft = domain.isLeft();
        return entity;
    }

    /**
     * Convert JPA Entity → domain Member
     * Package-private to restrict access to infrastructure layer only.
     */
    com.example.chat_service.domain.members.Member toDomain() {
        return new com.example.chat_service.domain.members.Member(
            this.id,
            this.userId,
            this.roomId,
            this.status.toDomain(),
            this.unreadMessages,
            this.joinedAt,
            this.updatedAt,
            this.isLeft
        );
    }

    // ── Standard Object Methods ──────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MemberEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", roomId=" + roomId +
                ", status=" + status +
                ", unreadMessages=" + unreadMessages +
                ", isLeft=" + isLeft +
                ", joinedAt=" + joinedAt +
                '}';
    }

    // ── Nested Enum for Persistence Layer ────────────────────────────
    /**
     * Persistence-layer enum mirroring domain Member.Status.
     * Provides conversion methods to maintain separation of concerns.
     */
    public enum MemberStatus {
        USER,
        ADMIN;

        /**
         * Convert domain Status → persistence MemberStatus
         */
        public static MemberStatus fromDomain(com.example.chat_service.domain.members.Member.Status domainStatus) {
            if (domainStatus == null) return null;
            return switch (domainStatus) {
                case USER -> USER;
                case ADMIN -> ADMIN;
            };
        }

        /**
         * Convert persistence MemberStatus → domain Status
         */
        public com.example.chat_service.domain.members.Member.Status toDomain() {
            return switch (this) {
                case USER -> com.example.chat_service.domain.members.Member.Status.USER;
                case ADMIN -> com.example.chat_service.domain.members.Member.Status.ADMIN;
            };
        }
    }
}