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

    public String uploadShopImage(Long shopId, MultipartFile imageFile) {
        return toUploadsPath(supabaseStorageService.uploadPublicPath(
                imageFile,
                "shops/" + shopId + "/avatar"
        ));
    }

    public String uploadShopCoverImage(Long shopId, MultipartFile imageFile) {
        return toUploadsPath(supabaseStorageService.uploadPublicPath(
                imageFile,
                "shops/" + shopId + "/cover_img"
        ));
    }

    public String normalizeProductImagePath(String imageUrl) {
        return supabaseStorageService.toPublicUrl(imageUrl);
    }

    public String normalizeServiceImagePath(String imageUrl) {
        return supabaseStorageService.toPublicUrl(imageUrl);
    }

    public String normalizeShopImagePath(String imageUrl) {
        return supabaseStorageService.toPublicUrl(imageUrl);
    }

    public String normalizeShopImageStoragePath(String imageUrl) {
        return toUploadsPath(supabaseStorageService.extractPublicObjectPath(imageUrl));
    }

    private String toUploadsPath(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return objectPath;
        }
        String normalized = objectPath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("uploads/")) {
            return "/" + normalized;
        }
        return "/uploads/" + normalized;
    }
}
