// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberOperationNotAllowedError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberOperationNotAllowedError extends MemberDomainError {
    private final String memberId;
    private final String operation;
    private final String reason;

    public MemberOperationNotAllowedError(UUID memberId, String operation, String reason, String message) {
        super(message != null ? message : "Cannot perform '" + operation + "' on member " + memberId + ": " + reason);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.operation = operation;
        this.reason = reason;
    }
    public MemberOperationNotAllowedError(UUID memberId, String operation, String reason) { this(memberId, operation, reason, null); }
    public String getMemberId() { return memberId; }
    public String getOperation() { return operation; }
    public String getReason() { return reason; }
}