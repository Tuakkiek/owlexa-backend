package com.owlexa.owlexabackend.modules.question_bank.controller;

import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionSummaryResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportResultResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportValidationResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.service.QuestionImportService;
import com.owlexa.owlexabackend.modules.question_bank.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock private QuestionService questionService;
    @Mock private QuestionImportService questionImportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionController(questionService, questionImportService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void findAllBindsSearchFiltersSortAndPagination() throws Exception {
        PageRequest expectedPageable = PageRequest.of(
                2,
                10,
                Sort.by(Sort.Order.asc("displayOrder"))
        );
        when(questionService.findAll(
                eq("meeting"),
                eq(7L),
                eq("PART_2"),
                eq(QuestionType.MULTIPLE_CHOICE),
                eq(QuestionDifficulty.EASY),
                eq(9L),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(questionResponse()), expectedPageable, 21));

        mockMvc.perform(get("/teacher/questions")
                        .param("search", "meeting")
                        .param("collectionId", "7")
                        .param("sectionCode", "PART_2")
                        .param("type", "MULTIPLE_CHOICE")
                        .param("difficulty", "EASY")
                        .param("gradingCriteriaId", "9")
                        .param("page", "2")
                        .param("size", "10")
                        .param("sort", "displayOrder,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sectionCode").value("PART_2"))
                .andExpect(jsonPath("$.totalElements").value(21));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(questionService).findAll(
                eq("meeting"),
                eq(7L),
                eq("PART_2"),
                eq(QuestionType.MULTIPLE_CHOICE),
                eq(QuestionDifficulty.EASY),
                eq(9L),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("displayOrder")).isNotNull();
    }

    @Test
    void controllerRequiresQuestionBankPermissionAtWriteEntryPoint() throws NoSuchMethodException {
        PreAuthorize authorization = QuestionController.class
                .getMethod("create", QuestionRequest.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAuthority('TEACHER_QUESTION_BANK')");
    }

    @Test
    void findSectionCodesUsesRequiredCollectionFilter() throws Exception {
        when(questionService.findSectionCodes(7L))
                .thenReturn(List.of("PART_1", "PART_2"));

        mockMvc.perform(get("/teacher/questions/section-codes")
                        .param("collectionId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("PART_1"))
                .andExpect(jsonPath("$[1]").value("PART_2"));
    }

    @Test
    void findSectionCodesRejectsMissingCollectionIdBeforeService() throws Exception {
        mockMvc.perform(get("/teacher/questions/section-codes"))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).findSectionCodes(any());
    }

    @Test
    void createReturns201ForValidQuestionContract() throws Exception {
        when(questionService.create(any(QuestionRequest.class)))
                .thenReturn(questionResponse());

        mockMvc.perform(post("/teacher/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validQuestionJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collection.code").value("TOEIC_TEST_1"))
                .andExpect(jsonPath("$.displayOrder").value(5));

        verify(questionService).create(any(QuestionRequest.class));
    }

    @Test
    void createRejectsMissingCollectionMetadataBeforeService() throws Exception {
        mockMvc.perform(post("/teacher/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "MULTIPLE_CHOICE"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(questionService, never()).create(any());
    }

    @Test
    void updateReturns200AndDelegatesQuestionId() throws Exception {
        when(questionService.update(eq(11L), any(QuestionRequest.class)))
                .thenReturn(questionResponse());

        mockMvc.perform(put("/teacher/questions/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validQuestionJson()))
                .andExpect(status().isOk());

        verify(questionService).update(eq(11L), any(QuestionRequest.class));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/teacher/questions/11"))
                .andExpect(status().isNoContent());

        verify(questionService).delete(11L);
    }

    @Test
    void bulkDeleteReturns204() throws Exception {
        mockMvc.perform(post("/teacher/questions/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionIds": [11, 12]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(questionService).deleteMany(List.of(11L, 12L));
    }

    @Test
    void validateImportReturns200() throws Exception {
        when(questionImportService.validate(eq(7L), any()))
                .thenReturn(QuestionImportValidationResponse.builder()
                        .version("2.0")
                        .collectionId(7L)
                        .collectionName("TOEIC Test 1")
                        .collectionCode("TOEIC_TEST_1")
                        .questionCount(1)
                        .questions(List.of())
                        .build());

        mockMvc.perform(post("/teacher/questions/import/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.collectionId").value(7))
                .andExpect(jsonPath("$.collectionCode").value("TOEIC_TEST_1"));
    }

    @Test
    void importQuestionsReturns201() throws Exception {
        when(questionImportService.importQuestions(eq(7L), any()))
                .thenReturn(QuestionImportResultResponse.builder()
                        .importedCount(1)
                        .questions(List.of(questionResponse()))
                        .build());

        mockMvc.perform(post("/teacher/questions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedCount").value(1));
    }

    private QuestionResponse questionResponse() {
        return QuestionResponse.builder()
                .id(11L)
                .questionCode("Q-000011")
                .collection(QuestionCollectionSummaryResponse.builder()
                        .id(7L)
                        .code("TOEIC_TEST_1")
                        .name("TOEIC Test 1")
                        .build())
                .sectionCode("PART_2")
                .displayOrder(5)
                .type(QuestionType.MULTIPLE_CHOICE)
                .difficulty(QuestionDifficulty.EASY)
                .build();
    }

    private String validQuestionJson() {
        return """
                {
                  "collectionId": 7,
                  "sectionCode": "PART_2",
                  "displayOrder": 5,
                  "type": "MULTIPLE_CHOICE",
                  "difficulty": "EASY",
                  "points": 1,
                  "options": [
                    {
                      "content": "A",
                      "isCorrect": true,
                      "displayOrder": 1
                    },
                    {
                      "content": "B",
                      "isCorrect": false,
                      "displayOrder": 2
                    }
                  ]
                }
                """;
    }

    private String importJson() {
        return """
                {
                  "collectionId": 7,
                  "json": "{\\"version\\":\\"2.0\\",\\"questions\\":[]}"
                }
                """;
    }
}
