// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageAlreadyExistsError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class MessageAlreadyExistsError extends MessageDomainError {
    private final String messageId;
    private final String roomId;
    private final String senderId;

    public MessageAlreadyExistsError(UUID messageId, UUID roomId, UUID senderId, String message) {
        super(message != null ? message : buildMessage(messageId, roomId, senderId));
        this.messageId = messageId != null ? messageId.toString() : null;
        this.roomId = roomId != null ? roomId.toString() : null;
        this.senderId = senderId != null ? senderId.toString() : null;
    }
    
    public MessageAlreadyExistsError(UUID messageId, UUID roomId, UUID senderId) { 
        this(messageId, roomId, senderId, null); 
    }
    
    public MessageAlreadyExistsError(UUID roomId, UUID senderId, String message) { 
        this(null, roomId, senderId, message != null ? message : buildMessage(null, roomId, senderId)); 
    }
    
    public MessageAlreadyExistsError(UUID roomId, UUID senderId) { 
        this(null, roomId, senderId, null); 
    }
    
    private static String buildMessage(UUID messageId, UUID roomId, UUID senderId) {
        if (messageId != null) {
            return "Message with ID '" + messageId + "' (room: " + roomId + ", sender: " + senderId + ") already exists";
        }
        if (roomId != null && senderId != null) {
            return "Message already exists (room: " + roomId + ", sender: " + senderId + ")";
        }
        if (roomId != null) {
            return "Message already exists (room: " + roomId + ")";
        }
        if (senderId != null) {
            return "Message already exists (sender: " + senderId + ")";
        }
        return "Message already exists";
    }
    
    public String getMessageId() { return messageId; }
    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
}