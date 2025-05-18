package io.github.ozkanpakdil.swaggerific.tools;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages proxy settings for the application. This class handles loading, saving, and applying proxy settings.
 */
public class ProxySettings {
    private static final Logger log = LoggerFactory.getLogger(ProxySettings.class);
    private static final Preferences userPrefs = Preferences.userNodeForPackage(SwaggerApplication.class);

    // Preference keys
    private static final String USE_SYSTEM_PROXY = "useSystemProxy";
    private static final String PROXY_TYPE = "proxyType";
    private static final String PROXY_SERVER = "proxyServer";
    private static final String PROXY_PORT = "proxyPort";
    private static final String PROXY_AUTH = "proxyAuth";
    private static final String PROXY_AUTH_USERNAME = "proxyAuthUsername";
    private static final String PROXY_AUTH_PASSWORD = "proxyAuthPassword";
    private static final String PROXY_BYPASS = "proxyBypass";
    private static final String DISABLE_SSL_VALIDATION = "disableSslValidation";

    // Default values
    private static final boolean DEFAULT_USE_SYSTEM_PROXY = true;
    private static final String DEFAULT_PROXY_TYPE = "HTTP";
    private static final String DEFAULT_PROXY_SERVER = "";
    private static final int DEFAULT_PROXY_PORT = 8080;
    private static final boolean DEFAULT_PROXY_AUTH = false;
    private static final String DEFAULT_PROXY_AUTH_USERNAME = "";
    private static final String DEFAULT_PROXY_AUTH_PASSWORD = "";
    private static final String DEFAULT_PROXY_BYPASS = "localhost,127.0.0.1";
    private static final boolean DEFAULT_DISABLE_SSL_VALIDATION = false;

    /**
     * Loads proxy settings from preferences.
     */
    public static boolean useSystemProxy() {
        return userPrefs.getBoolean(USE_SYSTEM_PROXY, DEFAULT_USE_SYSTEM_PROXY);
    }

    public static String getProxyType() {
        return userPrefs.get(PROXY_TYPE, DEFAULT_PROXY_TYPE);
    }

    public static String getProxyServer() {
        return userPrefs.get(PROXY_SERVER, DEFAULT_PROXY_SERVER);
    }

    public static int getProxyPort() {
        return userPrefs.getInt(PROXY_PORT, DEFAULT_PROXY_PORT);
    }

    public static boolean useProxyAuth() {
        return userPrefs.getBoolean(PROXY_AUTH, DEFAULT_PROXY_AUTH);
    }

    public static String getProxyAuthUsername() {
        return userPrefs.get(PROXY_AUTH_USERNAME, DEFAULT_PROXY_AUTH_USERNAME);
    }

    public static List<String> getProxyBypass() {
        String bypass = userPrefs.get(PROXY_BYPASS, DEFAULT_PROXY_BYPASS);
        return Arrays.asList(bypass.split(","));
    }

    /**
     * Checks if SSL certificate validation should be disabled. This is useful for development and testing with self-signed
     * certificates. WARNING: Disabling SSL certificate validation is a security risk in production.
     *
     * @return true if SSL certificate validation should be disabled, false otherwise
     */
    public static boolean disableSslValidation() {
        return userPrefs.getBoolean(DISABLE_SSL_VALIDATION, DEFAULT_DISABLE_SSL_VALIDATION);
    }

    /**
     * Gets the proxy authentication password as a char array. The caller is responsible for clearing the returned char array
     * after use.
     *
     * @return The proxy authentication password as a char array
     */
    public static char[] getProxyAuthPassword() {
        String encryptedPassword = userPrefs.get(PROXY_AUTH_PASSWORD, "");
        if (encryptedPassword.isEmpty()) {
            return new char[0];
        }

        String decryptedPassword = PasswordEncryption.decrypt(encryptedPassword);
        char[] passwordChars = decryptedPassword.toCharArray();

        // Clear the decrypted string from memory
        // This doesn't guarantee the string will be garbage collected immediately,
        // but it's better than nothing
        decryptedPassword = null;

        return passwordChars;
    }

    /**
     * Saves proxy settings to preferences. This method securely handles proxy credentials and ensures they are properly
     * encrypted.
     *
     * @param useSystemProxy Whether to use system proxy
     * @param proxyType The proxy type
     * @param proxyServer The proxy server
     * @param proxyPort The proxy port
     * @param proxyAuth Whether proxy authentication is required
     * @param proxyAuthUsername The proxy authentication username
     * @param proxyAuthPassword The proxy authentication password
     * @param proxyBypass The proxy bypass list
     * @param disableSslValidation Whether to disable SSL certificate validation
     */
    public static void saveSettings(boolean useSystemProxy, String proxyType, String proxyServer,
            int proxyPort, boolean proxyAuth, String proxyAuthUsername,
            String proxyAuthPassword, String proxyBypass, boolean disableSslValidation) {
        try {
            userPrefs.putBoolean(USE_SYSTEM_PROXY, useSystemProxy);
            userPrefs.put(PROXY_TYPE, proxyType != null ? proxyType : DEFAULT_PROXY_TYPE);
            userPrefs.put(PROXY_SERVER, proxyServer != null ? proxyServer : DEFAULT_PROXY_SERVER);
            userPrefs.putInt(PROXY_PORT, proxyPort > 0 && proxyPort <= 65535 ? proxyPort : DEFAULT_PROXY_PORT);
            userPrefs.putBoolean(PROXY_AUTH, proxyAuth);
            userPrefs.put(PROXY_AUTH_USERNAME, proxyAuthUsername != null ? proxyAuthUsername : DEFAULT_PROXY_AUTH_USERNAME);

            // Encrypt password before saving
            String encryptedPassword = "";
            if (proxyAuthPassword != null && !proxyAuthPassword.isEmpty()) {
                encryptedPassword = PasswordEncryption.encrypt(proxyAuthPassword);
            }
            userPrefs.put(PROXY_AUTH_PASSWORD, encryptedPassword);

            userPrefs.put(PROXY_BYPASS, proxyBypass != null ? proxyBypass : DEFAULT_PROXY_BYPASS);
            userPrefs.putBoolean(DISABLE_SSL_VALIDATION, disableSslValidation);

            // Log without sensitive information
            log.info("Proxy settings saved. Using system proxy: {}", useSystemProxy);
            if (!useSystemProxy && proxyServer != null && !proxyServer.isEmpty()) {
                log.info("Custom proxy configured: {}:{}", proxyServer, proxyPort);
                if (proxyAuth) {
                    log.info("Proxy authentication enabled");
                }
            }
        } catch (Exception e) {
            log.error("Failed to save proxy settings", e);
        }
    }

    /**
     * Creates a Proxy object based on current settings. Returns null if no proxy should be used.
     */
    public static Proxy createProxy() {
        if (useSystemProxy()) {
            // System proxy settings are handled by the JVM
            return null;
        }

        String server = getProxyServer();
        int port = getProxyPort();

        if (server == null || server.isEmpty()) {
            return null;
        }
        // Validate port range
        if (port <= 0 || port > 65535) {
            log.warn("Invalid proxy port: {}. Using default port: {}", port, DEFAULT_PROXY_PORT);
            port = DEFAULT_PROXY_PORT;
        }

        // Convert string proxy type to Proxy.Type enum
        Proxy.Type type;
        String proxyTypeStr = getProxyType();

        // Java's Proxy.Type enum only supports HTTP and SOCKS, not HTTPS
        // Both HTTP and HTTPS proxy settings use Proxy.Type.HTTP
        if ("HTTP".equalsIgnoreCase(proxyTypeStr) || "HTTPS".equalsIgnoreCase(proxyTypeStr)) {
            type = Proxy.Type.HTTP;
        } else if ("SOCKS".equalsIgnoreCase(proxyTypeStr)) {
            type = Proxy.Type.SOCKS;
        } else {
            // Default to HTTP for any unrecognized type
            log.warn("Unrecognized proxy type: {}. Using HTTP instead.", proxyTypeStr);
            type = Proxy.Type.HTTP;
        }

        return new Proxy(type, new InetSocketAddress(server, port));
    }

    /**
     * Sets up proxy authentication if needed. This method securely handles proxy credentials and ensures they are cleared from
     * memory after use.
     *
     * @deprecated This method sets a global JVM authenticator which affects all HTTP connections and can potentially break
     * other components. Use {@link #createProxyAuthenticator()} for HttpClient or {@link #getProxyAuthorizationHeader()} for
     * HttpURLConnection instead.
     */
    @Deprecated
    public static void setupProxyAuthentication() {
        // This method is deprecated and should not be used.
        // It's kept for backward compatibility but does nothing.
        log.warn("setupProxyAuthentication() is deprecated and does nothing. " +
                "Use createProxyAuthenticator() for HttpClient or getProxyAuthorizationHeader() for HttpURLConnection instead.");
    }

    /**
     * Creates an Authenticator for use with HttpClient that handles proxy authentication. This method securely handles proxy
     * credentials and ensures they are cleared from memory after use.
     *
     * @return An Authenticator that can be used with HttpClient.Builder.authenticator()
     */
    public static Authenticator createProxyAuthenticator() {
        if (!useSystemProxy() && useProxyAuth()) {
            final String username = getProxyAuthUsername();
            final char[] passwordChars = getProxyAuthPassword();

            if (username != null && !username.isEmpty() && passwordChars != null && passwordChars.length > 0) {
                Authenticator authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(getProxyServer())) {
                            // Create a copy of the password chars to avoid modifying the original
                            char[] passwordCopy = Arrays.copyOf(passwordChars, passwordChars.length);
                            return new PasswordAuthentication(username, passwordCopy);
                        }
                        return null;
                    }
                };

                // Log that authentication is set up but don't log the username
                log.info("Proxy authentication set up");

                // Clear the password from memory
                Arrays.fill(passwordChars, '\0');

                return authenticator;
            } else {
                // Clear the password from memory even if not used
                if (passwordChars != null) {
                    Arrays.fill(passwordChars, '\0');
                }
            }
        }

        return null;
    }

    /**
     * Gets the "Proxy-Authorization" header value for use with HttpURLConnection. This method securely handles proxy
     * credentials and ensures they are cleared from memory after use.
     *
     * @return The "Proxy-Authorization" header value, or null if proxy authentication is not needed
     */
    public static String getProxyAuthorizationHeader() {
        if (!useSystemProxy() && useProxyAuth()) {
            final String username = getProxyAuthUsername();
            final char[] passwordChars = getProxyAuthPassword();

            if (username != null && !username.isEmpty() && passwordChars != null && passwordChars.length > 0) {
                // Create the authorization header value
                String auth = username + ":" + new String(passwordChars);
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                String authHeader = "Basic " + encodedAuth;

                // Clear the password from memory
                Arrays.fill(passwordChars, '\0');

                return authHeader;
            } else {
                // Clear the password from memory even if not used
                if (passwordChars != null) {
                    Arrays.fill(passwordChars, '\0');
                }
            }
        }

        return null;
    }

    /**
     * Checks if a host should bypass the proxy. This method performs precise matching to avoid false positives: 1. Exact match:
     * host exactly matches a bypass entry 2. Domain suffix match: host ends with a bypass entry preceded by a dot 3. Wildcard
     * match: supports simple wildcard patterns like "*.example.com"
     */
    public static boolean shouldBypassProxy(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }

        List<String> bypassList = getProxyBypass();
        for (String bypassEntry : bypassList) {
            String bypass = bypassEntry.trim();
            if (bypass.isEmpty()) {
                continue;
            }

            // Case 1: Exact match
            if (host.equalsIgnoreCase(bypass)) {
                return true;
            }

            // Case 2: Domain suffix match (e.g., ".example.com" matches "sub.example.com")
            if (bypass.startsWith(".") && host.toLowerCase().endsWith(bypass.toLowerCase())) {
                return true;
            }

            // Case 3: Wildcard match (e.g., "*.example.com" matches "sub.example.com")
            if (bypass.startsWith("*.")) {
                String suffix = bypass.substring(1); // Remove the *
                if (host.toLowerCase().endsWith(suffix.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static class PasswordEncryption {
        private static final String ALGORITHM = "AES/GCM/NoPadding";
        private static final String KEYSTORE_TYPE = "PKCS12";
        private static final String KEY_ALIAS = "swaggerific-proxy-key";
        private static final String KEYSTORE_FILENAME = "swaggerific-keystore.p12";
        private static final int GCM_TAG_LENGTH = 128;
        private static final int GCM_IV_LENGTH = 12;

        /**
         * Encrypts a string using AES-GCM with a secure key from the keystore
         */
        static String encrypt(String value) throws NoSuchAlgorithmException {
            if (value == null || value.isEmpty()) {
                return "";
            }

            try {
                SecretKey key = getOrCreateSecretKey();
                byte[] iv = generateIv();

                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

                byte[] encryptedData = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

                // Combine IV and encrypted data
                ByteBuffer combined = ByteBuffer.allocate(iv.length + encryptedData.length);
                combined.put(iv);
                combined.put(encryptedData);

                return Base64.getEncoder().encodeToString(combined.array());
            } catch (Exception e) {
                log.error("Encryption failed", e);
                return "";
            }
        }

        /**
         * Decrypts a string using AES-GCM with a secure key from the keystore
         */
        static String decrypt(String encrypted) {
            if (encrypted == null || encrypted.isEmpty()) {
                return "";
            }

            try {
                byte[] combined = Base64.getDecoder().decode(encrypted);
                if (combined.length < GCM_IV_LENGTH) {
                    throw new IllegalArgumentException("Invalid encrypted data");
                }

                // Extract IV and encrypted data
                ByteBuffer buffer = ByteBuffer.wrap(combined);
                byte[] iv = new byte[GCM_IV_LENGTH];
                buffer.get(iv);
                byte[] encryptedData = new byte[buffer.remaining()];
                buffer.get(encryptedData);

                SecretKey key = getOrCreateSecretKey();
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

                return new String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Decryption failed", e);
                return "";
            }
        }

        /**
         * Gets or creates a secret key from the keystore
         */
        private static SecretKey getOrCreateSecretKey() throws Exception {
            KeyStore keyStore = loadOrCreateKeyStore();

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                // Generate new key with proper strength
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, SecureRandom.getInstanceStrong());
                SecretKey key = keyGen.generateKey();

                // Store in keystore
                KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(key);
                keyStore.setEntry(KEY_ALIAS, entry,
                        new KeyStore.PasswordProtection(getKeystorePassword()));

                saveKeyStore(keyStore);
                return key;
            }

            return (SecretKey) keyStore.getKey(KEY_ALIAS, getKeystorePassword());
        }

        /**
         * Generates a random IV for each encryption
         */
        private static byte[] generateIv() throws NoSuchAlgorithmException {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            return iv;
        }

        /**
         * Gets the keystore password from system properties or environment
         */
        private static char[] getKeystorePassword() throws NoSuchAlgorithmException {
            String password = System.getProperty("swaggerific.keystore.password");
            if (password == null) {
                password = System.getenv("SWAGGERIFIC_KEYSTORE_PASSWORD");
            }
            if (password == null) {
                // Generate a random password if none exists
                byte[] randomBytes = new byte[32];
                SecureRandom.getInstanceStrong().nextBytes(randomBytes);
                password = Base64.getEncoder().encodeToString(randomBytes);
                System.setProperty("swaggerific.keystore.password", password);
            }
            return password.toCharArray();
        }

        private static KeyStore loadOrCreateKeyStore() throws Exception {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            Path keystorePath = getKeystorePath();

            if (Files.exists(keystorePath)) {
                try (InputStream is = Files.newInputStream(keystorePath)) {
                    keyStore.load(is, getKeystorePassword());
                }
            } else {
                keyStore.load(null, getKeystorePassword());
            }
            return keyStore;
        }

        private static void saveKeyStore(KeyStore keyStore) throws Exception {
            Path keystorePath = getKeystorePath();
            Files.createDirectories(keystorePath.getParent());

            try (OutputStream os = Files.newOutputStream(keystorePath)) {
                keyStore.store(os, getKeystorePassword());
            }
        }

        private static Path getKeystorePath() {
            return Paths.get(System.getProperty("user.home"))
                    .resolve(".swaggerific")
                    .resolve(KEYSTORE_FILENAME);
        }
    }
}
