package com.owlexa.owlexabackend.modules.file.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(
        prefix = "owlexa.files",
        name = "storage",
        havingValue = "LOCAL",
        matchIfMissing = true
)
public class LocalFileWebConfig implements WebMvcConfigurer {

    private final FileStorageProperties properties;

    public LocalFileWebConfig(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.getLocalRoot())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(86_400);
    }
}
