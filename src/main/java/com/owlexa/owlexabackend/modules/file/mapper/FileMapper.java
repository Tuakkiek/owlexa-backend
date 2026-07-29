package com.owlexa.owlexabackend.modules.file.mapper;

import com.owlexa.owlexabackend.modules.file.dto.FileResponse;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    public FileResponse toResponse(StoredFile file) {
        return FileResponse.builder()
                .id(file.getId())
                .originalName(file.getOriginalName())
                .url(file.getUrl())
                .mimeType(file.getMimeType())
                .type(file.getFileType())
                .extension(file.getExtension())
                .size(file.getSize())
                .status(file.getStatus())
                .createdAt(file.getCreatedAt())
                .build();
    }
}
