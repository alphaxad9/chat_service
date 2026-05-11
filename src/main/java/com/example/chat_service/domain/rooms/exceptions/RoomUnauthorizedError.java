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