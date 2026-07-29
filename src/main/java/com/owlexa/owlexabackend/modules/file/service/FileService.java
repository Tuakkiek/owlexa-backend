package com.owlexa.owlexabackend.modules.file.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.common.exception.ResourceNotFoundException;
import com.owlexa.owlexabackend.modules.file.config.FileStorageProperties;
import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.file.entity.FileStatus;
import com.owlexa.owlexabackend.modules.file.entity.FileType;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.file.repository.FileReferenceRepository;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import com.owlexa.owlexabackend.modules.file.storage.FileStorage;
import com.owlexa.owlexabackend.modules.file.storage.StoredObject;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> GENERIC_CLIENT_MIME_TYPES = Set.of(
            "application/octet-stream",
            "binary/octet-stream"
    );

    private static final Map<String, AllowedFile> ALLOWED_FILES = Map.ofEntries(
            Map.entry("png", allowed(FileType.IMAGE, "image/png")),
            Map.entry("jpg", allowed(FileType.IMAGE, "image/jpeg")),
            Map.entry("jpeg", allowed(FileType.IMAGE, "image/jpeg")),
            Map.entry("gif", allowed(FileType.IMAGE, "image/gif")),
            Map.entry("webp", allowed(FileType.IMAGE, "image/webp")),
            Map.entry("mp3", allowed(FileType.AUDIO, "audio/mpeg", "audio/mp3")),
            Map.entry("wav", allowed(FileType.AUDIO, "audio/wav", "audio/x-wav", "audio/vnd.wave")),
            Map.entry("m4a", allowed(FileType.AUDIO, "audio/mp4", "audio/x-m4a", "video/mp4")),
            Map.entry("ogg", allowed(FileType.AUDIO, "audio/ogg", "application/ogg")),
            Map.entry("mp4", allowed(FileType.VIDEO, "video/mp4")),
            Map.entry("webm", allowed(FileType.VIDEO, "video/webm")),
            Map.entry("pdf", allowed(FileType.PDF, "application/pdf")),
            Map.entry("doc", allowed(FileType.ATTACHMENT, "application/msword")),
            Map.entry("docx", allowed(FileType.ATTACHMENT,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/zip")),
            Map.entry("xls", allowed(FileType.ATTACHMENT,
                    "application/vnd.ms-excel", "application/x-tika-msoffice")),
            Map.entry("xlsx", allowed(FileType.ATTACHMENT,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/zip")),
            Map.entry("ppt", allowed(FileType.ATTACHMENT,
                    "application/vnd.ms-powerpoint", "application/x-tika-msoffice")),
            Map.entry("pptx", allowed(FileType.ATTACHMENT,
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/zip")),
            Map.entry("zip", allowed(FileType.ATTACHMENT,
                    "application/zip", "application/x-zip-compressed")),
            Map.entry("rar", allowed(FileType.ATTACHMENT,
                    "application/vnd.rar", "application/x-rar-compressed"))
    );

    private final StoredFileRepository storedFileRepository;
    private final FileReferenceRepository fileReferenceRepository;
    private final CenterRepository centerRepository;
    private final AuthorizationService authorizationService;
    private final FileStorage fileStorage;
    private final FileStorageProperties properties;
    private final FileMapper fileMapper;

    @Transactional
    public FileResponse upload(MultipartFile multipartFile) {
        validateBasicFile(multipartFile);
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = requiredCenterId();
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found with id: " + centerId));

        String originalName = normalizeOriginalName(multipartFile.getOriginalFilename());
        String extension = extensionOf(originalName);
        AllowedFile allowed = ALLOWED_FILES.get(extension);
        if (allowed == null) {
            throw new BadRequestException("File extension is not allowed");
        }

        String detectedMimeType = detectMimeType(multipartFile, originalName);
        if (!allowed.mimeTypes().contains(detectedMimeType)) {
            throw new BadRequestException("File content does not match its extension");
        }
        validateDeclaredMimeType(multipartFile.getContentType(), allowed);

        String storedName = UUID.randomUUID() + "." + extension;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String storageKey = "%04d/%02d/%s".formatted(today.getYear(), today.getMonthValue(), storedName);
        StoredObject storedObject;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            storedObject = fileStorage.store(
                    storageKey,
                    inputStream,
                    multipartFile.getSize(),
                    detectedMimeType
            );
        } catch (IOException exception) {
            throw new BadRequestException("Could not store uploaded file");
        }

        StoredFile file = StoredFile.builder()
                .center(center)
                .originalName(originalName)
                .storedName(storedName)
                .mimeType(detectedMimeType)
                .fileType(allowed.type())
                .extension(extension)
                .size(multipartFile.getSize())
                .path(storedObject.path())
                .url(storedObject.url())
                .storageProvider(storedObject.provider())
                .status(FileStatus.TEMPORARY)
                .uploadedBy(currentUser)
                .build();
        return fileMapper.toResponse(storedFileRepository.save(file));
    }

    @Transactional(readOnly = true)
    public FileResponse findById(Long fileId) {
        return fileMapper.toResponse(findCurrentCenterFile(fileId));
    }

    @Transactional
    public void delete(Long fileId) {
        StoredFile file = findCurrentCenterFile(fileId);
        if (fileReferenceRepository.existsByFile_Id(fileId)) {
            throw new BadRequestException("File is still referenced by editor content");
        }
        file.setStatus(FileStatus.DELETED);
        file.setDeletedAt(Instant.now());
        file.setOrphanedAt(file.getOrphanedAt() == null ? Instant.now() : file.getOrphanedAt());
        storedFileRepository.save(file);
    }

    private StoredFile findCurrentCenterFile(Long fileId) {
        return storedFileRepository.findByIdAndCenter_IdAndDeletedAtIsNull(fileId, requiredCenterId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
    }

    private void validateBasicFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > properties.getMaxSize()) {
            throw new BadRequestException("File exceeds maximum allowed size");
        }
    }

    private String detectMimeType(MultipartFile file, String originalName) {
        try (InputStream inputStream = file.getInputStream()) {
            return new Tika().detect(inputStream, originalName).toLowerCase(Locale.ROOT);
        } catch (IOException exception) {
            throw new BadRequestException("Could not inspect uploaded file");
        }
    }

    private void validateDeclaredMimeType(String declaredMimeType, AllowedFile allowed) {
        if (declaredMimeType == null || declaredMimeType.isBlank()) {
            return;
        }
        String normalized = declaredMimeType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        if (!GENERIC_CLIENT_MIME_TYPES.contains(normalized) && !allowed.mimeTypes().contains(normalized)) {
            throw new BadRequestException("Declared MIME type does not match file content");
        }
    }

    private String normalizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("Original file name is required");
        }
        String normalized = Path.of(originalName.replace('\\', '/')).getFileName().toString().trim();
        if (normalized.isBlank() || normalized.length() > 512 || normalized.indexOf('\0') >= 0) {
            throw new BadRequestException("Invalid original file name");
        }
        return normalized;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1 || dotIndex == fileName.length() - 1) {
            throw new BadRequestException("File extension is required");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private Long requiredCenterId() {
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved");
        }
        return centerId;
    }

    private static AllowedFile allowed(FileType type, String... mimeTypes) {
        return new AllowedFile(type, Set.of(mimeTypes));
    }

    private record AllowedFile(FileType type, Set<String> mimeTypes) {
    }
}
