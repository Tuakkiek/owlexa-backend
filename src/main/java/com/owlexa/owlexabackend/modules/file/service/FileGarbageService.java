package com.owlexa.owlexabackend.modules.file.service;

import com.owlexa.owlexabackend.modules.file.config.FileStorageProperties;
import com.owlexa.owlexabackend.modules.file.entity.FileStatus;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.repository.FileReferenceRepository;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileGarbageService {

    private final StoredFileRepository storedFileRepository;
    private final FileReferenceRepository fileReferenceRepository;
    private final FileStorageProperties properties;

    /**
     * Marks abandoned uploads only. Physical deletion is intentionally not
     * scheduled, so recovery remains possible during the retention window.
     */
    @Scheduled(cron = "${owlexa.files.orphan-scan-cron:0 30 3 * * *}")
    @Transactional
    public void markAbandonedUploads() {
        Instant cutoff = Instant.now().minus(properties.getOrphanRetentionDays(), ChronoUnit.DAYS);
        List<StoredFile> candidates =
                storedFileRepository.findAllByStatusAndCreatedAtBefore(FileStatus.TEMPORARY, cutoff);
        Instant now = Instant.now();
        for (StoredFile file : candidates) {
            if (!fileReferenceRepository.existsByFile_Id(file.getId())) {
                file.setStatus(FileStatus.ORPHANED);
                file.setOrphanedAt(now);
            }
        }
        storedFileRepository.saveAll(candidates);
    }

    /**
     * Query boundary for a future operator-approved physical cleanup job.
     */
    @Transactional(readOnly = true)
    public List<StoredFile> findPurgeCandidates(Instant cutoff) {
        return storedFileRepository.findAllByStatusAndOrphanedAtBefore(FileStatus.ORPHANED, cutoff);
    }
}
