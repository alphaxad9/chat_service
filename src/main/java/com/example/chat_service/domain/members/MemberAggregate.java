// chat_service/src/main/java/com/example/chat_service/domain/members/MemberAggregate.java
package com.example.chat_service.domain.members;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ← Imports for exceptions in sub-package
import com.example.chat_service.domain.members.exceptions.InvalidMemberEntityError;
import com.example.chat_service.domain.members.exceptions.InvalidUnreadMessagesError;
import com.example.chat_service.domain.members.exceptions.MemberOperationNotAllowedError;
import com.example.chat_service.domain.members.exceptions.MemberStateTransitionError;
import com.example.chat_service.domain.members.exceptions.MemberUnauthorizedError;
import com.example.chat_service.domain.members.exceptions.MemberUnauthorizedErrorWithNoId;
import static java.util.Objects.requireNonNull;

/**
 * Aggregate root for managing the lifecycle and state of a Member.
 * Enforces business rules, coordinates state transitions, guards operations,
 * and validates ownership for user-initiated actions.
 */
public final class MemberAggregate {

    private Member member; // Mutable reference to current state; Member itself is immutable

    private MemberAggregate(Member member) {
        this.member = requireNonNull(member, "member cannot be null");
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public Member member() {
        return member;
    }

    // ── Factory Methods ──────────────────────────────────────────────

    /**
     * Create an aggregate from an existing Member entity (e.g., loaded from repository).
     */
    public static MemberAggregate fromEntity(Member member) {
        return new MemberAggregate(member);
    }

    /**
     * Create a new member aggregate with validation.
     * Fails fast on invalid input before entity creation.
     */
    public static MemberAggregate createNew(
            UUID id,
            UUID userId,
            UUID roomId,
            Member.Status initialStatus,
            LocalDateTime joinedAt
    ) {
        // Validate IDs
        if (id == null) {
            throw new InvalidMemberEntityError(null, userId, roomId, "Member ID cannot be null");
        }
        if (userId == null) {
            throw new InvalidMemberEntityError(id, null, roomId, "User ID cannot be null");
        }
        if (roomId == null) {
            throw new InvalidMemberEntityError(id, userId, null, "Room ID cannot be null");
        }
        if (initialStatus == null) {
            throw new InvalidMemberEntityError(id, userId, roomId, "Initial status cannot be null");
        }

        // Use provided timestamp or default to now
        
        Member newMember = Member.create(id, userId, roomId, initialStatus);
        // Note: Member.create() uses LocalDateTime.now() internally.
        // If exact timestamp control is needed, use constructor directly via infrastructure.
        return new MemberAggregate(newMember);
    }

    /**
     * Convenience overload using current timestamp.
     */
    public static MemberAggregate createNew(
            UUID id,
            UUID userId,
            UUID roomId,
            Member.Status initialStatus
    ) {
        return createNew(id, userId, roomId, initialStatus, null);
    }

    /**
     * Convenience factory for creating a regular USER member.
     */
    public static MemberAggregate createNewAsUser(UUID id, UUID userId, UUID roomId) {
        return createNew(id, userId, roomId, Member.Status.USER);
    }

    /**
     * Convenience factory for creating an ADMIN member (e.g., room creator).
     */
    public static MemberAggregate createNewAsAdmin(UUID id, UUID userId, UUID roomId) {
        return createNew(id, userId, roomId, Member.Status.ADMIN);
    }

    // ── Business Operations ──────────────────────────────────────────

    /**
     * Promote this member to ADMIN status.
     * Application layer should verify caller has admin privileges before calling.
     */
    public MemberAggregate promote() {
        ensureActive("promote");
        
        if (member.isAdmin()) {
            throw new MemberStateTransitionError(
                member.id(),
                member.status().name(),
                Member.Status.ADMIN.name(),
                "Member is already an admin"
            );
        }
        
        this.member = member.promote();
        return this;
    }

    /**
     * Demote this member to USER status.
     * Application layer should verify caller has admin privileges before calling.
     */
    public MemberAggregate demote() {
        ensureActive("demote");
        
        if (!member.isAdmin()) {
            throw new MemberStateTransitionError(
                member.id(),
                member.status().name(),
                Member.Status.USER.name(),
                "Member is already a regular user"
            );
        }
        
        this.member = member.demote();
        return this;
    }

    /**
     * Increment unread messages count (system operation, e.g., new message delivered).
     * Does not require ownership check - typically called by message delivery service.
     */
    public MemberAggregate addUnreadMessages(int amount) {
        ensureActive("add_unread_messages");
        
        if (amount < 0) {
            throw new InvalidUnreadMessagesError(
                member.id(),
                member.unreadMessages(),
                amount,
                "increment amount cannot be negative"
            );
        }
        
        this.member = member.incrementUnreadMessages(amount);
        return this;
    }

    /**
     * Mark all messages as read.
     * Requires ownership - only the member themselves can clear their unread count.
     * @param requesterId ID of the user attempting this operation
     */
    public MemberAggregate markAllRead(UUID requesterId) {
        ensureActive("mark_all_read");
        ensureOwnership(requesterId, "mark_all_read");
        
        this.member = member.markAllRead();
        return this;
    }

    /**
     * Member voluntarily leaves the room.
     * Requires ownership - only the member can leave their own membership.
     * @param requesterId ID of the user attempting to leave
     */
    public MemberAggregate leave(UUID requesterId) {
        ensureActive("leave");
        ensureOwnership(requesterId, "leave");
        
        if (member.isLeft()) {
            throw new MemberStateTransitionError(
                member.id(),
                "active",
                "left",
                "Member has already left this room"
            );
        }
        
        this.member = member.leave();
        return this;
    }

    /**
     * Remove this member from the room (admin/system-initiated).
     * Application layer should verify caller has admin privileges before calling.
     * Does not use ensureOwnership - admins can remove any member.
     */
    public MemberAggregate remove() {
        ensureActive("remove");
        
        if (member.isLeft()) {
            throw new MemberStateTransitionError(
                member.id(),
                "active",
                "left",
                "Member has already left this room"
            );
        }
        
        this.member = member.remove();
        return this;
    }

    /**
     * Update the updated_at timestamp (e.g., for cache invalidation or heartbeat).
     * Requires ownership for user-initiated touches.
     * @param requesterId ID of the user performing the touch
     */
    public MemberAggregate touch(UUID requesterId) {
        ensureActive("touch");
        ensureOwnership(requesterId, "touch");
        
        this.member = member.touch();
        return this;
    }

    /**
     * Internal touch for system use (no ownership check).
     * Use sparingly - prefer explicit requesterId version for audit trails.
     */
    public MemberAggregate touchInternal() {
        ensureActive("touch_internal");
        this.member = member.touch();
        return this;
    }

    // ── State Queries (delegated to Member) ────────────────────────────

    public boolean isActive() {
        return member.isActive();
    }

    public boolean isAdmin() {
        return member.isAdmin();
    }

    public boolean hasUnreadMessages() {
        return member.hasUnreadMessages();
    }

    public UUID id() { return member.id(); }
    public UUID userId() { return member.userId(); }
    public UUID roomId() { return member.roomId(); }
    public Member.Status status() { return member.status(); }
    public int unreadMessages() { return member.unreadMessages(); }
    public LocalDateTime joinedAt() { return member.joinedAt(); }
    public boolean isLeft() { return member.isLeft(); }

    // ── Helper Methods ───────────────────────────────────────────────

    /**
     * Verify that the requester is the owner of this member record.
     * Throws MemberUnauthorizedError if IDs don't match.
     * @param requesterId ID of the user attempting the operation
     * @param operation Name of the operation for error context
     */
    private void ensureOwnership(UUID requesterId, String operation) {
        if (requesterId == null) {
            throw new MemberUnauthorizedErrorWithNoId(
                null,
                operation,
                "Requester ID cannot be null for ownership check"
            );
        }
        if (!requesterId.equals(member.userId())) {
            throw new MemberUnauthorizedError(
                member.id(),
                requesterId,
                operation,
                "User " + requesterId + " cannot perform '" + operation + "' on member record belonging to user " + member.userId()
            );
        }
    }

    private void ensureActive(String operation) {
        if (!member.isActive()) {
            throw new MemberOperationNotAllowedError(
                member.id(),
                operation,
                "Member is inactive or has left the room"
            );
        }
    }

    // ── Standard Object Methods ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberAggregate that)) return false;
        return Objects.equals(member.id(), that.member.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(member.id());
    }

    @Override
    public String toString() {
        return "MemberAggregate{" +
                "id=" + member.id() +
                ", userId=" + member.userId() +
                ", roomId=" + member.roomId() +
                ", status=" + member.status() +
                ", unreadMessages=" + member.unreadMessages() +
                ", isActive=" + isActive() +
                ", joinedAt=" + member.joinedAt() +
                '}';
    }
}