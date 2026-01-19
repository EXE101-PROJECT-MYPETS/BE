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
    private final FileStorageService  fileStorageService;

    public FileUploadUtil(SupabaseStorageService supabaseStorageService, FileStorageService fileStorageService) {
        this.supabaseStorageService = supabaseStorageService;
        this.fileStorageService = fileStorageService;
    }

    public String uploadUserAvatar(Long userId, MultipartFile avatarFile) {
        return supabaseStorageService.uploadPublic(avatarFile, "users/" + userId + "/avatar");
    }

    public String uploadUserAvatarLocal(Long userId, MultipartFile file) {
        return fileStorageService.save(file, "avatars", userId);
    }
}
