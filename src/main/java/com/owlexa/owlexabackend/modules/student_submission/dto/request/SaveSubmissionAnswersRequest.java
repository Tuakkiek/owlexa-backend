package com.owlexa.owlexabackend.modules.student_submission.dto.request;

import jakarta.validation.Valid;
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
public class SaveSubmissionAnswersRequest {

    @Valid
    @NotNull(message = "Danh sách câu trả lời không được để trống")
    private List<SubmissionAnswerRequest> answers;
}
