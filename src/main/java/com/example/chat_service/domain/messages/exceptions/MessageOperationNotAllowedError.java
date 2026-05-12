
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