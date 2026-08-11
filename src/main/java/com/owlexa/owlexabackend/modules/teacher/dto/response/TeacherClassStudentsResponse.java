package com.owlexa.owlexabackend.modules.teacher.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.owlexa.owlexabackend.modules.student.dto.response.StudentResponse;
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
