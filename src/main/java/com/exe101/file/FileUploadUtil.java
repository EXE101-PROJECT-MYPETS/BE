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
        return supabaseStorageService.uploadPublic(avatarFile, "users/" + userId + "/avatar");
    }

    public String uploadProductImage(Long shopId, Long productId, MultipartFile imageFile) {
        return supabaseStorageService.uploadPublicPath(
                imageFile,
                "shops/" + shopId + "/products/" + productId + "/images"
        );
    }

    public String uploadServiceImage(Long shopId, Long serviceId, MultipartFile imageFile) {
        return supabaseStorageService.uploadPublicPath(
                imageFile,
                "shops/" + shopId + "/services/" + serviceId + "/images"
        );
    }

    public String uploadPetAvatar(Long userId, Long petId, MultipartFile avatarFile) {
        return supabaseStorageService.uploadPublic(
                avatarFile,
                "users/" + userId + "/pets/" + petId + "/avatar"
        );
    }

    public String normalizePetAvatarPath(String imageUrl) {
        return supabaseStorageService.toPublicUrl(imageUrl);
    }

    public String normalizeProductImagePath(String imageUrl) {
        return supabaseStorageService.toPublicUrl(imageUrl);
    }

    public String normalizeServiceImagePath(String imageUrl) {
        return supabaseStorageService.toPublicUrl(imageUrl);
    }
}
