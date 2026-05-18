package com.owlexa.owlexabackend.exception;

import com.owlexa.owlexabackend.dto.response.BulkStudentError;

import java.util.List;

public class BulkStudentValidationException extends RuntimeException {

    private final List<BulkStudentError> errors;

    public BulkStudentValidationException(List<BulkStudentError> errors) {
        super("Bulk student validation failed");
        this.errors = errors;
    }

    public List<BulkStudentError> getErrors() {
        return errors;
    }
}
