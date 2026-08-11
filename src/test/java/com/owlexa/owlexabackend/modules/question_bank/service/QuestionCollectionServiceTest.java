package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.common.exception.DuplicateResourceException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionCreateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionCollectionUpdateRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionCollectionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.mapper.QuestionCollectionMapper;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionCollectionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionCollectionServiceTest {

    private static final Long CENTER_ID = 10L;
    private static final Long TEACHER_ID = 20L;
    private static final Long COLLECTION_ID = 30L;

    @Mock private QuestionCollectionRepository collectionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;

    private QuestionCollectionService service;
    private User teacher;
    private Center center;

    @BeforeEach
    void setUp() {
        service = new QuestionCollectionService(
                collectionRepository,
                questionRepository,
                centerRepository,
                membershipRepository,
                authorizationService,
                new QuestionCollectionMapper()
        );
        TenantContext.setCurrentTenantId(CENTER_ID);

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole(Role.TEACHER);
        center = new Center();
        center.setId(CENTER_ID);

        when(authorizationService.getCurrentUser()).thenReturn(teacher);
        when(membershipRepository.existsByUser_IdAndCenter_Id(TEACHER_ID, CENTER_ID))
                .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createNormalizesStableCodeAndDisplayFields() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(collectionRepository.saveAndFlush(any(QuestionCollection.class)))
                .thenAnswer(invocation -> {
                    QuestionCollection collection = invocation.getArgument(0);
                    collection.setId(COLLECTION_ID);
                    return collection;
                });

        QuestionCollectionResponse response = service.create(
                QuestionCollectionCreateRequest.builder()
                        .code(" toeic_test_1 ")
                        .name(" TOEIC Test 1 ")
                        .description(" Practice test ")
                        .build()
        );

        assertThat(response.getCode()).isEqualTo("TOEIC_TEST_1");
        assertThat(response.getName()).isEqualTo("TOEIC Test 1");
        assertThat(response.getDescription()).isEqualTo("Practice test");
        assertThat(response.getQuestionCount()).isZero();
    }

    @Test
    void createRejectsPermanentlyDuplicateCode() {
        when(collectionRepository.existsByCenter_IdAndCode(CENTER_ID, "TOEIC_TEST_1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                QuestionCollectionCreateRequest.builder()
                        .code("TOEIC_TEST_1")
                        .name("TOEIC Test 1")
                        .build()
        )).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void findAllUsesOneGroupedCountQuery() {
        QuestionCollection collection = activeCollection();
        QuestionRepository.CollectionQuestionCount count =
                org.mockito.Mockito.mock(QuestionRepository.CollectionQuestionCount.class);
        when(count.getCollectionId()).thenReturn(COLLECTION_ID);
        when(count.getQuestionCount()).thenReturn(6L);
        when(collectionRepository.findAllByCenter_IdAndDeletedAtIsNullOrderByNameAsc(CENTER_ID))
                .thenReturn(List.of(collection));
        when(questionRepository.countActiveByCollectionIds(List.of(COLLECTION_ID)))
                .thenReturn(List.of(count));

        List<QuestionCollectionResponse> response = service.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getQuestionCount()).isEqualTo(6L);
        verify(questionRepository).countActiveByCollectionIds(List.of(COLLECTION_ID));
    }

    @Test
    void updateKeepsCodeImmutable() {
        QuestionCollection collection = activeCollection();
        when(collectionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(COLLECTION_ID, CENTER_ID))
                .thenReturn(Optional.of(collection));
        when(collectionRepository.saveAndFlush(collection)).thenReturn(collection);
        when(questionRepository.countByCollection_IdAndDeletedAtIsNull(COLLECTION_ID))
                .thenReturn(2L);

        QuestionCollectionResponse response = service.update(
                COLLECTION_ID,
                QuestionCollectionUpdateRequest.builder()
                        .name("Renamed Test")
                        .description(null)
                        .build()
        );

        assertThat(response.getCode()).isEqualTo("TOEIC_TEST_1");
        assertThat(response.getName()).isEqualTo("Renamed Test");
        assertThat(response.getQuestionCount()).isEqualTo(2L);
    }

    @Test
    void deleteRejectsCollectionWithActiveQuestions() {
        QuestionCollection collection = activeCollection();
        when(collectionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(COLLECTION_ID, CENTER_ID))
                .thenReturn(Optional.of(collection));
        when(questionRepository.existsByCollection_IdAndDeletedAtIsNull(COLLECTION_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(COLLECTION_ID))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(collection.getDeletedAt()).isNull();
    }

    @Test
    void findByIdDoesNotResolveCollectionFromAnotherTenant() {
        when(collectionRepository.findByIdAndCenter_IdAndDeletedAtIsNull(COLLECTION_ID, CENTER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(COLLECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private QuestionCollection activeCollection() {
        return QuestionCollection.builder()
                .id(COLLECTION_ID)
                .center(center)
                .code("TOEIC_TEST_1")
                .name("TOEIC Test 1")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();
    }
}
