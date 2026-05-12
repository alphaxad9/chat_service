
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