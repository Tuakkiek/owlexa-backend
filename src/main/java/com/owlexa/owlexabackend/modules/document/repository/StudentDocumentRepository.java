package com.owlexa.owlexabackend.modules.document.repository;
import com.owlexa.owlexabackend.modules.document.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {
    List<StudentDocument> findAllByClazzIdAndCenterIdOrderByUploadedAtDesc(Long clazzId, Long centerId);

    void deleteByCenterId(Long centerId);
}
