package com.example.chat_service.domain.post.exceptions;
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
}