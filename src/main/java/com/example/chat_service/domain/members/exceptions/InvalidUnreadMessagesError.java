// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/InvalidUnreadMessagesError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class InvalidUnreadMessagesError extends MemberDomainError {
    private final String memberId;
    private final Integer currentValue;
    private final Integer incrementValue;
    private final String reason;

    public InvalidUnreadMessagesError(UUID memberId, Integer currentValue, Integer incrementValue, String reason, String message) {
        super(message != null ? message : buildMessage(memberId, currentValue, incrementValue, reason));
        this.memberId = memberId != null ? memberId.toString() : null;
        this.currentValue = currentValue;
        this.incrementValue = incrementValue;
        this.reason = reason;
    }
    public InvalidUnreadMessagesError(UUID memberId, Integer currentValue, Integer incrementValue, String reason) { this(memberId, currentValue, incrementValue, reason, null); }
    public InvalidUnreadMessagesError(Integer incrementValue, String reason) { this(null, null, incrementValue, reason, null); }
    
    private static String buildMessage(UUID memberId, Integer currentValue, Integer incrementValue, String reason) {
        String msg = reason;
        if (memberId != null) msg += " (member_id=" + memberId + ")";
        if (currentValue != null) msg += ", current=" + currentValue;
        if (incrementValue != null) msg += ", attempted_increment=" + incrementValue;
        return msg;
    }
    public String getMemberId() { return memberId; }
    public Integer getCurrentValue() { return currentValue; }
    public Integer getIncrementValue() { return incrementValue; }
    public String getReason() { return reason; }
}