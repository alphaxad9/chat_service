
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
