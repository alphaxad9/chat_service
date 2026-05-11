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