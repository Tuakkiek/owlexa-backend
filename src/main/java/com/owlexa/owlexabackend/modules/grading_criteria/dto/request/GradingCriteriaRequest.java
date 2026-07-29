package com.owlexa.owlexabackend.modules.grading_criteria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingCriteriaRequest {

    @NotBlank(message = "Tên tiêu chí chấm không được để trống")
    @Size(max = 255, message = "Tên tiêu chí chấm không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Nội dung tiêu chí chấm không được để trống")
    private JsonNode content;
}
