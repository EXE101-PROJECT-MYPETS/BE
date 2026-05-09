package com.exe101.file;

import com.exe101.common.supabase.service.SupabaseStorageService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUploadUtil {

    private final SupabaseStorageService supabaseStorageService;

    public FileUploadUtil(SupabaseStorageService supabaseStorageService) {
        this.supabaseStorageService = supabaseStorageService;
    }

    public String uploadUserAvatar(Long userId, MultipartFile avatarFile) {
        return supabaseStorageService.uploadPublicPath(avatarFile, "users/" + userId + "/avatar");
    }

    public String uploadProductImage(Long shopId, Long productId, MultipartFile imageFile) {
        return supabaseStorageService.uploadPublicPath(
                imageFile,
                "shops/" + shopId + "/products/" + productId + "/images"
        );
    }

    public String normalizeProductImagePath(String imageUrl) {
        return supabaseStorageService.extractPublicObjectPath(imageUrl);
    }
}
