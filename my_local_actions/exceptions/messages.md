// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageDomainError.java
package com.example.chat_service.domain.messages.exceptions;

public class MessageDomainError extends RuntimeException {
    public MessageDomainError(String message) { super(message); }
    public MessageDomainError(String message, Throwable cause) { super(message, cause); }
}

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

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/InvalidMessageContentError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class InvalidMessageContentError extends MessageDomainError {
    private final String messageId;
    private final String providedContent;
    private final Integer providedLength;
    private final Integer maxLength;
    private final String reason;

    public InvalidMessageContentError(UUID messageId, String providedContent, Integer providedLength, Integer maxLength, String reason, String message) {
        super(message != null ? message : buildMessage(messageId, providedContent, providedLength, maxLength, reason));
        this.messageId = messageId != null ? messageId.toString() : null;
        this.providedContent = providedContent;
        this.providedLength = providedLength;
        this.maxLength = maxLength;
        this.reason = reason;
    }
    public InvalidMessageContentError(UUID messageId, String providedContent, Integer providedLength, Integer maxLength, String reason) { this(messageId, providedContent, providedLength, maxLength, reason, null); }
    public InvalidMessageContentError(String providedContent, String reason) { this(null, providedContent, providedContent != null ? providedContent.length() : null, 10000, reason, null); }
    
    private static String buildMessage(UUID messageId, String providedContent, Integer providedLength, Integer maxLength, String reason) {
        String msg = reason;
        if (messageId != null) msg += " (message_id=" + messageId + ")";
        if (providedContent != null) msg += ", provided='" + (providedContent.length() > 50 ? providedContent.substring(0, 50) + "..." : providedContent) + "'";
        if (providedLength != null) msg += ", length=" + providedLength;
        if (maxLength != null) msg += ", max=" + maxLength;
        return msg;
    }
    public String getMessageId() { return messageId; }
    public String getProvidedContent() { return providedContent; }
    public Integer getProvidedLength() { return providedLength; }
    public Integer getMaxLength() { return maxLength; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/InvalidMessageImageError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class InvalidMessageImageError extends MessageDomainError {
    private final String messageId;
    private final String providedUrl;
    private final String reason;

    public InvalidMessageImageError(UUID messageId, String providedUrl, String reason, String message) {
        super(message != null ? message : reason + ": message_id=" + messageId + ", url='" + providedUrl + "'");
        this.messageId = messageId != null ? messageId.toString() : null;
        this.providedUrl = providedUrl;
        this.reason = reason;
    }
    public InvalidMessageImageError(UUID messageId, String providedUrl, String reason) { this(messageId, providedUrl, reason, null); }
    public InvalidMessageImageError(String providedUrl, String reason) { this(null, providedUrl, reason, null); }
    public String getMessageId() { return messageId; }
    public String getProvidedUrl() { return providedUrl; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/InvalidMessageStatusError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class InvalidMessageStatusError extends MessageDomainError {
    private final String messageId;
    private final String currentStatus;
    private final String targetStatus;
    private final String reason;

    public InvalidMessageStatusError(UUID messageId, String currentStatus, String targetStatus, String reason, String message) {
        super(message != null ? message : reason + ": cannot transition message " + messageId + " from status '" + currentStatus + "' to '" + targetStatus + "'");
        this.messageId = messageId != null ? messageId.toString() : null;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.reason = reason;
    }
    public InvalidMessageStatusError(UUID messageId, String currentStatus, String targetStatus, String reason) { this(messageId, currentStatus, targetStatus, reason, null); }
    public InvalidMessageStatusError(String currentStatus, String targetStatus, String reason) { this(null, currentStatus, targetStatus, reason, null); }
    public String getMessageId() { return messageId; }
    public String getCurrentStatus() { return currentStatus; }
    public String getTargetStatus() { return targetStatus; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/InvalidMessageParentError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class InvalidMessageParentError extends MessageDomainError {
    private final String messageId;
    private final String parentId;
    private final String reason;

    public InvalidMessageParentError(UUID messageId, UUID parentId, String reason, String message) {
        super(message != null ? message : buildMessage(messageId, parentId, reason));
        this.messageId = messageId != null ? messageId.toString() : null;
        this.parentId = parentId != null ? parentId.toString() : null;
        this.reason = reason;
    }
    public InvalidMessageParentError(UUID messageId, UUID parentId, String reason) { this(messageId, parentId, reason, null); }
    public InvalidMessageParentError(UUID parentId, String reason) { this(null, parentId, reason, null); }
    
    private static String buildMessage(UUID messageId, UUID parentId, String reason) {
        String msg = reason;
        if (messageId != null && parentId != null && messageId.equals(parentId)) {
            msg += ": message cannot reference itself as parent (id=" + messageId + ")";
        } else if (messageId != null) {
            msg += " (message_id=" + messageId + ")";
        }
        if (parentId != null) msg += ", parent_id=" + parentId;
        return msg;
    }
    public String getMessageId() { return messageId; }
    public String getParentId() { return parentId; }
    public String getReason() { return reason; }
}

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

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageOperationNotAllowedError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class MessageOperationNotAllowedError extends MessageDomainError {
    private final String messageId;
    private final String operation;
    private final String reason;

    public MessageOperationNotAllowedError(UUID messageId, String operation, String reason, String message) {
        super(message != null ? message : "Cannot perform '" + operation + "' on message " + messageId + ": " + reason);
        this.messageId = messageId != null ? messageId.toString() : null;
        this.operation = operation;
        this.reason = reason;
    }
    public MessageOperationNotAllowedError(UUID messageId, String operation, String reason) { this(messageId, operation, reason, null); }
    public String getMessageId() { return messageId; }
    public String getOperation() { return operation; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageStateTransitionError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class MessageStateTransitionError extends MessageDomainError {
    private final String messageId;
    private final String currentState;
    private final String targetState;
    private final String reason;

    public MessageStateTransitionError(UUID messageId, String currentState, String targetState, String reason, String message) {
        super(message != null ? message : reason + ": cannot transition message " + messageId + " from state '" + currentState + "' to '" + targetState + "'");
        this.messageId = messageId != null ? messageId.toString() : null;
        this.currentState = currentState;
        this.targetState = targetState;
        this.reason = reason;
    }
    public MessageStateTransitionError(UUID messageId, String currentState, String targetState, String reason) { this(messageId, currentState, targetState, reason, null); }
    public String getMessageId() { return messageId; }
    public String getCurrentState() { return currentState; }
    public String getTargetState() { return targetState; }
    public String getReason() { return reason; }
}

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageUnauthorizedError.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class MessageUnauthorizedError extends MessageDomainError {
    private final String messageId;
    private final String actorId;
    private final String operation;

    public MessageUnauthorizedError(UUID messageId, UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "' on message " + messageId);
        this.messageId = messageId != null ? messageId.toString() : null;
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public MessageUnauthorizedError(UUID messageId, UUID actorId, String operation) { this(messageId, actorId, operation, null); }
    public String getMessageId() { return messageId; }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}

// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageUnauthorizedErrorWithNoId.java
package com.example.chat_service.domain.messages.exceptions;
import java.util.UUID;

public class MessageUnauthorizedErrorWithNoId extends MessageDomainError {
    private final String actorId;
    private final String operation;

    public MessageUnauthorizedErrorWithNoId(UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "'");
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public MessageUnauthorizedErrorWithNoId(UUID actorId, String operation) { this(actorId, operation, null); }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}
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