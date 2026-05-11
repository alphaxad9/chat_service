// chat_service/src/main/java/com/example/chat_service/domain/members/Member.java
package com.example.chat_service.domain.members;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing a Group Member aggregate.
 * Manages membership state, role status, and unread message tracking for a user in a room.
 * 
 * <p>Constructor is public to allow infrastructure mapping from persistence layer.
 * Validation is enforced in constructor, so instantiation is always safe.</p>
 */
public final class Member {

    private final UUID id;           // Group member ID (primary key)
    private final UUID userId;       // Reference to the user
    private final UUID roomId;       // Reference to the room/group
    private final Status status;     // Member role: ADMIN or USER
    private final int unreadMessages; // Count of unread messages
    
    // Metadata
    private final LocalDateTime joinedAt;
    private final LocalDateTime updatedAt;
    private final boolean isLeft;    // Soft delete flag for members who left

    // ── Status Enum ────────────────────────────────────────────────
    public enum Status {
        USER,
        ADMIN
    }

    // ── Constructor with validation ──────────────────────────────────
    /**
     * Public constructor for domain creation and infrastructure mapping.
     * All arguments are validated to ensure domain invariants.
     */
    public Member(UUID id, UUID userId, UUID roomId, Status status, int unreadMessages,
                  LocalDateTime joinedAt, LocalDateTime updatedAt, boolean isLeft) {
        
        // Validate required fields
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        if (roomId == null) throw new IllegalArgumentException("roomId cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (unreadMessages < 0) throw new IllegalArgumentException("unreadMessages cannot be negative");
        if (joinedAt == null) throw new IllegalArgumentException("joinedAt cannot be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt cannot be null");

        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.status = status;
        this.unreadMessages = unreadMessages;
        this.joinedAt = joinedAt;
        this.updatedAt = updatedAt;
        this.isLeft = isLeft;
    }

    // ── Factory Methods ─────────────────────────────────────────────
    
    /**
     * Create a new Member with explicit status.
     * Use this when the initial role matters (e.g., group creator becomes ADMIN).
     */
    public static Member create(UUID id, UUID userId, UUID roomId, Status initialStatus) {
        if (initialStatus == null) {
            throw new IllegalArgumentException("initialStatus cannot be null");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Member(id, userId, roomId, initialStatus, 0, now, now, false);
    }

    /**
     * Create a new Member with USER status (convenience method).
     * Use for inviting regular participants to a room.
     */
    public static Member createAsUser(UUID id, UUID userId, UUID roomId) {
        return create(id, userId, roomId, Status.USER);
    }

    /**
     * Create a new Member with ADMIN status (convenience method).
     * Use for the room creator or when explicitly assigning admin rights at creation.
     */
    public static Member createAsAdmin(UUID id, UUID userId, UUID roomId) {
        return create(id, userId, roomId, Status.ADMIN);
    }

    // ── Getters (no setters - immutable) ───────────────────────────
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID roomId() { return roomId; }
    public Status status() { return status; }
    public int unreadMessages() { return unreadMessages; }
    public LocalDateTime joinedAt() { return joinedAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public boolean isLeft() { return isLeft; }

    // ── State Queries ──────────────────────────────────────────────
    public boolean isActive() {
        return !isLeft;
    }

    public boolean isAdmin() {
        return status == Status.ADMIN;
    }

    public boolean hasUnreadMessages() {
        return unreadMessages > 0;
    }

    // ── Role Management (return new instance) ──────────────────────
    /**
     * Promote this member to ADMIN status.
     * @return new Member instance with updated status and timestamp
     */
    public Member promote() {
        if (isAdmin()) {
            return this; // Already admin, no change needed
        }
        return new Member(this.id, this.userId, this.roomId, Status.ADMIN, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    /**
     * Demote this member to USER status.
     * @return new Member instance with updated status and timestamp
     */
    public Member demote() {
        if (!isAdmin()) {
            return this; // Already user, no change needed
        }
        return new Member(this.id, this.userId, this.roomId, Status.USER, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    // ── Unread Messages Management (return new instance) ───────────
    /**
     * Increment unread messages count by the specified amount.
     * @param amount positive value to add to unread count
     * @return new Member instance with updated unread count and timestamp
     */
    public Member incrementUnreadMessages(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("increment amount cannot be negative");
        }
        int newCount = this.unreadMessages + amount;
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         newCount, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    /**
     * Reset unread messages count to zero.
     * @return new Member instance with zero unread count and updated timestamp
     */
    public Member markAllRead() {
        if (this.unreadMessages == 0) {
            return this; // Already zero, no change needed
        }
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         0, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    // ── Membership State Transformers ──────────────────────────────
    /**
     * Mark this member as having left the room voluntarily.
     * @return new Member instance with isLeft flag set to true
     */
    public Member leave() {
        if (isLeft) {
            return this;
        }
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), true);
    }

    /**
     * Remove this member from the room (admin/system-initiated).
     * Semantically distinct from leave() for audit/event purposes.
     * @return new Member instance with isLeft flag set to true
     */
    public Member remove() {
        if (isLeft) {
            return this;
        }
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), true);
    }

    public Member touch() {
        return new Member(this.id, this.userId, this.roomId, this.status, 
                         this.unreadMessages, this.joinedAt, LocalDateTime.now(), this.isLeft);
    }

    // ── Standard Object Methods ────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member member)) return false;
        return Objects.equals(id, member.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", userId=" + userId +
                ", roomId=" + roomId +
                ", status=" + status +
                ", unreadMessages=" + unreadMessages +
                ", isActive=" + isActive() +
                ", joinedAt=" + joinedAt +
                '}';
    }
}