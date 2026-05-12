// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomDomainError.java
package com.example.chat_service.domain.rooms.exceptions;

public class RoomDomainError extends RuntimeException {
    public RoomDomainError(String message) { super(message); }
    public RoomDomainError(String message, Throwable cause) { super(message, cause); }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/InvalidRoomEntityError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class InvalidRoomEntityError extends RoomDomainError {
    private final String roomId;
    private final String creatorId;
    private final String roomType;
    private final String reason;

    public InvalidRoomEntityError(UUID roomId, UUID creatorId, String roomType, String reason, String message) {
        super(message != null ? message : reason + ": room_id=" + roomId + ", creator_id=" + creatorId + ", type=" + roomType);
        this.roomId = roomId != null ? roomId.toString() : null;
        this.creatorId = creatorId != null ? creatorId.toString() : null;
        this.roomType = roomType;
        this.reason = reason;
    }
    public InvalidRoomEntityError(UUID roomId, UUID creatorId, String roomType, String reason) { this(roomId, creatorId, roomType, reason, null); }
    public String getRoomId() { return roomId; }
    public String getCreatorId() { return creatorId; }
    public String getRoomType() { return roomType; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/InvalidRoomGroupNameError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class InvalidRoomGroupNameError extends RoomDomainError {
    private final String roomId;
    private final String providedName;
    private final Integer providedLength;
    private final String reason;

    public InvalidRoomGroupNameError(UUID roomId, String providedName, Integer providedLength, String reason, String message) {
        super(message != null ? message : buildMessage(roomId, providedName, providedLength, reason));
        this.roomId = roomId != null ? roomId.toString() : null;
        this.providedName = providedName;
        this.providedLength = providedLength;
        this.reason = reason;
    }
    public InvalidRoomGroupNameError(UUID roomId, String providedName, Integer providedLength, String reason) { this(roomId, providedName, providedLength, reason, null); }
    public InvalidRoomGroupNameError(String providedName, String reason) { this(null, providedName, providedName != null ? providedName.length() : null, reason, null); }
    
    private static String buildMessage(UUID roomId, String providedName, Integer providedLength, String reason) {
        String msg = reason;
        if (roomId != null) msg += " (room_id=" + roomId + ")";
        if (providedName != null) msg += ", provided='" + providedName + "'";
        if (providedLength != null) msg += ", length=" + providedLength;
        return msg;
    }
    public String getRoomId() { return roomId; }
    public String getProvidedName() { return providedName; }
    public Integer getProvidedLength() { return providedLength; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/InvalidRoomDescriptionError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class InvalidRoomDescriptionError extends RoomDomainError {
    private final String roomId;
    private final Integer providedLength;
    private final Integer maxLength;
    private final String reason;

    public InvalidRoomDescriptionError(UUID roomId, Integer providedLength, Integer maxLength, String reason, String message) {
        super(message != null ? message : reason + ": room_id=" + roomId + ", length=" + providedLength + ", max=" + maxLength);
        this.roomId = roomId != null ? roomId.toString() : null;
        this.providedLength = providedLength;
        this.maxLength = maxLength;
        this.reason = reason;
    }
    public InvalidRoomDescriptionError(UUID roomId, Integer providedLength, Integer maxLength, String reason) { this(roomId, providedLength, maxLength, reason, null); }
    public InvalidRoomDescriptionError(Integer providedLength, Integer maxLength, String reason) { this(null, providedLength, maxLength, reason, null); }
    public String getRoomId() { return roomId; }
    public Integer getProvidedLength() { return providedLength; }
    public Integer getMaxLength() { return maxLength; }
    public String getReason() { return reason; }
}


// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/InvalidRoomCreatorError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class InvalidRoomCreatorError extends RoomDomainError {
    private final String roomId;
    private final String currentCreatorId;
    private final String newCreatorId;
    private final String reason;

    public InvalidRoomCreatorError(UUID roomId, UUID currentCreatorId, UUID newCreatorId, String reason, String message) {
        super(message != null ? message : reason + ": room_id=" + roomId + ", current_creator=" + currentCreatorId + ", new_creator=" + newCreatorId);
        this.roomId = roomId != null ? roomId.toString() : null;
        this.currentCreatorId = currentCreatorId != null ? currentCreatorId.toString() : null;
        this.newCreatorId = newCreatorId != null ? newCreatorId.toString() : null;
        this.reason = reason;
    }
    public InvalidRoomCreatorError(UUID roomId, UUID currentCreatorId, UUID newCreatorId, String reason) { this(roomId, currentCreatorId, newCreatorId, reason, null); }
    public InvalidRoomCreatorError(UUID newCreatorId, String reason) { this(null, null, newCreatorId, reason, null); }
    public String getRoomId() { return roomId; }
    public String getCurrentCreatorId() { return currentCreatorId; }
    public String getNewCreatorId() { return newCreatorId; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomAlreadyExistsError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class RoomAlreadyExistsError extends RoomDomainError {
    private final String roomId;
    private final String creatorId;
    private final String roomType;

    public RoomAlreadyExistsError(UUID roomId, UUID creatorId, String roomType, String message) {
        super(message != null ? message : "Room with ID '" + roomId + "' (creator: " + creatorId + ", type: " + roomType + ") already exists");
        this.roomId = roomId != null ? roomId.toString() : null;
        this.creatorId = creatorId != null ? creatorId.toString() : null;
        this.roomType = roomType;
    }
    public RoomAlreadyExistsError(UUID roomId, UUID creatorId, String roomType) { this(roomId, creatorId, roomType, null); }
    public RoomAlreadyExistsError(UUID creatorId, String roomType, String message) { 
        this(null, creatorId, roomType, message != null ? message : "Room already exists (creator: " + creatorId + ", type: " + roomType + ")"); 
    }
    public RoomAlreadyExistsError(UUID creatorId, String roomType) { this(creatorId, roomType, null); }
    public String getRoomId() { return roomId; }
    public String getCreatorId() { return creatorId; }
    public String getRoomType() { return roomType; }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomNotFoundError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class RoomNotFoundError extends RoomDomainError {
    private final String roomId;
    private final String creatorId;
    private final String roomType;

    public RoomNotFoundError(UUID roomId, UUID creatorId, String roomType, String message) {
        super(message != null ? message : buildMessage(roomId, creatorId, roomType));
        this.roomId = roomId != null ? roomId.toString() : null;
        this.creatorId = creatorId != null ? creatorId.toString() : null;
        this.roomType = roomType;
    }
    public RoomNotFoundError(UUID roomId, UUID creatorId, String roomType) { this(roomId, creatorId, roomType, null); }
    public RoomNotFoundError(UUID roomId, String message) { 
        this(roomId, null, null, message != null ? message : "Room not found (ID: " + roomId + ")"); 
    }
    public RoomNotFoundError(UUID roomId) { this(roomId, null); }
    public RoomNotFoundError(String message) { super(message); this.roomId = null; this.creatorId = null; this.roomType = null; }
    
    private static String buildMessage(UUID roomId, UUID creatorId, String roomType) {
        if (roomId != null) return "Room not found (ID: " + roomId + ")";
        if (creatorId != null && roomType != null) return "Room not found (creator: " + creatorId + ", type: " + roomType + ")";
        if (creatorId != null) return "Room not found (creator: " + creatorId + ")";
        if (roomType != null) return "Room not found (type: " + roomType + ")";
        return "Room not found";
    }
    public String getRoomId() { return roomId; }
    public String getCreatorId() { return creatorId; }
    public String getRoomType() { return roomType; }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomOperationNotAllowedError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class RoomOperationNotAllowedError extends RoomDomainError {
    private final String roomId;
    private final String operation;
    private final String reason;

    public RoomOperationNotAllowedError(UUID roomId, String operation, String reason, String message) {
        super(message != null ? message : "Cannot perform '" + operation + "' on room " + roomId + ": " + reason);
        this.roomId = roomId != null ? roomId.toString() : null;
        this.operation = operation;
        this.reason = reason;
    }
    public RoomOperationNotAllowedError(UUID roomId, String operation, String reason) { this(roomId, operation, reason, null); }
    public String getRoomId() { return roomId; }
    public String getOperation() { return operation; }
    public String getReason() { return reason; }
}


// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomStateTransitionError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class RoomStateTransitionError extends RoomDomainError {
    private final String roomId;
    private final String currentState;
    private final String targetState;
    private final String reason;

    public RoomStateTransitionError(UUID roomId, String currentState, String targetState, String reason, String message) {
        super(message != null ? message : reason + ": cannot transition room " + roomId + " from state '" + currentState + "' to '" + targetState + "'");
        this.roomId = roomId != null ? roomId.toString() : null;
        this.currentState = currentState;
        this.targetState = targetState;
        this.reason = reason;
    }
    public RoomStateTransitionError(UUID roomId, String currentState, String targetState, String reason) { this(roomId, currentState, targetState, reason, null); }
    public String getRoomId() { return roomId; }
    public String getCurrentState() { return currentState; }
    public String getTargetState() { return targetState; }
    public String getReason() { return reason; }
}


// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomUnauthorizedError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class RoomUnauthorizedError extends RoomDomainError {
    private final String roomId;
    private final String actorId;
    private final String operation;

    public RoomUnauthorizedError(UUID roomId, UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "' on room " + roomId);
        this.roomId = roomId != null ? roomId.toString() : null;
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public RoomUnauthorizedError(UUID roomId, UUID actorId, String operation) { this(roomId, actorId, operation, null); }
    public String getRoomId() { return roomId; }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}


// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomUnauthorizedErrorWithNoId.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class RoomUnauthorizedErrorWithNoId extends RoomDomainError {
    private final String actorId;
    private final String operation;

    public RoomUnauthorizedErrorWithNoId(UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "'");
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public RoomUnauthorizedErrorWithNoId(UUID actorId, String operation) { this(actorId, operation, null); }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}

// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/InvalidRoomImageError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class InvalidRoomImageError extends RoomDomainError {
    private final String roomId;
    private final String imageType; // "cover" or "profile"
    private final String providedUrl;
    private final String reason;

    public InvalidRoomImageError(UUID roomId, String imageType, String providedUrl, String reason, String message) {
        super(message != null ? message : reason + ": room_id=" + roomId + ", image_type=" + imageType + ", url='" + providedUrl + "'");
        this.roomId = roomId != null ? roomId.toString() : null;
        this.imageType = imageType;
        this.providedUrl = providedUrl;
        this.reason = reason;
    }
    public InvalidRoomImageError(UUID roomId, String imageType, String providedUrl, String reason) { this(roomId, imageType, providedUrl, reason, null); }
    public InvalidRoomImageError(String imageType, String providedUrl, String reason) { this(null, imageType, providedUrl, reason, null); }
    public String getRoomId() { return roomId; }
    public String getImageType() { return imageType; }
    public String getProvidedUrl() { return providedUrl; }
    public String getReason() { return reason; }
}