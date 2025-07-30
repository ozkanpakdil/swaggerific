package io.github.ozkanpakdil.swaggerific.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for {@link CredentialEncryption} class. These tests verify the encryption and decryption functionality
 * works correctly.
 */
class CredentialEncryptionTest {

    @Test
    void testEncryptDecryptRoundTrip() {
        // Given
        String originalValue = "testPassword123";

        // When
        String encrypted = CredentialEncryption.encrypt(originalValue);
        String decrypted = CredentialEncryption.decrypt(encrypted);

        // Then
        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(originalValue, encrypted, "Encrypted value should be different from original");
        assertEquals(originalValue, decrypted, "Decrypted value should match original");
    }

    @Test
    void testEncryptDecryptWithEmptyString() {
        // Given
        String originalValue = "";

        // When
        String encrypted = CredentialEncryption.encrypt(originalValue);
        String decrypted = CredentialEncryption.decrypt(encrypted);

        // Then
        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(originalValue, encrypted, "Encrypted value should be different from original");
        assertEquals(originalValue, decrypted, "Decrypted value should match original");
    }

    @Test
    void testEncryptDecryptWithSpecialCharacters() {
        // Given
        String originalValue = "P@$$w0rd!#%^&*()_+<>?:\"{}|~`";

        // When
        String encrypted = CredentialEncryption.encrypt(originalValue);
        String decrypted = CredentialEncryption.decrypt(encrypted);

        // Then
        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(originalValue, encrypted, "Encrypted value should be different from original");
        assertEquals(originalValue, decrypted, "Decrypted value should match original");
    }

    @Test
    void testEncryptDecryptWithLongString() {
        // Given
        String originalValue = "abcdefghijklmnopqrstuvwxyz".repeat(1000);

        // When
        String encrypted = CredentialEncryption.encrypt(originalValue);
        String decrypted = CredentialEncryption.decrypt(encrypted);

        // Then
        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(originalValue, encrypted, "Encrypted value should be different from original");
        assertEquals(originalValue, decrypted, "Decrypted value should match original");
    }

    @Test
    void testEncryptWithNull() {
        // When
        String encrypted = CredentialEncryption.encrypt(null);

        // Then
        assertNull(encrypted, "Encrypted null should be null");
    }

    @Test
    void testDecryptWithNull() {
        // When
        String decrypted = CredentialEncryption.decrypt(null);

        // Then
        assertNull(decrypted, "Decrypted null should be null");
    }

    @Test
    void testMultipleEncryptionsOfSameValueAreDifferent() {
        // Given
        String originalValue = "testPassword123";

        // When
        String encrypted1 = CredentialEncryption.encrypt(originalValue);
        String encrypted2 = CredentialEncryption.encrypt(originalValue);

        // Then
        assertNotEquals(encrypted1, encrypted2, "Multiple encryptions of the same value should be different due to random IV");
        assertEquals(originalValue, CredentialEncryption.decrypt(encrypted1), "First encryption should decrypt correctly");
        assertEquals(originalValue, CredentialEncryption.decrypt(encrypted2), "Second encryption should decrypt correctly");
    }

    @Test
    void testDecryptInvalidValue() {
        // Given
        String invalidEncrypted = "ThisIsNotAValidEncryptedString";

        // When & Then
        assertThrows(RuntimeException.class, () -> CredentialEncryption.decrypt(invalidEncrypted),
                "Decrypting an invalid value should throw an exception");
    }
}

