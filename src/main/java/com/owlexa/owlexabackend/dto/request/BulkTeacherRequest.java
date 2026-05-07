package com.owlexa.owlexabackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class BulkTeacherRequest {

    private List<Item> teachers;

    public List<Item> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Item> teachers) {
        this.teachers = teachers;
    }

    public static class Item {
        private String phoneNumber;
        private String fullName;
        private String email;

        public String getPhoneNumber() { return phoneNumber; }

        public String getFullName() {return fullName;}

        public String getEmail() {return email;}

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
