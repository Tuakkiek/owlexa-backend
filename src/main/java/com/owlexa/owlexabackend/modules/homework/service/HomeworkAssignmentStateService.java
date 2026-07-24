package com.owlexa.owlexabackend.modules.homework.service;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class HomeworkAssignmentStateService {

    /**
     * Validates and performs the transition of a HomeworkAssignment to the target status.
     * Throws BusinessRuleException if the transition is not allowed.
     */
    public void transitionTo(HomeworkAssignment assignment, HomeworkAssignmentStatus targetStatus, java.time.Clock clock) {
        HomeworkAssignmentStatus currentStatus = assignment.getStatus();
        
        if (currentStatus == targetStatus) {
            return;
        }

        switch (targetStatus) {
            case SCHEDULED:
                assignment.schedule(clock);
                break;
                
            case OPEN:
                assignment.open(clock);
                break;
                
            case CLOSED:
                assignment.close(clock);
                break;
                
            case ARCHIVED:
                assignment.archive(clock);
                break;
                
            case CANCELLED:
                assignment.cancel(clock);
                break;
                
            case DRAFT:
                if (currentStatus != HomeworkAssignmentStatus.SCHEDULED) {
                    throw new BusinessRuleException("Cannot revert to DRAFT from current state.");
                }
                assignment.setStatus(HomeworkAssignmentStatus.DRAFT);
                break;
        }

        log.info("Transitioning assignment {} from {} to {}", assignment.getId(), currentStatus, targetStatus);
    }

    /**
     * Validates timeline logic: availableFrom < dueDate < closeAt.
     */
    public void validateTimeline(Instant availableFrom, Instant dueDate, Instant closeAt, java.time.Clock clock) {
        if (availableFrom == null || dueDate == null || closeAt == null) {
            throw new BusinessRuleException("Dates cannot be null for scheduled assignments.");
        }
        
        if (!availableFrom.isBefore(dueDate)) {
            throw new BusinessRuleException("availableFrom must be before dueDate.");
        }
        
        if (!dueDate.isBefore(closeAt)) {
            throw new BusinessRuleException("dueDate must be before closeAt.");
        }
        
        if (closeAt.isBefore(Instant.now(clock))) {
            throw new BusinessRuleException("closeAt cannot be in the past.");
        }
    }
}
