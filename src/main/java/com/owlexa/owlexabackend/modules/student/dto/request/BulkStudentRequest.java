package com.owlexa.owlexabackend.modules.student.dto.request;
import lombok.Data;

import java.util.List;

@Data
public class BulkStudentRequest {

    private List<Item> students;

    @Data
    public static class Item {
        private String phoneNumber;
        private String fullName;
        private String email;
    }
}
