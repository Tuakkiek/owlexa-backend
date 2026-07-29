package com.owlexa.owlexabackend.modules.file.storage;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.file.config.FileStorageProperties;
import com.owlexa.owlexabackend.modules.file.entity.StorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Component
@ConditionalOnProperty(prefix = "owlexa.files", name = "storage", havingValue = "S3")
public class S3FileStorage implements FileStorage {

    private final FileStorageProperties.S3 properties;
    private final S3Client client;

    public S3FileStorage(FileStorageProperties fileProperties) {
        this.properties = fileProperties.getS3();
        validateConfiguration();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ));
        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
            builder.forcePathStyle(true);
        }
        this.client = builder.build();
    }

    @Override
    public StoredObject store(String key, InputStream content, long contentLength, String contentType)
            throws IOException {
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .contentType(contentType)
                            .contentLength(contentLength)
                            .build(),
                    RequestBody.fromInputStream(content, contentLength)
            );
            return new StoredObject(key, buildPublicUrl(key), StorageProvider.S3);
        } catch (RuntimeException exception) {
            throw new IOException("Could not store object", exception);
        }
    }

    @Override
    public void delete(String path) throws IOException {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(path)
                    .build());
        } catch (RuntimeException exception) {
            throw new IOException("Could not delete object", exception);
        }
    }

    private String buildPublicUrl(String key) {
        if (hasText(properties.getPublicBaseUrl())) {
            return properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + key;
        }
        if (hasText(properties.getEndpoint())) {
            return properties.getEndpoint().replaceAll("/+$", "")
                    + "/" + properties.getBucket() + "/" + key;
        }
        return "https://" + properties.getBucket() + ".s3."
                + properties.getRegion() + ".amazonaws.com/" + key;
    }

    private void validateConfiguration() {
        if (!hasText(properties.getBucket())
                || !hasText(properties.getAccessKey())
                || !hasText(properties.getSecretKey())) {
            throw new BadRequestException("S3 file storage configuration is incomplete");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
