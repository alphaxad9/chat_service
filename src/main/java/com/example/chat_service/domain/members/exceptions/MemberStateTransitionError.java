// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberStateTransitionError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberStateTransitionError extends MemberDomainError {
    private final String memberId;
    private final String currentState;
    private final String targetState;
    private final String reason;

    public MemberStateTransitionError(UUID memberId, String currentState, String targetState, String reason, String message) {
        super(message != null ? message : reason + ": cannot transition member " + memberId + " from state '" + currentState + "' to '" + targetState + "'");
        this.memberId = memberId != null ? memberId.toString() : null;
        this.currentState = currentState;
        this.targetState = targetState;
        this.reason = reason;
    }
    public MemberStateTransitionError(UUID memberId, String currentState, String targetState, String reason) { this(memberId, currentState, targetState, reason, null); }
    public String getMemberId() { return memberId; }
    public String getCurrentState() { return currentState; }
    public String getTargetState() { return targetState; }
    public String getReason() { return reason; }
}