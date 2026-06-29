package com.owlexa.owlexabackend.modules.student.dto.response;
public class BulkStudentError {

    private Integer row;
    private String phoneNumber;
    private String status;
    private String message;

    public Integer getRow() {
        return row;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
