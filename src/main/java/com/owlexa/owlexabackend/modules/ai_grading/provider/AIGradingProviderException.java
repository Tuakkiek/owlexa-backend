package com.owlexa.owlexabackend.modules.ai_grading.provider;

public class AIGradingProviderException extends RuntimeException {

    public AIGradingProviderException(String message) {
        super(message);
    }

    public AIGradingProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
