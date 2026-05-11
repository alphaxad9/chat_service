// chat_service/src/main/java/com/example/chat_service/domain/members/exceptions/MemberUnauthorizedErrorWithNoId.java
package com.example.chat_service.domain.members.exceptions;
import java.util.UUID;

public class MemberUnauthorizedErrorWithNoId extends MemberDomainError {
    private final String actorId;
    private final String operation;

    public MemberUnauthorizedErrorWithNoId(UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "'");
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public MemberUnauthorizedErrorWithNoId(UUID actorId, String operation) { this(actorId, operation, null); }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}