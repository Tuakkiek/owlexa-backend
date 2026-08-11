package com.owlexa.owlexabackend.modules.file.service;

import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.entity.FileStatus;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.repository.FileReferenceRepository;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileReferenceServiceTest {

    @Mock private StoredFileRepository storedFileRepository;
    @Mock private FileReferenceRepository fileReferenceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractFileIds_findsEveryMediaNodeRecursively() {
        FileReferenceService service =
                new FileReferenceService(storedFileRepository, fileReferenceRepository);
        JsonNode document = objectMapper.readTree("""
                {
                  "type":"doc",
                  "content":[
                    {"type":"image","attrs":{"fileId":1}},
                    {"type":"blockquote","content":[
                      {"type":"audio","attrs":{"fileId":2}},
                      {"type":"video","attrs":{"fileId":3}},
                      {"type":"pdfAttachment","attrs":{"fileId":4}},
                      {"type":"fileAttachment","attrs":{"fileId":5}}
                    ]}
                  ]
                }
                """);

        Set<Long> ids = service.extractAndValidateFileIds(document);

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void syncReferences_activatesTemporaryFileAndCreatesReference() {
        FileReferenceService service =
                new FileReferenceService(storedFileRepository, fileReferenceRepository);
        Center center = new Center();
        center.setId(10L);
        StoredFile file = StoredFile.builder()
                .id(15L)
                .center(center)
                .status(FileStatus.TEMPORARY)
                .build();
        JsonNode document = objectMapper.readTree("""
                {"type":"doc","content":[{"type":"image","attrs":{"fileId":15}}]}
                """);

        when(fileReferenceRepository.findAllByOwnerTypeAndOwnerId(
                FileOwnerType.ASSESSMENT, 30L)).thenReturn(List.of());
        when(storedFileRepository.findAllByIdInAndCenter_IdAndDeletedAtIsNull(
                Set.of(15L), 10L)).thenReturn(List.of(file));

        service.syncDocumentReferences(FileOwnerType.ASSESSMENT, 30L, 10L, document);

        assertThat(file.getStatus()).isEqualTo(FileStatus.ACTIVE);
        assertThat(file.getLastReferencedAt()).isNotNull();
        ArgumentCaptor<com.owlexa.owlexabackend.modules.file.entity.FileReference> captor =
                ArgumentCaptor.forClass(
                        com.owlexa.owlexabackend.modules.file.entity.FileReference.class);
        verify(fileReferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getFile().getId()).isEqualTo(15L);
        assertThat(captor.getValue().getOwnerId()).isEqualTo(30L);
        verify(storedFileRepository).saveAll(any());
    }
}
