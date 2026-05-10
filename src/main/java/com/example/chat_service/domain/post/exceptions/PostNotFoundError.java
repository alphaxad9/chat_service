package com.example.chat_service.domain.post.exceptions;
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
}