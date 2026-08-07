package com.owlexa.owlexabackend.modules.document.repository;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {
    List<StudentDocument> findAllByClazz_IdAndCenter_IdOrderByCreatedAtDesc(Long clazzId, Long centerId);

    long countByClazz_IdAndCenter_Id(Long clazzId, Long centerId);

    void deleteByCenter_Id(Long centerId);
}
