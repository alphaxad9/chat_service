package com.example.chat_service.infrastructure.persistence.messages;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Message persistence.
 * Maps the immutable domain Message model to database schema.
 * Uses soft-delete pattern with @SQLRestriction filter for automatic query filtering.
 * 
 * <p>Enforces content-length invariant at application layer (1-10000 chars).
 * All query methods automatically filter out deleted messages via @SQLRestriction("is_deleted = false").</p>
 * 
 * <p>Indexes are optimized for CQRS read-side queries: room-based lookups, sender filtering,
 * status tracking, reply-chain navigation, and cursor-based pagination. See MessageQueryRepository
 * and MessageCommandRepository for query patterns these indexes support.</p>
 * 
 * Note: Soft-delete is handled via repository methods, not @SQLDelete, for Hibernate 7.x compatibility.
 */
@Entity
@Table(
    name = "messages",
    indexes = {
        // ── Primary Lookup Indexes ─────────────────────────────────────
        @Index(name = "idx_messages_room", columnList = "room_id"),
        @Index(name = "idx_messages_sender", columnList = "sender_id"),
        @Index(name = "idx_messages_parent", columnList = "parent_id"),
        @Index(name = "idx_messages_status", columnList = "status"),
        @Index(name = "idx_messages_is_deleted", columnList = "is_deleted"),
        @Index(name = "idx_messages_created", columnList = "created_at"),
        @Index(name = "idx_messages_updated", columnList = "updated_at"),
        @Index(name = "idx_messages_seen", columnList = "seen_at"),
        
        // ── Composite Indexes for Room-Based Queries ───────────────────
        @Index(name = "idx_messages_room_active_created", columnList = "room_id, is_deleted, created_at"),
        @Index(name = "idx_messages_room_active_created_desc", columnList = "room_id, is_deleted, created_at DESC"),
        @Index(name = "idx_messages_room_active_cursor", columnList = "room_id, is_deleted, created_at, id"),
        @Index(name = "idx_messages_room_latest", columnList = "room_id, is_deleted, created_at DESC, id"),
        @Index(name = "idx_messages_room_status_active", columnList = "room_id, status, is_deleted"),
        @Index(name = "idx_messages_room_images_active", columnList = "room_id, is_deleted, image_url, created_at"),
        
        // ── Composite Indexes for Sender-Based Queries ─────────────────
        @Index(name = "idx_messages_sender_active_created", columnList = "sender_id, is_deleted, created_at DESC"),
        @Index(name = "idx_messages_sender_room_active", columnList = "sender_id, room_id, is_deleted"),
        @Index(name = "idx_messages_sender_images_active", columnList = "sender_id, is_deleted, image_url, created_at DESC"),
        @Index(name = "idx_messages_sender_status_active", columnList = "sender_id, status, is_deleted"),
        
        // ── Composite Indexes for Status-Based Queries ─────────────────
        @Index(name = "idx_messages_status_active_created", columnList = "status, is_deleted, created_at DESC"),
        @Index(name = "idx_messages_sent_old_active", columnList = "status, is_deleted, created_at"),
        @Index(name = "idx_messages_status_room_active", columnList = "status, room_id, is_deleted"),
        
        // ── Composite Indexes for Reply Chain Queries ──────────────────
        @Index(name = "idx_messages_parent_active_created", columnList = "parent_id, is_deleted, created_at"),
        @Index(name = "idx_messages_parent_active_created_desc", columnList = "parent_id, is_deleted, created_at DESC"),
        @Index(name = "idx_messages_parent_exists", columnList = "parent_id, is_deleted, id"),
        
        // ── Composite Indexes for Time-Based Queries ───────────────────
        @Index(name = "idx_messages_created_active", columnList = "is_deleted, created_at DESC"),
        @Index(name = "idx_messages_updated_active", columnList = "is_deleted, updated_at DESC"),
        @Index(name = "idx_messages_seen_active", columnList = "is_deleted, seen_at DESC"),
        
        // ── Covering Indexes for Projection Queries (MessageSummary) ───
        @Index(name = "idx_messages_room_summary", columnList = "room_id, is_deleted, created_at, sender_id, status, content, image_url, parent_id, seen_at, updated_at"),
        @Index(name = "idx_messages_room_cursor_summary", columnList = "room_id, is_deleted, created_at, id, sender_id, content, image_url, parent_id, status, seen_at, updated_at"),
        @Index(name = "idx_messages_sender_summary", columnList = "sender_id, is_deleted, created_at DESC, room_id, content, image_url, parent_id, status, seen_at, updated_at"),
        @Index(name = "idx_messages_parent_summary", columnList = "parent_id, is_deleted, created_at, sender_id, room_id, content, image_url, status, seen_at, updated_at"),
        @Index(name = "idx_messages_status_summary", columnList = "status, is_deleted, created_at DESC, sender_id, room_id, content, image_url, parent_id, seen_at, updated_at"),
        
        // ── Bulk Lookup Indexes ────────────────────────────────────────
        @Index(name = "idx_messages_ids_active", columnList = "id, is_deleted"),
        @Index(name = "idx_messages_rooms_active", columnList = "room_id, is_deleted, created_at DESC"),
        
        // ── Advanced Composite Indexes for Complex Queries ─────────────
        @Index(name = "idx_messages_room_sender_active", columnList = "room_id, sender_id, is_deleted"),
        @Index(name = "idx_messages_room_sender_status_active", columnList = "room_id, sender_id, status, is_deleted"),
        @Index(name = "idx_messages_parent_room_active", columnList = "parent_id, room_id, is_deleted"),
        @Index(name = "idx_messages_room_has_image_active", columnList = "room_id, is_deleted, image_url, created_at DESC"),
        @Index(name = "idx_messages_sender_has_image_active", columnList = "sender_id, is_deleted, image_url, created_at DESC")
    }
)
@SQLRestriction("is_deleted = false")
public class MessageEntity {

    @Id
    @Column(columnDefinition = "UUID", updatable = false)
    private UUID id;

    @Column(name = "room_id", nullable = false, columnDefinition = "UUID")
    private UUID roomId;

    @Column(name = "sender_id", nullable = false, columnDefinition = "UUID")
    private UUID senderId;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "parent_id", columnDefinition = "UUID")
    private UUID parentId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    protected MessageEntity() {
        // For JPA/Hibernate only
    }

    public MessageEntity(UUID id, UUID roomId, UUID senderId, String content, String imageUrl,
                         UUID parentId, Status status, LocalDateTime seenAt,
                         LocalDateTime createdAt, LocalDateTime updatedAt, boolean isDeleted) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.roomId = Objects.requireNonNull(roomId, "roomId cannot be null");
        this.senderId = Objects.requireNonNull(senderId, "senderId cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        if (content.length() > 10000) {
            throw new IllegalArgumentException("content cannot exceed 10000 characters");
        }
        this.imageUrl = imageUrl;
        this.parentId = parentId;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.seenAt = seenAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.isDeleted = isDeleted;
    }

    public UUID getId() { return id; }
    public UUID getRoomId() { return roomId; }
    public UUID getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public UUID getParentId() { return parentId; }
    public Status getStatus() { return status; }
    public LocalDateTime getSeenAt() { return seenAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isDeleted() { return isDeleted; }

    void setContent(String content) {
        if (content == null || content.length() > 10000) {
            throw new IllegalArgumentException("content must be non-null and <= 10000 chars");
        }
        this.content = content;
    }

    void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    void setStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
    }

    void setSeenAt(LocalDateTime seenAt) {
        this.seenAt = seenAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    void markDeleted() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    void restore() {
        this.isDeleted = false;
        this.updatedAt = LocalDateTime.now();
    }

    public static MessageEntity fromDomain(com.example.chat_service.domain.messages.Message domain) {
        MessageEntity entity = new MessageEntity();
        entity.id = domain.id();
        entity.roomId = domain.roomId();
        entity.senderId = domain.senderId();
        entity.content = domain.content();
        entity.imageUrl = domain.imageUrl();
        entity.parentId = domain.parentId();
        entity.status = Status.fromDomain(domain.status());
        entity.seenAt = domain.seenAt();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.isDeleted = domain.isDeleted();
        return entity;
    }

    com.example.chat_service.domain.messages.Message toDomain() {
        return new com.example.chat_service.domain.messages.Message(
            this.id,
            this.roomId,
            this.senderId,
            this.content,
            this.imageUrl,
            this.parentId,
            this.status.toDomain(),
            this.seenAt,
            this.createdAt,
            this.updatedAt,
            this.isDeleted
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "id=" + id +
                ", roomId=" + roomId +
                ", senderId=" + senderId +
                ", contentLength=" + content.length() +
                ", hasImage=" + (imageUrl != null) +
                ", isReply=" + (parentId != null) +
                ", parentId=" + parentId +
                ", status=" + status +
                ", isSeen=" + (status == Status.SEEN) +
                ", createdAt=" + createdAt +
                ", isDeleted=" + isDeleted +
                '}';
    }

    public enum Status {
        SENT,
        RECEIVED,
        SEEN;

        public static Status fromDomain(com.example.chat_service.domain.messages.Message.Status domainStatus) {
            if (domainStatus == null) return null;
            return switch (domainStatus) {
                case SENT -> SENT;
                case RECEIVED -> RECEIVED;
                case SEEN -> SEEN;
            };
        }

        public com.example.chat_service.domain.messages.Message.Status toDomain() {
            return switch (this) {
                case SENT -> com.example.chat_service.domain.messages.Message.Status.SENT;
                case RECEIVED -> com.example.chat_service.domain.messages.Message.Status.RECEIVED;
                case SEEN -> com.example.chat_service.domain.messages.Message.Status.SEEN;
            };
        }
    }
}