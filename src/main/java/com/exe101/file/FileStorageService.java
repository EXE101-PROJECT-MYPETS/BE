package com.exe101.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * @param file     file upload
     * @param module   avatars, shops, pets...
     * @param ownerId  userId
     */
    public String save(
            MultipartFile file,
            String module,
            Long ownerId
    ) {
        try {
            Path baseDir = Paths.get(uploadDir, module, ownerId.toString());
            Files.createDirectories(baseDir);

            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(name -> name.contains("."))
                    .map(name -> name.substring(name.lastIndexOf(".")))
                    .orElse("");

            String fileName = UUID.randomUUID() + ext;
            Path target = baseDir.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // URL public cho FE
            return "/" + uploadDir + "/" + module + "/" + ownerId + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file", e);
        }
    }
}
