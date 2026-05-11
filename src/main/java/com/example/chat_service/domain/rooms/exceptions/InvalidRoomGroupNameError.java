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