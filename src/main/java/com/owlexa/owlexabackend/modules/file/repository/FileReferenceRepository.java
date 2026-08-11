package com.owlexa.owlexabackend.modules.file.repository;

import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.entity.FileReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileReferenceRepository extends JpaRepository<FileReference, Long> {

    List<FileReference> findAllByOwnerTypeAndOwnerId(FileOwnerType ownerType, Long ownerId);

    boolean existsByFile_Id(Long fileId);
}
