package com.owlexa.owlexabackend.modules.student_submission.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAnswerRequest {

    @NotNull(message = "Mã mục bài tập không được để trống")
    private Long assignmentItemId;

    private String answerText;

    private List<Long> selectedOptionIds;
}
