package com.owlexa.owlexabackend.modules.file.service;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.file.entity.FileOwnerType;
import com.owlexa.owlexabackend.modules.file.entity.FileReference;
import com.owlexa.owlexabackend.modules.file.entity.FileStatus;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.repository.FileReferenceRepository;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileReferenceService {

    private final StoredFileRepository storedFileRepository;
    private final FileReferenceRepository fileReferenceRepository;

    @Transactional
    public void syncDocumentReferences(
            FileOwnerType ownerType,
            Long ownerId,
            Long centerId,
            JsonNode document
    ) {
        syncReferences(ownerType, ownerId, centerId, List.of(document));
    }

    @Transactional
    public void syncReferences(
            FileOwnerType ownerType,
            Long ownerId,
            Long centerId,
            Collection<JsonNode> documents
    ) {
        syncReferences(ownerType, ownerId, centerId, documents, Set.of());
    }

    @Transactional
    public void syncReferences(
            FileOwnerType ownerType,
            Long ownerId,
            Long centerId,
            Collection<JsonNode> documents,
            Collection<Long> explicitFileIds
    ) {
        Set<Long> requestedIds = new HashSet<>();
        for (JsonNode document : documents) {
            if (document != null && !document.isNull()) {
                requestedIds.addAll(extractAndValidateFileIds(document));
            }
        }
        if (explicitFileIds != null) {
            explicitFileIds.stream()
                    .filter(fileId -> fileId != null && fileId > 0)
                    .forEach(requestedIds::add);
        }
        List<FileReference> existingReferences =
                fileReferenceRepository.findAllByOwnerTypeAndOwnerId(ownerType, ownerId);
        Map<Long, FileReference> existingByFileId = existingReferences.stream()
                .collect(Collectors.toMap(reference -> reference.getFile().getId(), Function.identity()));

        List<StoredFile> requestedFiles = requestedIds.isEmpty()
                ? List.of()
                : storedFileRepository.findAllByIdInAndCenter_IdAndDeletedAtIsNull(requestedIds, centerId);
        if (requestedFiles.size() != requestedIds.size()) {
            throw new BadRequestException("Editor document contains an invalid or unauthorized file");
        }

        Instant now = Instant.now();
        Set<Long> removedIds = new HashSet<>(existingByFileId.keySet());
        removedIds.removeAll(requestedIds);

        List<FileReference> removedReferences = existingReferences.stream()
                .filter(reference -> removedIds.contains(reference.getFile().getId()))
                .toList();
        fileReferenceRepository.deleteAll(removedReferences);
        fileReferenceRepository.flush();

        for (StoredFile file : requestedFiles) {
            if (file.getStatus() == FileStatus.DELETED) {
                throw new BadRequestException("Deleted file cannot be referenced");
            }
            if (!existingByFileId.containsKey(file.getId())) {
                fileReferenceRepository.save(FileReference.builder()
                        .file(file)
                        .center(file.getCenter())
                        .ownerType(ownerType)
                        .ownerId(ownerId)
                        .build());
            }
            file.setStatus(FileStatus.ACTIVE);
            file.setLastReferencedAt(now);
            file.setOrphanedAt(null);
        }

        for (FileReference removed : removedReferences) {
            StoredFile file = removed.getFile();
            if (!fileReferenceRepository.existsByFile_Id(file.getId())) {
                file.setStatus(FileStatus.ORPHANED);
                file.setOrphanedAt(now);
            }
        }
        storedFileRepository.saveAll(requestedFiles);
    }

    public Set<Long> extractAndValidateFileIds(JsonNode document) {
        if (document == null || !document.isObject() || !"doc".equals(document.path("type").asText())) {
            throw new BadRequestException("Editor content must be a valid ProseMirror document");
        }
        Set<Long> fileIds = new HashSet<>();
        collectFileIds(document, fileIds);
        return fileIds;
    }

    private void collectFileIds(JsonNode node, Set<Long> fileIds) {
        JsonNode attrs = node.path("attrs");
        JsonNode fileId = attrs.path("fileId");
        if (!fileId.isMissingNode() && !fileId.isNull()) {
            if (!fileId.canConvertToLong() || fileId.asLong() <= 0) {
                throw new BadRequestException("Editor content contains an invalid file id");
            }
            fileIds.add(fileId.asLong());
        }
        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode child : content) {
                collectFileIds(child, fileIds);
            }
        }
    }
}
