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