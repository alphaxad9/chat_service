
// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/InvalidMessageEntityError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class InvalidMessageEntityError extends MessageDomainError {
    private final String messageId;
    private final String roomId;
    private final String senderId;
    private final String reason;

    public InvalidMessageEntityError(UUID messageId, UUID roomId, UUID senderId, String reason, String message) {
        super(message != null ? message : reason + ": message_id=" + messageId + ", room_id=" + roomId + ", sender_id=" + senderId);
        this.messageId = messageId != null ? messageId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
        this.senderId = senderId != null ? senderId.toString() : null;
        this.reason = reason;
    }
    public InvalidMessageEntityError(UUID messageId, UUID roomId, UUID senderId, String reason) { this(messageId, roomId, senderId, reason, null); }
    public String getMessageId() { return messageId; }
    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
    public String getReason() { return reason; }
}