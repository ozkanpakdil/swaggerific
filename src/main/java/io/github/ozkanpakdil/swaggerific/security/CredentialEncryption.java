package io.github.ozkanpakdil.swaggerific.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

public class CredentialEncryption {
    private static final String ALGORITHM = "AES";
    private static final byte[] SALT = "SwaggerificSalt".getBytes(StandardCharsets.UTF_8);
    private static SecretKey secretKey;

    static {
        try {
            // Generate a key based on system-specific properties
            String systemSpecific = System.getProperty("user.name") +
                                  System.getProperty("os.name") +
                                  System.getProperty("user.home");

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(systemSpecific.toCharArray(), SALT, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            secretKey = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }

    public static String encrypt(String value) {
        if (value == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
