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