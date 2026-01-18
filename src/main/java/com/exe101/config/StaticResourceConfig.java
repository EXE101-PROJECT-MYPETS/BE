package com.exe101.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    private String avatarLocationUri;

    @PostConstruct
    void init() {
        Path avatarDir = Paths
                .get(uploadDir, "avatars")
                .toAbsolutePath()
                .normalize();
        avatarLocationUri = avatarDir.toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/EXE101/avatars/**")
                .addResourceLocations(avatarLocationUri)
                .setCachePeriod(3600);
    }
}
