package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.class_management.repository.ScheduleRepository;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestion;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkQuestionOption;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkRubricCriterion;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeworkValidationService {

    private final ScheduleRepository scheduleRepository;

    public void validateTeacherAssignedToClass(Long clazzId, Long teacherId, Long centerId) {
        boolean isAssigned = scheduleRepository.existsByClazz_IdAndTeacherUser_IdAndCenter_Id(clazzId, teacherId, centerId);
        if (!isAssigned) {
            throw new BusinessRuleException("Teacher is not assigned to the specified class.");
        }
    }

    public void validateForPublish(HomeworkAssignment assignment) {
        if (!StringUtils.hasText(assignment.getHomeworkTemplate().getTitle())) {
            throw new BusinessRuleException("Homework title must not be blank to publish.");
        }
        if (assignment.getDueDate() == null) {
            throw new BusinessRuleException("Homework due date is required to publish.");
        }
        if (assignment.getDueDate().isBefore(Instant.now())) {
            throw new BusinessRuleException("Homework due date must be in the future.");
        }
        if (assignment.getHomeworkTemplate().getQuestions() == null || assignment.getHomeworkTemplate().getQuestions().isEmpty()) {
            throw new BusinessRuleException("Homework must have at least one question to publish.");
        }

        double totalMaxScore = 0.0;

        for (HomeworkQuestion question : assignment.getHomeworkTemplate().getQuestions()) {
            if (!StringUtils.hasText(question.getQuestionText())) {
                throw new BusinessRuleException("Question text must not be blank.");
            }
            if (question.getMaxScore() == null || question.getMaxScore() <= 0) {
                throw new BusinessRuleException("Question max score must be greater than 0.");
            }
            
            totalMaxScore += question.getMaxScore();

            if (question.getType() == HomeworkQuestionType.QUIZ) {
                validateQuizQuestion(question);
            } else if (question.getType() == HomeworkQuestionType.ESSAY) {
                validateEssayQuestion(question);
            }
        }

        if (assignment.getHomeworkTemplate().getMaxScore() == null || assignment.getHomeworkTemplate().getMaxScore() <= 0) {
            throw new BusinessRuleException("Homework max score must be greater than 0.");
        }
        if (Math.abs(assignment.getHomeworkTemplate().getMaxScore() - totalMaxScore) > 0.01) {
            throw new BusinessRuleException("Sum of questions' max scores does not equal homework max score.");
        }
    }

    private void validateQuizQuestion(HomeworkQuestion question) {
        Set<HomeworkQuestionOption> options = question.getOptions();
        if (options == null || options.size() < 2) {
            throw new BusinessRuleException("Quiz question must have at least 2 options.");
        }
        boolean hasCorrectOption = false;
        for (HomeworkQuestionOption option : options) {
            if (!StringUtils.hasText(option.getContent())) {
                throw new BusinessRuleException("Option content must not be blank.");
            }
            if (Boolean.TRUE.equals(option.getIsCorrect())) {
                hasCorrectOption = true;
            }
        }
        if (!hasCorrectOption) {
            throw new BusinessRuleException("Quiz question must have at least one correct option.");
        }
    }

    private void validateEssayQuestion(HomeworkQuestion question) {
        if (question.getRubric() == null) {
            throw new BusinessRuleException("Essay question must have a rubric.");
        }
        Set<HomeworkRubricCriterion> criteria = question.getRubric().getCriteria();
        if (criteria == null || criteria.isEmpty()) {
            throw new BusinessRuleException("Essay rubric must have at least one criterion.");
        }
        
        double totalCriteriaScore = 0.0;
        Set<String> criteriaNames = new HashSet<>();
        
        for (HomeworkRubricCriterion criterion : criteria) {
            if (!StringUtils.hasText(criterion.getName())) {
                throw new BusinessRuleException("Criterion name must not be blank.");
            }
            if (!criteriaNames.add(criterion.getName())) {
                throw new BusinessRuleException("Criterion names must be unique within the same rubric. Duplicate: " + criterion.getName());
            }
            if (criterion.getMaxScore() == null || criterion.getMaxScore() <= 0) {
                throw new BusinessRuleException("Criterion max score must be greater than 0.");
            }
            totalCriteriaScore += criterion.getMaxScore();
        }
        
        if (Math.abs(question.getMaxScore() - totalCriteriaScore) > 0.01) {
            throw new BusinessRuleException("Total rubric criteria score does not equal question max score.");
        }
    }
}
