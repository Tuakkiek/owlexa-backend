package com.owlexa.owlexabackend.modules.document.controller;
import com.owlexa.owlexabackend.modules.document.dto.request.StudentDocumentRequest;
import com.owlexa.owlexabackend.modules.document.dto.response.StudentDocumentResponse;
import com.owlexa.owlexabackend.modules.document.service.StudentDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudentDocumentController {

    private final StudentDocumentService studentDocumentService;

    // ── Student: own documents ───────────────────────────────────────────────

    @GetMapping("/student/documents")
    public List<StudentDocumentResponse> findMyDocuments() {
        return studentDocumentService.findMyDocuments();
    }

    // ── Owner: class documents ───────────────────────────────────────────────

    @GetMapping("/owner/classes/{classId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public List<StudentDocumentResponse> findClassDocuments(@PathVariable Long classId) {
        return studentDocumentService.findClassDocuments(classId);
    }

    @PostMapping("/owner/classes/{classId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    public StudentDocumentResponse createForClass(
            @PathVariable Long classId,
            @RequestBody StudentDocumentRequest request
    ) {
        return studentDocumentService.createForClass(classId, request);
    }

    @DeleteMapping("/owner/classes/{classId}/documents/{documentId}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    public void deleteForClass(
            @PathVariable Long classId,
            @PathVariable Long documentId
    ) {
        studentDocumentService.deleteDocumentForClass(classId, documentId);
    }

    // ── Teacher: class documents ─────────────────────────────────────────────

    @GetMapping("/teacher/classes/{classId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_VIEW')")
    public List<StudentDocumentResponse> findClassDocumentsAsTeacher(@PathVariable Long classId) {
        return studentDocumentService.findClassDocumentsAsTeacher(classId);
    }

    @PostMapping("/teacher/classes/{classId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    public StudentDocumentResponse createForClassAsTeacher(
            @PathVariable Long classId,
            @RequestBody StudentDocumentRequest request
    ) {
        return studentDocumentService.createForClassAsTeacher(classId, request);
    }

    @DeleteMapping("/teacher/classes/{classId}/documents/{documentId}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    public void deleteForClassAsTeacher(
            @PathVariable Long classId,
            @PathVariable Long documentId
    ) {
        studentDocumentService.deleteDocumentForClassAsTeacher(classId, documentId);
    }
}
