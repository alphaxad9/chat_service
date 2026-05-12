
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