// chat_service/src/main/java/com/example/chat_service/domain/messages/exceptions/MessageDomainError.java
package com.example.chat_service.domain.messages.exceptions;

public class MessageDomainError extends RuntimeException {
    public MessageDomainError(String message) { super(message); }
    public MessageDomainError(String message, Throwable cause) { super(message, cause); }
}