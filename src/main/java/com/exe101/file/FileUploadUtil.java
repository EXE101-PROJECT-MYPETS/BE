package com.exe101.file;

import com.exe101.common.supabase.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class FileUploadUtil {

    private final SupabaseStorageService supabaseStorageService;

    public FileUploadUtil(SupabaseStorageService supabaseStorageService) {
        this.supabaseStorageService = supabaseStorageService;
    }

    public String uploadUserAvatar(Long userId, MultipartFile avatarFile) {
        return supabaseStorageService.uploadPublic(avatarFile, "users/" + userId + "/avatar");
    }

    @Value("${app.storage.root}")
    private String storageRoot;

    @Value("${app.storage.public-base-url:/EXE101}")
    private String publicBaseUrl;

    public String uploadUserAvatarLocal(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh (image/*).");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "avatar" : file.getOriginalFilename());
        String ext = getExt(original); // .jpg .png ...
        String filename = UUID.randomUUID() + ext;

        Path userDir = Path.of(storageRoot, "avatars", String.valueOf(userId)); // D:/EXE101/avatars/12
        try {
            Files.createDirectories(userDir);

            Path target = userDir.resolve(filename);
            // overwrite nếu trùng tên (hầu như không vì UUID)
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Trả về URL để FE render
            return publicBaseUrl + "/avatars/" + userId + "/" + filename; // /EXE101/avatars/12/xxx.jpg
        } catch (IOException e) {
            throw new RuntimeException("Upload avatar thất bại", e);
        }
    }

    private String getExt(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = filename.substring(dot).toLowerCase();
        // chặn mấy ext lạ (tuỳ bạn)
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".webp" -> ext;
            default -> "";
        };
    }
}
