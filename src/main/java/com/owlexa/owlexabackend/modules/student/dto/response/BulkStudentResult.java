package com.owlexa.owlexabackend.modules.student.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkStudentResult {

    private String phoneNumber;
    private String status;
    private String temporaryPassword;

}
