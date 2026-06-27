package com.owlexa.owlexabackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherClassStudentsResponse {
    private Long id;
    private String className;
    private Long studentCount;
    private List<StudentResponse> students;
}
