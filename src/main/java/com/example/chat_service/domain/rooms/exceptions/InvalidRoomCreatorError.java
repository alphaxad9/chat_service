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