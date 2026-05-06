package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.BulkTeacherStatus;

public class BulkTeacherResult {

    private String phoneNumber;
    private BulkTeacherStatus status;
    private String temporaryPassword;

    public String getPhoneNumber() {return phoneNumber;}

    public BulkTeacherStatus getStatus() {return status;}

    public String getTemporaryPassword() {return temporaryPassword;}

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStatus(BulkTeacherStatus status) {
        this.status = status;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }
}
