package com.owlexa.owlexabackend.integration.question_bank;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.integration.BaseIntegrationTest;
import com.owlexa.owlexabackend.modules.question_bank.entity.Question;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionCollectionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionRepository;
import com.owlexa.owlexabackend.modules.question_bank.repository.QuestionSpecifications;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.repository.UserRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QuestionCollectionRepository collectionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        questionRepository.deleteAllInBatch();
        collectionRepository.deleteAllInBatch();
        centerRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void repositoriesKeepAggregateQueriesSeparateAndFetchQuestionListRelations() {
        User teacher = userRepository.save(user("0910000001", Role.TEACHER));
        User owner = userRepository.save(user("0910000002", Role.OWNER));

        Center center = new Center();
        center.setName("Question Repository Center");
        center.setSubdomain("question-repository-center");
        center.setOwner(owner);
        center = centerRepository.save(center);
        TenantContext.setCurrentTenantId(center.getId());

        QuestionCollection collection = collectionRepository.save(QuestionCollection.builder()
                .center(center)
                .code("TOEIC_REPOSITORY_TEST")
                .name("Repository Collection")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build());

        Question question = questionRepository.save(Question.builder()
                .center(center)
                .collection(collection)
                .sectionCode("PART_1")
                .displayOrder(1)
                .type(QuestionType.ESSAY)
                .questionCode("Q-REPOSITORY-TEST")
                .contentJson("{\"type\":\"doc\",\"content\":[]}")
                .createdBy(teacher)
                .updatedBy(teacher)
                .build());

        QuestionCollection foundCollection = collectionRepository
                .findByCodeAndCenter_IdAndDeletedAtIsNull("TOEIC_REPOSITORY_TEST", center.getId())
                .orElseThrow();
        assertThat(foundCollection.getId()).isEqualTo(collection.getId());
        assertThat(questionRepository.existsByCollection_IdAndDeletedAtIsNull(collection.getId()))
                .isTrue();

        Page<Question> result = questionRepository.findAll(
                QuestionSpecifications.search(
                        center.getId(),
                        "Repository Collection",
                        collection.getId(),
                        "PART_1",
                        null,
                        null,
                        null
                ),
                PageRequest.of(0, 20)
        );

        Question listed = result.getContent().get(0);
        assertThat(listed.getId()).isEqualTo(question.getId());
        assertThat(Hibernate.isInitialized(listed.getCollection())).isTrue();

        Question detail = questionRepository
                .findByIdAndCenter_IdAndDeletedAtIsNull(question.getId(), center.getId())
                .orElseThrow();
        assertThat(Hibernate.isInitialized(detail.getCollection())).isTrue();
        assertThat(Hibernate.isInitialized(detail.getOptions())).isTrue();
    }

    private User user(String phoneNumber, Role role) {
        User user = new User();
        user.setPhoneNumber(phoneNumber);
        user.setFullName(role.name() + " Repository User");
        user.setPassword("unused");
        user.setRole(role);
        return user;
    }
}
