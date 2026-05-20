package de.gkvtransmitter.util.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptionKeyManager {

    private static final String ENV_KEY_NAME = "GKV_ENCRYPTION_KEY";
    private static final String KEY_DIRECTORY = ".gkvtransmitter";
    private static final String KEY_FILE_NAME = "encryption.key";
    private static final int AES_256_KEY_LENGTH = 32;

    private EncryptionKeyManager() {
    }

    public static SecretKey loadKey() {
        String envKey = System.getenv(ENV_KEY_NAME);
        if (envKey != null && !envKey.isBlank()) {
            return new SecretKeySpec(normalizeKeyMaterial(envKey.trim()), "AES");
        }

        Path keyPath = Paths.get(System.getProperty("user.home"), KEY_DIRECTORY, KEY_FILE_NAME);
        try {
            if (Files.exists(keyPath)) {
                String fileKey = Files.readString(keyPath, StandardCharsets.UTF_8).trim();
                return new SecretKeySpec(Base64.getDecoder().decode(fileKey), "AES");
            }

            Files.createDirectories(keyPath.getParent());
            byte[] keyBytes = new byte[AES_256_KEY_LENGTH];
            new SecureRandom().nextBytes(keyBytes);
            String encoded = Base64.getEncoder().encodeToString(keyBytes);
            Files.writeString(keyPath, encoded, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            restrictPermissions(keyPath);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize encryption key", e);
        }
    }

    private static byte[] normalizeKeyMaterial(String keyMaterial) {
        try {
            byte[] decoded = Base64.getDecoder().decode(keyMaterial);
            if (decoded.length == AES_256_KEY_LENGTH) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to digest mode below.
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create key digest", e);
        }
    }

    private static void restrictPermissions(Path keyPath) {
        try {
            Set<PosixFilePermission> permissions = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(keyPath, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Ignore on filesystems without POSIX permission support.
        }
    }
}
