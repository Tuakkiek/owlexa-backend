package com.owlexa.owlexabackend.exception;

import com.owlexa.owlexabackend.dto.response.BulkTeacherError;
import com.owlexa.owlexabackend.dto.response.BulkTeacherResult;

import java.util.List;

public class BulkTeacherValidationException extends RuntimeException {

    private List<BulkTeacherError> errors;

    // Constructor
    public BulkTeacherValidationException(List<BulkTeacherError> errors) {
        super("Bulk teacher validation failed");
        this.errors = errors;
    }

    public List<BulkTeacherError> getErrors() {
        return errors;
    }
}
