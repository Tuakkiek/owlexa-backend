package com.owlexa.owlexabackend.modules.question_bank.controller;

import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionCreateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionUpdateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionResponse;
import com.owlexa.owlexabackend.modules.question_bank.service.QuestionCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
class QuestionCollectionControllerTest {

    @Mock
    private QuestionCollectionService collectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QuestionCollectionController(collectionService))
                .setValidator(validator)
                .build();
    }

    @Test
    void findAllReturnsUnpaginatedSidebarCollections() throws Exception {
        when(collectionService.findAll()).thenReturn(List.of(collectionResponse()));

        mockMvc.perform(get("/teacher/question-collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TOEIC_TEST_1"))
                .andExpect(jsonPath("$[0].questionCount").value(6));
    }

    @Test
    void controllerRequiresTeacherRoleAtEntryPoint() {
        PreAuthorize authorization =
                QuestionCollectionController.class.getAnnotation(PreAuthorize.class);

        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('TEACHER')");
    }

    @Test
    void findByIdUsesResourcePath() throws Exception {
        when(collectionService.findById(7L)).thenReturn(collectionResponse());

        mockMvc.perform(get("/teacher/question-collections/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(collectionService).findById(7L);
    }

    @Test
    void createReturns201AndDelegatesValidatedRequest() throws Exception {
        when(collectionService.create(any(QuestionCollectionCreateRequest.class)))
                .thenReturn(collectionResponse());

        mockMvc.perform(post("/teacher/question-collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "TOEIC_TEST_1",
                                  "name": "TOEIC Test 1",
                                  "description": "Practice test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TOEIC_TEST_1"));

        ArgumentCaptor<QuestionCollectionCreateRequest> captor =
                ArgumentCaptor.forClass(QuestionCollectionCreateRequest.class);
        verify(collectionService).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("TOEIC Test 1");
    }

    @Test
    void createRejectsStructurallyInvalidRequestBeforeService() throws Exception {
        mockMvc.perform(post("/teacher/question-collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "TOEIC Test 1"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(collectionService, never()).create(any());
    }

    @Test
    void updateReturns200AndDelegatesResourceId() throws Exception {
        when(collectionService.update(
                org.mockito.ArgumentMatchers.eq(7L),
                any(QuestionCollectionUpdateRequest.class)
        )).thenReturn(collectionResponse());

        mockMvc.perform(put("/teacher/question-collections/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Renamed TOEIC Test",
                                  "description": null
                                }
                                """))
                .andExpect(status().isOk());

        verify(collectionService).update(
                org.mockito.ArgumentMatchers.eq(7L),
                any(QuestionCollectionUpdateRequest.class)
        );
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/teacher/question-collections/7"))
                .andExpect(status().isNoContent());

        verify(collectionService).delete(7L);
    }

    private QuestionCollectionResponse collectionResponse() {
        return QuestionCollectionResponse.builder()
                .id(7L)
                .code("TOEIC_TEST_1")
                .name("TOEIC Test 1")
                .description("Practice test")
                .questionCount(6L)
                .build();
    }
}
