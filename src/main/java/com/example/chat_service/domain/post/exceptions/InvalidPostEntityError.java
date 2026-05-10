package com.example.chat_service.domain.post.exceptions;
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
}