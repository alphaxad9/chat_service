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