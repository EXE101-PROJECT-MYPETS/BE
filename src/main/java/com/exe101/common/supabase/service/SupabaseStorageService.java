package com.exe101.common.supabase.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
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
        UploadTarget uploadTarget = upload(file, folder);
        return buildPublicUrl(uploadTarget.encodedPath());
    }

    public String uploadPublicPath(MultipartFile file, String folder) {
        return upload(file, folder).path();
    }

    public String extractPublicObjectPath(String urlOrPath) {
        if (urlOrPath == null) {
            return null;
        }

        String normalized = urlOrPath.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return normalized;
        }

        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        String publicPrefix = buildPublicPrefix();
        if (normalized.startsWith(publicPrefix)) {
            return normalized.substring(publicPrefix.length());
        }

        String publicPathMarker = "/storage/v1/object/public/" + bucket + "/";
        int markerIndex = normalized.indexOf(publicPathMarker);
        if (markerIndex >= 0) {
            return normalized.substring(markerIndex + publicPathMarker.length());
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                URI uri = URI.create(normalized);
                String uriPath = uri.getPath();
                normalized = uriPath == null ? normalized : uriPath;
            } catch (IllegalArgumentException ignored) {
            }
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        String bucketPrefix = bucket + "/";
        if (normalized.startsWith(bucketPrefix)) {
            return normalized.substring(bucketPrefix.length());
        }

        String uploadsPrefix = "uploads/";
        if (normalized.startsWith(uploadsPrefix)) {
            return normalized.substring(uploadsPrefix.length());
        }

        return normalized;
    }

    private UploadTarget upload(MultipartFile file, String folder) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Tệp tải lên đang trống");
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
                throw new RuntimeException("Tải tệp lên Supabase thất bại: " + res.getStatusCode());
            }

            return new UploadTarget(path, encodedPath);

        } catch (Exception e) {
            throw new RuntimeException("Tải tệp lên Supabase thất bại: " + e.getMessage(), e);
        }
    }

    private String buildPublicUrl(String encodedPath) {
        return buildPublicPrefix() + encodedPath;
    }

    /**
     * Convert a stored object URL or path into a full public URL clients can use.
     */
    public String toPublicUrl(String urlOrPath) {
        if (urlOrPath == null) return null;
        String path = extractPublicObjectPath(urlOrPath);
        if (path == null || path.isBlank()) return path;
        String encoded = encodePath(path);
        return buildPublicPrefix() + encoded;
    }

    private String buildPublicPrefix() {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/";
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank() && !ct.equals("application/octet-stream")) {
            return ct;
        }

        String name = file.getOriginalFilename();
        if (name == null) return "image/jpeg";

        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";

        return "image/jpeg";
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

    private record UploadTarget(String path, String encodedPath) {
    }
}
