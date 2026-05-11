// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberNotFoundError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberNotFoundError extends MemberDomainError {
    private final String memberId;
    private final String userId;
    private final String roomId;

    // Main constructor - all parameters explicit
    public MemberNotFoundError(UUID memberId, UUID userId, UUID roomId, String message) {
        super(message != null ? message : buildMessage(memberId, userId, roomId));
        this.memberId = memberId != null ? memberId.toString() : null;
        this.userId = userId != null ? userId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
    }
    
    // Convenience: 3 UUIDs, no custom message
    public MemberNotFoundError(UUID memberId, UUID userId, UUID roomId) { 
        this(memberId, userId, roomId, null); 
    }
    
    // Convenience: message only (no IDs)
    public MemberNotFoundError(String message) { 
        super(message); 
        this.memberId = null; 
        this.userId = null; 
        this.roomId = null; 
    }
    
    private static String buildMessage(UUID memberId, UUID userId, UUID roomId) {
        if (memberId != null) return "Member not found (ID: " + memberId + ")";
        if (userId != null && roomId != null) return "Member not found (user: " + userId + ", room: " + roomId + ")";
        if (userId != null) return "Member not found (user: " + userId + ")";
        if (roomId != null) return "Member not found (room: " + roomId + ")";
        return "Member not found";
    }
    
    public String getMemberId() { return memberId; }
    public String getUserId() { return userId; }
    public String getRoomId() { return roomId; }
}