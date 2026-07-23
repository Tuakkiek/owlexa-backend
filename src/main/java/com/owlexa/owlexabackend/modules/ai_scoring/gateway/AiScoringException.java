package com.owlexa.owlexabackend.modules.ai_scoring.gateway;

/**
 * Thrown when the AI provider returns an error or produces unparseable output
 * after all retry attempts have been exhausted.
 */
public class AiScoringException extends RuntimeException {

    public AiScoringException(String message) {
        super(message);
    }

    public AiScoringException(String message, Throwable cause) {
        super(message, cause);
    }
}
