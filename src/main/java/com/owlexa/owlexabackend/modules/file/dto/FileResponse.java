package com.owlexa.owlexabackend.modules.file.dto;

import com.owlexa.owlexabackend.modules.file.entity.FileStatus;
import com.owlexa.owlexabackend.modules.file.entity.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private Long id;
    private String originalName;
    private String url;
    private String mimeType;
    private FileType type;
    private String extension;
    private Long size;
    private FileStatus status;
    private Instant createdAt;
}
