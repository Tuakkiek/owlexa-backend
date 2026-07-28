package com.owlexa.owlexabackend.modules.file.repository;

import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import com.owlexa.owlexabackend.modules.file.entity.FileStatus;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByIdAndCenter_IdAndDeletedAtIsNull(Long id, Long centerId);

    List<StoredFile> findAllByIdInAndCenter_IdAndDeletedAtIsNull(Collection<Long> ids, Long centerId);

    List<StoredFile> findAllByStatusAndCreatedAtBefore(FileStatus status, Instant cutoff);

    List<StoredFile> findAllByStatusAndOrphanedAtBefore(FileStatus status, Instant cutoff);
}
