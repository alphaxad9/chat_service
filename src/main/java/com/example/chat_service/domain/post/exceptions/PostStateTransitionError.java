package com.example.chat_service.domain.post.exceptions;

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
}