// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberDomainError.java
package com.example.chat_service.domain.members.exceptions;

public class MemberDomainError extends RuntimeException {
    public MemberDomainError(String message) { super(message); }
    public MemberDomainError(String message, Throwable cause) { super(message, cause); }
}