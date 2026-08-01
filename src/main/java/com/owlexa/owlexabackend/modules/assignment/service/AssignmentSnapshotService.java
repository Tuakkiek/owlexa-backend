package com.owlexa.owlexabackend.modules.assignment.service;

import com.owlexa.owlexabackend.modules.assessment_builder.entity.Assessment;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentItem;
import com.owlexa.owlexabackend.modules.assessment_builder.entity.PlaybackMode;
import com.owlexa.owlexabackend.modules.assessment_builder.exception.AssessmentDocumentIntegrityException;
import com.owlexa.owlexabackend.modules.assignment.entity.Assignment;
import com.owlexa.owlexabackend.modules.assignment.entity.AssignmentItem;
import com.owlexa.owlexabackend.modules.assignment.mapper.AssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentSnapshotService {

    private final AssignmentMapper assignmentMapper;

    public void rebuildSnapshot(Assignment assignment, Instant snapshotAt) {
        Assessment assessment = assignment.getAssessment();
        if (assessment == null) {
            throw new AssessmentDocumentIntegrityException("Assignment snapshot source is missing");
        }

        assignment.getItems().clear();
        assignment.getBlocks().clear();

        java.util.Map<Long, com.owlexa.owlexabackend.modules.assignment.entity.AssignmentContentBlock> blockMap = new java.util.HashMap<>();
        if (assessment.getBlocks() != null && !assessment.getBlocks().isEmpty()) {
            for (com.owlexa.owlexabackend.modules.assessment_builder.entity.AssessmentContentBlock ab : assessment.getBlocks()) {
                com.owlexa.owlexabackend.modules.assignment.entity.AssignmentContentBlock asb =
                        com.owlexa.owlexabackend.modules.assignment.entity.AssignmentContentBlock.builder()
                                .assignment(assignment)
                                .assessmentBlock(ab)
                                .position(ab.getPosition())
                                .title(ab.getTitle())
                                .contentJson(ab.getContentJson())
                                .build();
                assignment.getBlocks().add(asb);
                if (ab.getId() != null) {
                    blockMap.put(ab.getId(), asb);
                }
            }
        }

        orderedAssessmentItems(assessment).stream()
                .map(item -> {
                    AssignmentItem ai = assignmentMapper.toItemSnapshot(item);
                    if (item.getBlock() != null && item.getBlock().getId() != null) {
                        ai.setBlock(blockMap.get(item.getBlock().getId()));
                    }
                    return ai;
                })
                .forEach(item -> attachItem(assignment, item));

        assignment.setAudioFile(assessment.getAudioFile());
        assignment.setPlaybackMode(
                assessment.getPlaybackMode() == null ? PlaybackMode.PRACTICE : assessment.getPlaybackMode()
        );
        assignment.setContentJson(assessment.getContentJson());
        assignment.setAssessmentSnapshotAt(snapshotAt);
    }

    private List<AssessmentItem> orderedAssessmentItems(Assessment assessment) {
        return assessment.getItems().stream()
                .sorted(Comparator.comparing(AssessmentItem::getDisplayOrder))
                .toList();
    }

    private void attachItem(Assignment assignment, AssignmentItem item) {
        item.setAssignment(assignment);
        assignment.getItems().add(item);
    }
}
