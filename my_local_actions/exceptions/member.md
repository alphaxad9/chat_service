and i have (
    // chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberDomainError.java
package com.example.chat_service.domain.members.exceptions;

public class MemberDomainError extends RuntimeException {
    public MemberDomainError(String message) { super(message); }
    public MemberDomainError(String message, Throwable cause) { super(message, cause); }
}
)

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

 
// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/InvalidMemberStatusError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class InvalidMemberStatusError extends MemberDomainError {
    private final String memberId;
    private final String currentStatus;
    private final String targetStatus;
    private final String reason;

    public InvalidMemberStatusError(UUID memberId, String currentStatus, String targetStatus, String reason, String message) {
        super(message != null ? message : reason + ": cannot change status from '" + currentStatus + "' to '" + targetStatus + "' for member " + memberId);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.reason = reason;
    }
    public InvalidMemberStatusError(UUID memberId, String currentStatus, String targetStatus, String reason) { this(memberId, currentStatus, targetStatus, reason, null); }
    public String getMemberId() { return memberId; }
    public String getCurrentStatus() { return currentStatus; }
    public String getTargetStatus() { return targetStatus; }
    public String getReason() { return reason; }
}


 

// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/InvalidUnreadMessagesError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class InvalidUnreadMessagesError extends MemberDomainError {
    private final String memberId;
    private final Integer currentValue;
    private final Integer incrementValue;
    private final String reason;

    public InvalidUnreadMessagesError(UUID memberId, Integer currentValue, Integer incrementValue, String reason, String message) {
        super(message != null ? message : buildMessage(memberId, currentValue, incrementValue, reason));
        this.memberId = memberId != null ? memberId.toString() : null;
        this.currentValue = currentValue;
        this.incrementValue = incrementValue;
        this.reason = reason;
    }
    public InvalidUnreadMessagesError(UUID memberId, Integer currentValue, Integer incrementValue, String reason) { this(memberId, currentValue, incrementValue, reason, null); }
    public InvalidUnreadMessagesError(Integer incrementValue, String reason) { this(null, null, incrementValue, reason, null); }
    
    private static String buildMessage(UUID memberId, Integer currentValue, Integer incrementValue, String reason) {
        String msg = reason;
        if (memberId != null) msg += " (member_id=" + memberId + ")";
        if (currentValue != null) msg += ", current=" + currentValue;
        if (incrementValue != null) msg += ", attempted_increment=" + incrementValue;
        return msg;
    }
    public String getMemberId() { return memberId; }
    public Integer getCurrentValue() { return currentValue; }
    public Integer getIncrementValue() { return incrementValue; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberAlreadyExistsError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberAlreadyExistsError extends MemberDomainError {
    private final String memberId;
    private final String userId;
    private final String roomId;

    public MemberAlreadyExistsError(UUID memberId, UUID userId, UUID roomId, String message) {
        super(message != null ? message : "A member with ID '" + memberId + "' (user: " + userId + ", room: " + roomId + ") already exists");
        this.memberId = memberId != null ? memberId.toString() : null;
        this.userId = userId != null ? userId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
    }
    public MemberAlreadyExistsError(UUID memberId, UUID userId, UUID roomId) { this(memberId, userId, roomId, null); }
    public MemberAlreadyExistsError(UUID userId, UUID roomId, String message) { 
        this(null, userId, roomId, message != null ? message : "User " + userId + " is already a member of room " + roomId); 
    }
    public MemberAlreadyExistsError(UUID userId, UUID roomId) { this(userId, roomId, null); }
    public String getMemberId() { return memberId; }
    public String getUserId() { return userId; }
    public String getRoomId() { return roomId; }
}

// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberNotFoundError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberNotFoundError extends MemberDomainError {
    private final String memberId;
    private final String userId;
    private final String roomId;

    public MemberNotFoundError(UUID memberId, UUID userId, UUID roomId, String message) {
        super(message != null ? message : buildMessage(memberId, userId, roomId));
        this.memberId = memberId != null ? memberId.toString() : null;
        this.userId = userId != null ? userId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
    }
    public MemberNotFoundError(UUID memberId, UUID userId, UUID roomId) { this(memberId, userId, roomId, null); }
    public MemberNotFoundError(UUID userId, UUID roomId, String message) { 
        this(null, userId, roomId, message != null ? message : "Member not found (user: " + userId + ", room: " + roomId + ")"); 
    }
    public MemberNotFoundError(UUID userId, UUID roomId) { this(userId, roomId, null); }
    public MemberNotFoundError(String message) { super(message); this.memberId = null; this.userId = null; this.roomId = null; }
    
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

 

// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberOperationNotAllowedError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberOperationNotAllowedError extends MemberDomainError {
    private final String memberId;
    private final String operation;
    private final String reason;

    public MemberOperationNotAllowedError(UUID memberId, String operation, String reason, String message) {
        super(message != null ? message : "Cannot perform '" + operation + "' on member " + memberId + ": " + reason);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.operation = operation;
        this.reason = reason;
    }
    public MemberOperationNotAllowedError(UUID memberId, String operation, String reason) { this(memberId, operation, reason, null); }
    public String getMemberId() { return memberId; }
    public String getOperation() { return operation; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberStateTransitionError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberStateTransitionError extends MemberDomainError {
    private final String memberId;
    private final String currentState;
    private final String targetState;
    private final String reason;

    public MemberStateTransitionError(UUID memberId, String currentState, String targetState, String reason, String message) {
        super(message != null ? message : reason + ": cannot transition member " + memberId + " from state '" + currentState + "' to '" + targetState + "'");
        this.memberId = memberId != null ? memberId.toString() : null;
        this.currentState = currentState;
        this.targetState = targetState;
        this.reason = reason;
    }
    public MemberStateTransitionError(UUID memberId, String currentState, String targetState, String reason) { this(memberId, currentState, targetState, reason, null); }
    public String getMemberId() { return memberId; }
    public String getCurrentState() { return currentState; }
    public String getTargetState() { return targetState; }
    public String getReason() { return reason; }
}


 
// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberUnauthorizedError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberUnauthorizedError extends MemberDomainError {
    private final String memberId;
    private final String actorId;
    private final String operation;

    public MemberUnauthorizedError(UUID memberId, UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "' on member " + memberId);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public MemberUnauthorizedError(UUID memberId, UUID actorId, String operation) { this(memberId, actorId, operation, null); }
    public String getMemberId() { return memberId; }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}


// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberUnauthorizedErrorWithNoId.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberUnauthorizedErrorWithNoId extends MemberDomainError {
    private final String actorId;
    private final String operation;

    public MemberUnauthorizedErrorWithNoId(UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "'");
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public MemberUnauthorizedErrorWithNoId(UUID actorId, String operation) { this(actorId, operation, null); }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}