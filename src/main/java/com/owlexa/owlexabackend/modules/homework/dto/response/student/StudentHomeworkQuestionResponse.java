package com.owlexa.owlexabackend.modules.homework.dto.response.student;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import lombok.Data;

import java.util.List;

@Data
public class StudentHomeworkQuestionResponse {
    private Long id;
    private HomeworkQuestionType type;
    private String questionText;
    private String attachedImageUrl;
    private String attachedAudioUrl;
    private String attachedFileUrl;
    private Integer sortOrder;
    private Double maxScore;
    
    private List<StudentHomeworkOptionResponse> options;
    private StudentHomeworkRubricResponse rubric;
}
