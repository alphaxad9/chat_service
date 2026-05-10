package com.example.chat_service.domain.post.exceptions;
import java.util.UUID;

public class PostUnauthorizedError extends PostDomainError {
    private final String postId;
    private final String actorId;
    private final String operation;

    public PostUnauthorizedError(UUID postId, UUID actorId, String operation, String message) {
        super(message != null ? message : "Actor " + actorId + " is not authorized to perform '" + operation + "' on post " + postId);
        this.postId = postId != null ? postId.toString() : null;
        this.actorId = actorId != null ? actorId.toString() : null;
        this.operation = operation;
    }
    public PostUnauthorizedError(UUID postId, UUID actorId, String operation) { this(postId, actorId, operation, null); }
    public String getPostId() { return postId; }
    public String getActorId() { return actorId; }
    public String getOperation() { return operation; }
}