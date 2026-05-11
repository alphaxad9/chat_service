// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/RoomDomainError.java
package com.example.chat_service.domain.rooms.exceptions;

public class RoomDomainError extends RuntimeException {
    public RoomDomainError(String message) { super(message); }
    public RoomDomainError(String message, Throwable cause) { super(message, cause); }
}