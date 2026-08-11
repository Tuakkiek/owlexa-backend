package com.owlexa.owlexabackend.integration.question_bank;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.integration.BaseIntegrationTest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionCollectionRepository;
import com.owlexa.owlexabackend.modules.question_bank.service.QuestionService;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Membership;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static com.owlexa.owlexabackend.support.RichTextTestFixtures.document;
import static org.assertj.core.api.Assertions.assertThat;

class QuestionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionCollectionRepository collectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private Long collectionId;

    @AfterEach
    void cleanUp() {
        questionRepository.deleteAllInBatch();
        collectionRepository.deleteAllInBatch();
        membershipRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Question lifecycle persists final questionCode and preserves it across detail, update, and search")
    void questionLifecycle_shouldPersistFinalQuestionCodeAndPreserveIt() {
        Center center = seedTeacherInCenter();
        TenantContext.setCurrentTenantId(center.getId());

        QuestionResponse created = questionService.create(questionRequest(
                "Do you agree or disagree with online learning?"
        ));

        assertThat(created.getQuestionCode()).matches("Q-\\d{6,}");
        assertThat(created.getQuestionCode()).doesNotStartWith("TMP-");

        Question persisted = questionRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getQuestionCode()).isEqualTo(created.getQuestionCode());
        assertThat(persisted.getQuestionCode()).doesNotStartWith("TMP-");

        QuestionResponse detail = questionService.findById(created.getId());
        assertThat(detail.getQuestionCode()).isEqualTo(created.getQuestionCode());

        QuestionResponse updated = questionService.update(created.getId(), questionRequest(
                "Explain whether classroom learning is better than online learning."
        ));
        assertThat(updated.getQuestionCode()).isEqualTo(created.getQuestionCode());

        Question reloaded = questionRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getQuestionCode()).isEqualTo(created.getQuestionCode());

        Page<QuestionResponse> searchResult = questionService.findAll(
                "classroom learning",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );
        assertThat(searchResult.getContent())
                .singleElement()
                .extracting(QuestionResponse::getQuestionCode)
                .isEqualTo(created.getQuestionCode());
    }

    private Center seedTeacherInCenter() {
        User owner = userRepository.save(user("0900000000", Role.OWNER));

        Center center = new Center();
        center.setName("Question Bank Integration Center");
        center.setSubdomain("question-bank-integration");
        center.setOwner(owner);
        center = centerRepository.save(center);

        User teacher = userRepository.save(user("0900000001", Role.TEACHER));

        Membership membership = new Membership();
        membership.setCenter(center);
        membership.setUser(teacher);
        membership.setJoinedByUser(owner);
        membershipRepository.save(membership);

        QuestionCollection collection = QuestionCollection.builder()
                .center(center)
                .code("INTEGRATION_TEST")
                .name("Integration Test")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build();
        collectionId = collectionRepository.save(collection).getId();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(teacher.getPhoneNumber(), null, List.of())
        );

        return center;
    }

    private User user(String phoneNumber, Role role) {
        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setFullName(role.name() + " User");
        user.setPassword("unused");
        user.setRole(role);
        return user;
    }

    private QuestionRequest questionRequest(String content) {
        return QuestionRequest.builder()
                .collectionId(collectionId)
                .sectionCode("WRITING")
                .displayOrder(1)
                .type(QuestionType.ESSAY)
                .content(document(content))
                .difficulty(QuestionDifficulty.MEDIUM)
                .points(BigDecimal.TEN)
                .build();
    }
}
