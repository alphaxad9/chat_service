i have (package com.example.chat_service.domain.post.exceptions;

public class InvalidPostContentError extends PostDomainError {
    private final String content;
    private final String reason;
    private final Integer maxLength;

    public InvalidPostContentError(String content, String reason, Integer maxLength, String message) {
        super(message != null ? message : buildMessage(content, reason, maxLength));
        this.content = content; this.reason = reason; this.maxLength = maxLength;
    }
    public InvalidPostContentError(String content, String reason, Integer maxLength) { this(content, reason, maxLength, null); }
    public InvalidPostContentError(String content, String reason) { this(content, reason, null, null); }
    
    private static String buildMessage(String content, String reason, Integer maxLength) {
        String preview = content != null ? (content.length() > 50 ? content.substring(0, 50) + "..." : content) : "null";
        String msg = reason + ": content='" + preview + "'";
        if (maxLength != null) msg += " (max_length=" + maxLength + ")";
        return msg;
    }
    public String getContent() { return content; }
    public String getReason() { return reason; }
    public Integer getMaxLength() { return maxLength; }
})(package com.example.chat_service.domain.post.exceptions;
import java.util.UUID;

public class InvalidPostEntityError extends PostDomainError {
    private final String postId;
    private final String authorId;
    private final String reason;

    public InvalidPostEntityError(UUID postId, UUID authorId, String reason, String message) {
        super(message != null ? message : reason + ": post_id=" + postId + ", author_id=" + authorId);
        this.postId = postId != null ? postId.toString() : null;
        this.authorId = authorId != null ? authorId.toString() : null;
        this.reason = reason;
    }
    public InvalidPostEntityError(UUID postId, UUID authorId, String reason) { this(postId, authorId, reason, null); }
    public String getPostId() { return postId; }
    public String getAuthorId() { return authorId; }
    public String getReason() { return reason; }
})(package com.example.chat_service.domain.post.exceptions;

public class InvalidPostMetricsError extends PostDomainError {
    private final String metricName;
    private final Number value;
    private final String reason;

    public InvalidPostMetricsError(String metricName, Number value, String reason, String message) {
        super(message != null ? message : reason + ": " + metricName + "=" + value + " (must be non-negative)");
        this.metricName = metricName; this.value = value; this.reason = reason;
    }
    public InvalidPostMetricsError(String metricName, Number value, String reason) { this(metricName, value, reason, null); }
    public String getMetricName() { return metricName; }
    public Number getValue() { return value; }
    public String getReason() { return reason; }
})(package com.example.chat_service.domain.post.exceptions;
import java.util.UUID;

public class PostAlreadyExistsError extends PostDomainError {
    private final String postId;
    private final String authorId;

    public PostAlreadyExistsError(UUID postId, UUID authorId, String message) {
        super(message != null ? message : "A post with ID '" + postId + "' by author '" + authorId + "' already exists");
        this.postId = postId.toString();
        this.authorId = authorId.toString();
    }
    public PostAlreadyExistsError(UUID postId, UUID authorId) { this(postId, authorId, null); }
    public String getPostId() { return postId; }
    public String getAuthorId() { return authorId; }
})(package com.example.chat_service.domain.post.exceptions;

public class PostDomainError extends RuntimeException {
    public PostDomainError(String message) { super(message); }
    public PostDomainError(String message, Throwable cause) { super(message, cause); }
})(package com.example.chat_service.domain.post.exceptions;
import java.util.UUID;

public class PostNotFoundError extends PostDomainError {
    private final String postId;
    private final String authorId;

    public PostNotFoundError(UUID postId, UUID authorId, String message) {
        super(message != null ? message : buildMessage(postId, authorId));
        this.postId = postId != null ? postId.toString() : null;
        this.authorId = authorId != null ? authorId.toString() : null;
    }
    public PostNotFoundError(UUID postId, UUID authorId) { this(postId, authorId, null); }
    public PostNotFoundError(String message) { super(message); this.postId = null; this.authorId = null; }
    
    private static String buildMessage(UUID postId, UUID authorId) {
        if (postId != null) return "Post not found (ID: " + postId + ")";
        if (authorId != null) return "Post not found (Author: " + authorId + ")";
        return "Post not found";
    }
    public String getPostId() { return postId; }
    public String getAuthorId() { return authorId; }
})(package com.example.chat_service.domain.post.exceptions;
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
})(package com.example.chat_service.domain.post.exceptions;

public class PostStateTransitionError extends PostDomainError {
    private final boolean currentState;
    private final boolean targetState;
    private final String reason;

    public PostStateTransitionError(boolean currentState, boolean targetState, String reason, String message) {
        super(message != null ? message : reason + ": cannot transition post from is_deleted=" + currentState + " to is_deleted=" + targetState);
        this.currentState = currentState; this.targetState = targetState; this.reason = reason;
    }
    public PostStateTransitionError(boolean currentState, boolean targetState, String reason) { this(currentState, targetState, reason, null); }
    public boolean getCurrentState() { return currentState; }
    public boolean getTargetState() { return targetState; }
    public String getReason() { return reason; }
})(package com.example.chat_service.domain.post.exceptions;
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
})(package com.example.chat_service.domain.post.exceptions;
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
})