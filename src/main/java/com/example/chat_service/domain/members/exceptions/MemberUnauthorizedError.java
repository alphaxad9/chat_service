// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberUnauthorizedError.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberUnauthorizedError extends MemberDomainError {
    private final String memberId;
    private final String actorId;
    private final String operation;

    public MemberUnauthorizedError(UUID memberId, UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "' on member " + memberId);
        this.memberId = memberId != null ? memberId.toString() : null;
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public MemberUnauthorizedError(UUID memberId, UUID actorId, String operation) { this(memberId, actorId, operation, null); }
    public String getMemberId() { return memberId; }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}