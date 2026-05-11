// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/InvalidMemberStatusError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class InvalidMemberStatusError extends MemberDomainError {
    private final String memberId;
    private final String currentStatus;
    private final String targetStatus;
    private final String reason;

    public InvalidMemberStatusError(UUID memberId, String currentStatus, String targetStatus, String reason, String message) {
        super(message != null ? message : reason + ": cannot change status from '" + currentStatus + "' to '" + targetStatus + "' for member " + memberId);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.reason = reason;
    }
    public InvalidMemberStatusError(UUID memberId, String currentStatus, String targetStatus, String reason) { this(memberId, currentStatus, targetStatus, reason, null); }
    public String getMemberId() { return memberId; }
    public String getCurrentStatus() { return currentStatus; }
    public String getTargetStatus() { return targetStatus; }
    public String getReason() { return reason; }
}