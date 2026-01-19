package com.exe101.common.supabase.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadPublic(MultipartFile file, String folder) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File rỗng");
            }

            String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
            String safeOriginal = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = UUID.randomUUID() + "-" + safeOriginal;

            String safeFolder = (folder == null || folder.isBlank()) ? "uploads" : folder.trim();
            String path = safeFolder + "/" + fileName;
            String encodedPath = encodePath(path);

            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + encodedPath;

            String contentType = resolveContentType(file);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceKey);
            headers.set("apikey", serviceKey);
            headers.set("Content-Type", contentType);
            headers.set("x-upsert", "true");

            HttpEntity<byte[]> req = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> res = restTemplate.exchange(uploadUrl, HttpMethod.POST, req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase upload lỗi: " + res.getStatusCode());
            }

            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encodedPath;

        } catch (Exception e) {
            throw new RuntimeException("Upload Supabase thất bại: " + e.getMessage(), e);
        }
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank()) return ct;

        String name = file.getOriginalFilename();
        if (name == null) return "image/jpeg";

        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";

        throw new IllegalArgumentException("File không phải ảnh hợp lệ: " + name);
    }

    private String encodePath(String path) {
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("/");
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
