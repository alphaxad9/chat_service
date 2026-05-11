// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/InvalidMemberEntityError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class InvalidMemberEntityError extends MemberDomainError {
    private final String memberId;
    private final String userId;
    private final String roomId;
    private final String reason;

    public InvalidMemberEntityError(UUID memberId, UUID userId, UUID roomId, String reason, String message) {
        super(message != null ? message : reason + ": member_id=" + memberId + ", user_id=" + userId + ", room_id=" + roomId);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.userId = userId != null ? userId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
        this.reason = reason;
    }
    public InvalidMemberEntityError(UUID memberId, UUID userId, UUID roomId, String reason) { this(memberId, userId, roomId, reason, null); }
    public String getMemberId() { return memberId; }
    public String getUserId() { return userId; }
    public String getRoomId() { return roomId; }
    public String getReason() { return reason; }
}