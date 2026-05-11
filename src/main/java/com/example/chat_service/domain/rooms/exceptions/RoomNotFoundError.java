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