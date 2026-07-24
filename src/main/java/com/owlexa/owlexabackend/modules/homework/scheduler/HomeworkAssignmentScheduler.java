package com.owlexa.owlexabackend.modules.homework.scheduler;

import com.owlexa.owlexabackend.modules.homework.entity.HomeworkAssignment;
import com.owlexa.owlexabackend.modules.homework.enums.HomeworkAssignmentStatus;
import com.owlexa.owlexabackend.modules.homework.repository.HomeworkAssignmentRepository;
import com.owlexa.owlexabackend.modules.homework.service.HomeworkAssignmentStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkAssignmentScheduler {

    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkAssignmentStateService stateService;
    private final java.time.Clock clock;

    // TODO: Ideally, we should fetch center-specific timezones if multi-tenant timezone is supported.
    // For now, we use the system default which is configured globally (e.g. Asia/Ho_Chi_Minh).
    
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void processAssignmentTransitions() {
        log.info("Running HomeworkAssignment status transition scheduler...");
        
        Instant now = Instant.now(clock);

        // 1. Transition SCHEDULED -> OPEN
        List<HomeworkAssignment> scheduledAssignments = homeworkAssignmentRepository.findAllByStatus(HomeworkAssignmentStatus.SCHEDULED);
        for (HomeworkAssignment assignment : scheduledAssignments) {
            if (assignment.getAvailableFrom() != null && !assignment.getAvailableFrom().isAfter(now)) {
                try {
                    stateService.transitionTo(assignment, HomeworkAssignmentStatus.OPEN, clock);
                    homeworkAssignmentRepository.save(assignment);
                } catch (Exception e) {
                    log.error("Failed to transition assignment {} to OPEN", assignment.getId(), e);
                }
            }
        }

        // 2. Transition OPEN -> CLOSED
        List<HomeworkAssignment> openAssignments = homeworkAssignmentRepository.findAllByStatus(HomeworkAssignmentStatus.OPEN);
        for (HomeworkAssignment assignment : openAssignments) {
            if (assignment.getCloseAt() != null && !assignment.getCloseAt().isAfter(now)) {
                try {
                    stateService.transitionTo(assignment, HomeworkAssignmentStatus.CLOSED, clock);
                    homeworkAssignmentRepository.save(assignment);
                } catch (Exception e) {
                    log.error("Failed to transition assignment {} to CLOSED", assignment.getId(), e);
                }
            }
        }
    }
}
