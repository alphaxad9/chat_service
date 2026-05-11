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