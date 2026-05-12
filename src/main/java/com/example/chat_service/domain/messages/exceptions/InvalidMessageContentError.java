
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