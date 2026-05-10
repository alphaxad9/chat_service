package com.example.chat_service.domain.post.exceptions;
import java.util.UUID;

public class PostOperationNotAllowedError extends PostDomainError {
    private final String postId;
    private final String operation;
    private final String reason;

    public PostOperationNotAllowedError(UUID postId, String operation, String reason, String message) {
        super(message != null ? message : "Cannot perform '" + operation + "' on post " + postId + ": " + reason);
        this.postId = postId != null ? postId.toString() : null;
        this.operation = operation; this.reason = reason;
    }
    public PostOperationNotAllowedError(UUID postId, String operation, String reason) { this(postId, operation, reason, null); }
    public String getPostId() { return postId; }
    public String getOperation() { return operation; }
    public String getReason() { return reason; }
}