
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