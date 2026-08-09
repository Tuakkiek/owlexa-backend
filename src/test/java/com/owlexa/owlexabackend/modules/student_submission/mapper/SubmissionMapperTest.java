package com.owlexa.owlexabackend.modules.student_submission.mapper;

import com.owlexa.owlexabackend.common.richtext.RichTextDocumentService;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentRecipient;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.StudentAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.SubmissionAttemptItemResponse;
import com.owlexa.owlexabackend.modules.student_submission.dto.response.TeacherAttemptDetailResponse;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttempt;
import com.owlexa.owlexabackend.modules.student_submission.entity.SubmissionAttemptStatus;
import com.owlexa.owlexabackend.modules.user.entity.User;
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
class SubmissionMapperTest {

    @Mock
    private RichTextDocumentService richTextDocumentService;

    @Mock
    private FileMapper fileMapper;

    private SubmissionMapper submissionMapper;

    @BeforeEach
    void setUp() {
        submissionMapper = new SubmissionMapper(richTextDocumentService, fileMapper);
    }

    @Test
    @DisplayName("toStudentAttemptDetailResponse maps questionId in StudentAttemptItemResponse")
    void toStudentAttemptDetailResponse_MapsQuestionId() {
        Question question = Question.builder().id(777L).build();
        AssessmentItem assessmentItem = AssessmentItem.builder().id(666L).question(question).build();

        AssignmentItem item = AssignmentItem.builder()
                .id(202L)
                .assessmentItem(assessmentItem)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title("Student Q")
                .contentJson("{\"type\":\"doc\"}")
                .points(new BigDecimal("1.00"))
                .displayOrder(1)
                .build();

        Assignment assignment = Assignment.builder()
                .id(15L)
                .items(List.of(item))
                .build();

        User studentUser = new User();
        studentUser.setId(100L);
        studentUser.setFullName("Student A");

        AssignmentRecipient recipient = AssignmentRecipient.builder()
                .id(30L)
                .assignment(assignment)
                .studentUser(studentUser)
                .build();

        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .id(500L)
                .assignmentRecipient(recipient)
                .status(SubmissionAttemptStatus.IN_PROGRESS)
                .attemptNumber(1)
                .answers(List.of())
                .build();

        JsonNode docNode = mock(JsonNode.class);
        org.mockito.Mockito.lenient().when(richTextDocumentService.deserialize(org.mockito.ArgumentMatchers.any())).thenReturn(docNode);

        StudentAttemptDetailResponse response = submissionMapper.toStudentAttemptDetailResponse(attempt);

        assertThat(response.getItems()).hasSize(1);
        StudentAttemptItemResponse studentItem = response.getItems().get(0);
        assertThat(studentItem.getAssignmentItemId()).isEqualTo(202L);
        assertThat(studentItem.getQuestionId()).isEqualTo(777L);
    }

    @Test
    @DisplayName("toTeacherAttemptDetailResponse maps questionId in SubmissionAttemptItemResponse")
    void toTeacherAttemptDetailResponse_MapsQuestionId() {
        Question question = Question.builder().id(777L).build();
        AssessmentItem assessmentItem = AssessmentItem.builder().id(666L).question(question).build();

        AssignmentItem item = AssignmentItem.builder()
                .id(202L)
                .assessmentItem(assessmentItem)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .title("Teacher Q")
                .contentJson("{\"type\":\"doc\"}")
                .points(new BigDecimal("1.00"))
                .displayOrder(1)
                .build();

        Assignment assignment = Assignment.builder()
                .id(15L)
                .items(List.of(item))
                .build();

        User studentUser = new User();
        studentUser.setId(100L);
        studentUser.setFullName("Student A");

        AssignmentRecipient recipient = AssignmentRecipient.builder()
                .id(30L)
                .assignment(assignment)
                .studentUser(studentUser)
                .build();

        SubmissionAttempt attempt = SubmissionAttempt.builder()
                .id(500L)
                .assignmentRecipient(recipient)
                .status(SubmissionAttemptStatus.SUBMITTED)
                .attemptNumber(1)
                .answers(List.of())
                .build();

        JsonNode docNode = mock(JsonNode.class);
        org.mockito.Mockito.lenient().when(richTextDocumentService.deserialize(org.mockito.ArgumentMatchers.any())).thenReturn(docNode);

        TeacherAttemptDetailResponse response = submissionMapper.toTeacherAttemptDetailResponse(attempt);

        assertThat(response.getItems()).hasSize(1);
        SubmissionAttemptItemResponse teacherItem = response.getItems().get(0);
        assertThat(teacherItem.getAssignmentItemId()).isEqualTo(202L);
        assertThat(teacherItem.getQuestionId()).isEqualTo(777L);
    }
}
