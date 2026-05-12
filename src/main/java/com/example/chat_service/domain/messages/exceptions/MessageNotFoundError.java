
// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageNotFoundError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class MessageNotFoundError extends MessageDomainError {
    private final String messageId;
    private final String roomId;
    private final String senderId;

    public MessageNotFoundError(UUID messageId, UUID roomId, UUID senderId, String message) {
        super(message != null ? message : buildMessage(messageId, roomId, senderId));
        this.messageId = messageId != null ? messageId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
        this.senderId = senderId != null ? senderId.toString() : null;
    }
    public MessageNotFoundError(UUID messageId, UUID roomId, UUID senderId) { this(messageId, roomId, senderId, null); }
    public MessageNotFoundError(UUID messageId, String message) { 
        this(messageId, null, null, message != null ? message : "Message not found (ID: " + messageId + ")"); 
    }
    public MessageNotFoundError(UUID messageId) { this(messageId, null); }
    public MessageNotFoundError(String message) { super(message); this.messageId = null; this.roomId = null; this.senderId = null; }
    
    private static String buildMessage(UUID messageId, UUID roomId, UUID senderId) {
        if (messageId != null) return "Message not found (ID: " + messageId + ")";
        if (roomId != null && senderId != null) return "Message not found (room: " + roomId + ", sender: " + senderId + ")";
        if (roomId != null) return "Message not found (room: " + roomId + ")";
        if (senderId != null) return "Message not found (sender: " + senderId + ")";
        return "Message not found";
    }
    public String getMessageId() { return messageId; }
    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
}