package com.example.chat_service.domain.post.exceptions;
import java.util.UUID;

public class PostUnauthorizedErrorWithNoId extends PostDomainError {
    private final String actorId;
    private final String operation;

    public PostUnauthorizedErrorWithNoId(UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "'");
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public PostUnauthorizedErrorWithNoId(UUID actorId, String operation) { this(actorId, operation, null); }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}