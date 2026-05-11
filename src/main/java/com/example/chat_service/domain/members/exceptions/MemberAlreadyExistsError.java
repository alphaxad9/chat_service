// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberAlreadyExistsError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberAlreadyExistsError extends MemberDomainError {
    private final String memberId;
    private final String userId;
    private final String roomId;

    // Main constructor - all parameters explicit
    public MemberAlreadyExistsError(UUID memberId, UUID userId, UUID roomId, String message) {
        super(message != null ? message : "A member with ID '" + memberId + "' (user: " + userId + ", room: " + roomId + ") already exists");
        this.memberId = memberId != null ? memberId.toString() : null;
        this.userId = userId != null ? userId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
    }
    
    // Convenience: 3 UUIDs, no custom message
    public MemberAlreadyExistsError(UUID memberId, UUID userId, UUID roomId) { 
        this(memberId, userId, roomId, null); 
    }
    
    // Convenience: message only (no IDs)
    public MemberAlreadyExistsError(String message) { 
        super(message); 
        this.memberId = null; 
        this.userId = null; 
        this.roomId = null; 
    }
    
    public String getMemberId() { return memberId; }
    public String getUserId() { return userId; }
    public String getRoomId() { return roomId; }
}