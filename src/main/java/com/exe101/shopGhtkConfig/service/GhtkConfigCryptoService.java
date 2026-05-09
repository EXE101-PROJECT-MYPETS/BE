package com.exe101.shopGhtkConfig.service;

import com.exe101.shopGhtkConfig.exception.ShopGhtkConfigValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class GhtkConfigCryptoService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${ghtk.config.encryption-key:}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(ByteBuffer
                    .allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array());
        } catch (GeneralSecurityException ex) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkTokenEncryptFailed",
                    "Không thể mã hóa token GHTK"
            );
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("Dữ liệu mã hoá không hợp lệ");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkTokenDecryptFailed",
                    "Không thể giải mã token GHTK. Vui lòng kiểm tra khóa mã hóa"
            );
        }
    }

    private SecretKeySpec buildSecretKey() {
        String rawKey = encryptionKey != null ? encryptionKey.trim() : "";
        if (rawKey.isBlank()) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkEncryptionKeyMissing",
                    "Chưa cấu hình khóa mã hóa GHTK_CONFIG_ENCRYPTION_KEY"
            );
        }

        try {
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException ex) {
            throw new ShopGhtkConfigValidationException(
                    "GhtkEncryptionUnavailable",
                    "Không thể khởi tạo mã hóa token GHTK"
            );
        }
    }
}
