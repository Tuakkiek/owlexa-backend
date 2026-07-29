package com.owlexa.owlexabackend.modules.file.storage;

import com.owlexa.owlexabackend.modules.file.config.FileStorageProperties;
import com.owlexa.owlexabackend.modules.file.entity.StorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(
        prefix = "owlexa.files",
        name = "storage",
        havingValue = "LOCAL",
        matchIfMissing = true
)
public class LocalFileStorage implements FileStorage {

    private final FileStorageProperties properties;
    private final Path root;

    public LocalFileStorage(FileStorageProperties properties) throws IOException {
        this.properties = properties;
        this.root = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public StoredObject store(String key, InputStream content, long contentLength, String contentType)
            throws IOException {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid storage key");
        }
        Files.createDirectories(target.getParent());
        // No replace option: an unexpected UUID collision fails instead of overwriting.
        Files.copy(content, target);

        String baseUrl = trimTrailingSlash(properties.getPublicBaseUrl());
        return new StoredObject(key.replace('\\', '/'), baseUrl + "/" + key.replace('\\', '/'), StorageProvider.LOCAL);
    }

    @Override
    public void delete(String path) throws IOException {
        Path target = root.resolve(path).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Invalid storage path");
        }
        Files.deleteIfExists(target);
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
