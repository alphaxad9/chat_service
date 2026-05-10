package com.example.chat_service.domain.post.exceptions;

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
}