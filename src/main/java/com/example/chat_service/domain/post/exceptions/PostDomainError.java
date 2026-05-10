package com.example.chat_service.domain.post.exceptions;

public class PostDomainError extends RuntimeException {
    public PostDomainError(String message) { super(message); }
    public PostDomainError(String message, Throwable cause) { super(message, cause); }
}