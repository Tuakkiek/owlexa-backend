package com.owlexa.owlexabackend.dto.response;

import com.owlexa.owlexabackend.entity.BulkTeacherStatus;

public class BulkTeacherError {

    private Integer row;
    private String phoneNumber;
    private BulkTeacherStatus status;
    private String message;

    public Integer getRow() {return row;}

    public String getPhoneNumber() {return phoneNumber;}

    public BulkTeacherStatus getStatus() {return status;}

    public String getMessage() {return message;}

    public void setRow(Integer row) {
        this.row = row;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStatus(BulkTeacherStatus status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
