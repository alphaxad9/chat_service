package com.example.chat_service.domain.post.exceptions;

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
}