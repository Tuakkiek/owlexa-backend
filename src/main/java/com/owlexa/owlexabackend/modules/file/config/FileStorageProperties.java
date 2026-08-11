package com.owlexa.owlexabackend.modules.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "owlexa.files")
public class FileStorageProperties {

    private long maxSize = 2_147_483_648L;
    private String storage = "LOCAL";
    private String localRoot = "uploads";
    private String publicBaseUrl = "http://localhost:8081/uploads";
    private int orphanRetentionDays = 7;
    private S3 s3 = new S3();

    @Data
    public static class S3 {
        private String bucket;
        private String region = "ap-southeast-1";
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String publicBaseUrl;
    }
}
