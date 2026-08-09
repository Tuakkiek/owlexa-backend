package com.owlexa.owlexabackend.modules.assignment.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentDetailResponse;
import com.owlexa.owlexabackend.modules.assignment.dto.response.AssignmentItemResponse;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentMapperTest {

    @Mock
    private RichTextDocumentService richTextDocumentService;

    @Mock
    private FileMapper fileMapper;

    private AssignmentMapper assignmentMapper;

    @BeforeEach
    void setUp() {
        assignmentMapper = new AssignmentMapper(richTextDocumentService, fileMapper);
    }

    @Test
    @DisplayName("toDetailResponse maps questionId from source AssessmentItem Question")
    void toDetailResponse_MapsQuestionId() {
        Question question = Question.builder().id(999L).build();
        AssessmentItem assessmentItem = AssessmentItem.builder().id(888L).question(question).build();

        AssignmentItem assignmentItem = AssignmentItem.builder()
                .id(101L)
                .assessmentItem(assessmentItem)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title("Sample Q")
                .contentJson("{\"type\":\"doc\"}")
                .points(new BigDecimal("2.50"))
                .displayOrder(1)
                .build();

        com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment assessment =
                com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment.builder().id(50L).build();

        Assignment assignment = Assignment.builder()
                .id(10L)
                .assessment(assessment)
                .title("Test Assignment")
                .items(List.of(assignmentItem))
                .build();

        JsonNode docNode = mock(JsonNode.class);
        org.mockito.Mockito.lenient().when(richTextDocumentService.deserialize(org.mockito.ArgumentMatchers.any())).thenReturn(docNode);

        AssignmentDetailResponse response = assignmentMapper.toDetailResponse(assignment);

        assertThat(response.getItems()).hasSize(1);
        AssignmentItemResponse itemResp = response.getItems().get(0);
        assertThat(itemResp.getId()).isEqualTo(101L);
        assertThat(itemResp.getAssessmentItemId()).isEqualTo(888L);
        assertThat(itemResp.getQuestionId()).isEqualTo(999L);
    }
}
