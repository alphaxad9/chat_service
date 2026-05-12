
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