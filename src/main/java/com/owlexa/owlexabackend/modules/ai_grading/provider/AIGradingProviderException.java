package com.owlexa.owlexabackend.modules.ai_grading.provider;

public class AIGradingProviderException extends RuntimeException {

    private final int statusCode;
    private final boolean retryable;

    public AIGradingProviderException(String message) {
        this(message, null, 0, false);
    }

    public AIGradingProviderException(String message, Throwable cause) {
        this(message, cause, 0, false);
    }

    public AIGradingProviderException(String message, int statusCode, boolean retryable) {
        this(message, null, statusCode, retryable);
    }

    public AIGradingProviderException(String message, Throwable cause, int statusCode, boolean retryable) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int statusCode() {
        return statusCode;
    }

    public boolean retryable() {
        return retryable;
    }
}
