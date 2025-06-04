package io.github.ozkanpakdil.swaggerific.tools;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

import static io.github.ozkanpakdil.swaggerific.ui.MainController.APP_SETTINGS_HOME;

/**
 * Manages proxy settings for the application. This class handles loading, saving, and applying proxy settings.
 */
public class ProxySettings {
    private static final Logger log = LoggerFactory.getLogger(ProxySettings.class);
    private static final Preferences userPrefs = Preferences.userNodeForPackage(SwaggerApplication.class);
    private static final String PROXY_SETTINGS_FILE = APP_SETTINGS_HOME + "/proxy_settings.bin";

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
    private static final String DEFAULT_PROXY_BYPASS = "localhost,127.0.0.1";
    private static final boolean DEFAULT_DISABLE_SSL_VALIDATION = false;

    private static boolean isProduction() {
        // Check if running in IntelliJ IDEA
        if (System.getProperty("java.class.path").contains("idea_rt.jar")) {
            return false;
        }

        // Fall back to environment property check
        String env = System.getProperty("app.environment", "production");
        return "production".equalsIgnoreCase(env);
    }

    private static ProxySettingsStorage getStorage() {
        return isProduction() ? new PreferencesStorage() : new FileStorage();
    }

    private interface ProxySettingsStorage {
        void putBoolean(String key, boolean value);

        void putString(String key, String value);

        void putInt(String key, int value);

        boolean getBoolean(String key, boolean defaultValue);

        String getString(String key, String defaultValue);

        int getInt(String key, int defaultValue);

        void remove(String key);

        void save() throws Exception;
    }

    private static class PreferencesStorage implements ProxySettingsStorage {
        @Override
        public void putBoolean(String key, boolean value) {
            userPrefs.putBoolean(key, value);
        }

        @Override
        public void putString(String key, String value) {
            userPrefs.put(key, value);
        }

        @Override
        public void putInt(String key, int value) {
            userPrefs.putInt(key, value);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return userPrefs.getBoolean(key, defaultValue);
        }

        @Override
        public String getString(String key, String defaultValue) {
            return userPrefs.get(key, defaultValue);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return userPrefs.getInt(key, defaultValue);
        }

        @Override
        public void remove(String key) {
            userPrefs.remove(key);
        }

        @Override
        public void save() {
            // No need to explicitly save for Preferences
        }
    }

    private static class FileStorage implements ProxySettingsStorage {
        private final Properties properties = new Properties();
        private final File file;

        public FileStorage() {
            file = new File(PROXY_SETTINGS_FILE);
            load();
        }

        private void load() {
            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    properties.load(in);
                } catch (IOException e) {
                    log.error("Failed to load proxy settings from file", e);
                }
            }
        }

        @Override
        public void putBoolean(String key, boolean value) {
            properties.setProperty(key, String.valueOf(value));
        }

        @Override
        public void putString(String key, String value) {
            properties.setProperty(key, value);
        }

        @Override
        public void putInt(String key, int value) {
            properties.setProperty(key, String.valueOf(value));
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
        }

        @Override
        public String getString(String key, String defaultValue) {
            return properties.getProperty(key, defaultValue);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        @Override
        public void remove(String key) {
            properties.remove(key);
        }

        @Override
        public void save() throws Exception {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }
            try (OutputStream out = new FileOutputStream(file)) {
                properties.store(out, "Proxy Settings");
            }
        }
    }

    public static boolean useSystemProxy() {
        return getStorage().getBoolean(USE_SYSTEM_PROXY, DEFAULT_USE_SYSTEM_PROXY);
    }

    public static String getProxyType() {
        return getStorage().getString(PROXY_TYPE, DEFAULT_PROXY_TYPE);
    }

    public static String getProxyServer() {
        return getStorage().getString(PROXY_SERVER, DEFAULT_PROXY_SERVER);
    }

    public static int getProxyPort() {
        return getStorage().getInt(PROXY_PORT, DEFAULT_PROXY_PORT);
    }

    public static boolean useProxyAuth() {
        return getStorage().getBoolean(PROXY_AUTH, DEFAULT_PROXY_AUTH);
    }

    public static String getProxyAuthUsername() {
        return getStorage().getString(PROXY_AUTH_USERNAME, DEFAULT_PROXY_AUTH_USERNAME);
    }

    public static List<String> getProxyBypass() {
        String bypass = getStorage().getString(PROXY_BYPASS, DEFAULT_PROXY_BYPASS);
        return Arrays.asList(bypass.split(","));
    }

    public static boolean disableSslValidation() {
        return getStorage().getBoolean(DISABLE_SSL_VALIDATION, DEFAULT_DISABLE_SSL_VALIDATION);
    }

    public static char[] getProxyAuthPassword() {
        String encryptedPassword = getStorage().getString(PROXY_AUTH_PASSWORD, "");
        if (encryptedPassword.isEmpty()) {
            return new char[0];
        }

        String decryptedPassword = PasswordEncryption.decrypt(encryptedPassword);
        if (decryptedPassword.isEmpty()) {
            log.warn("Proxy password decryption failed, clearing stored encrypted password.");
            getStorage().remove(PROXY_AUTH_PASSWORD);
            return new char[0];
        }
        return decryptedPassword.toCharArray();
    }

    public static void validateProxySettings() {
        if (!useSystemProxy()) {
            String server = getProxyServer();
            int port = getProxyPort();

            if (server == null || server.trim().isEmpty()) {
                throw new IllegalStateException("Proxy server cannot be empty when proxy is enabled");
            }

            if (port <= 0 || port > 65535) {
                throw new IllegalStateException("Invalid proxy port: " + port + ". Port must be between 1 and 65535");
            }

            if (useProxyAuth()) {
                String username = getProxyAuthUsername();
                char[] password = getProxyAuthPassword();

                try {
                    if (username == null || username.trim().isEmpty()) {
                        throw new IllegalStateException("Proxy username cannot be empty when authentication is enabled");
                    }

                    if (password == null || password.length == 0) {
                        throw new IllegalStateException("Proxy password cannot be empty when authentication is enabled");
                    }
                } finally {
                    if (password != null) {
                        Arrays.fill(password, '\0');
                    }
                }
            }
        }
    }

    public static void saveSettings(boolean useSystemProxy, String proxyType, String proxyServer,
            int proxyPort, boolean proxyAuth, String proxyAuthUsername,
            String proxyAuthPassword, String proxyBypass, boolean disableSslValidation) {
        try {
            // Validate inputs before saving
            if (!useSystemProxy && (proxyServer == null || proxyServer.trim().isEmpty())) {
                throw new IllegalStateException("Proxy server cannot be empty");
            }

            if (!useSystemProxy && (proxyPort <= 0 || proxyPort > 65535)) {
                throw new IllegalStateException("Invalid proxy port: " + proxyPort);
            }

            if (!useSystemProxy && proxyAuth) {
                if (proxyAuthUsername == null || proxyAuthUsername.trim().isEmpty()) {
                    throw new IllegalStateException("Proxy username cannot be empty when authentication is enabled");
                }
                if (proxyAuthPassword == null || proxyAuthPassword.trim().isEmpty()) {
                    throw new IllegalStateException("Proxy password cannot be empty when authentication is enabled");
                }
            }

            ProxySettingsStorage storage = getStorage();
            storage.putBoolean(USE_SYSTEM_PROXY, useSystemProxy);
            storage.putString(PROXY_TYPE, proxyType != null ? proxyType : DEFAULT_PROXY_TYPE);
            storage.putString(PROXY_SERVER, proxyServer != null ? proxyServer : DEFAULT_PROXY_SERVER);
            storage.putInt(PROXY_PORT, proxyPort > 0 && proxyPort <= 65535 ? proxyPort : DEFAULT_PROXY_PORT);
            storage.putBoolean(PROXY_AUTH, proxyAuth);
            storage.putString(PROXY_AUTH_USERNAME, proxyAuthUsername != null ? proxyAuthUsername : DEFAULT_PROXY_AUTH_USERNAME);

            String encryptedPassword = "";
            if (proxyAuthPassword != null && !proxyAuthPassword.isEmpty()) {
                encryptedPassword = PasswordEncryption.encrypt(proxyAuthPassword);
            }
            storage.putString(PROXY_AUTH_PASSWORD, encryptedPassword);

            storage.putString(PROXY_BYPASS, proxyBypass != null ? proxyBypass : DEFAULT_PROXY_BYPASS);
            storage.putBoolean(DISABLE_SSL_VALIDATION, disableSslValidation);

            storage.save();

            log.info("Proxy settings saved. Using system proxy: {}", useSystemProxy);
            if (!useSystemProxy && proxyServer != null && !proxyServer.isEmpty()) {
                log.info("Custom proxy configured: {}:{}", proxyServer, proxyPort);
                if (proxyAuth) {
                    log.info("Proxy authentication enabled for user: {}", proxyAuthUsername);
                }
            }
        } catch (Exception e) {
            String errorMsg = "Failed to save proxy settings: " + e.getMessage();
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    public static Proxy createProxy() {
        if (useSystemProxy()) {
            return null;
        }

        String server = getProxyServer();
        int port = getProxyPort();

        if (server == null || server.isEmpty()) {
            return null;
        }
        if (port <= 0 || port > 65535) {
            log.warn("Invalid proxy port: {}. Using default port: {}", port, DEFAULT_PROXY_PORT);
            port = DEFAULT_PROXY_PORT;
        }

        Proxy.Type type;
        String proxyTypeStr = getProxyType();

        if ("HTTP".equalsIgnoreCase(proxyTypeStr) || "HTTPS".equalsIgnoreCase(proxyTypeStr)) {
            type = Proxy.Type.HTTP;
        } else if ("SOCKS".equalsIgnoreCase(proxyTypeStr)) {
            type = Proxy.Type.SOCKS;
        } else {
            log.warn("Unrecognized proxy type: {}. Using HTTP instead.", proxyTypeStr);
            type = Proxy.Type.HTTP;
        }

        return new Proxy(type, new InetSocketAddress(server, port));
    }

    @Deprecated
    public static void setupProxyAuthentication() {
        log.warn("setupProxyAuthentication() is deprecated and does nothing. " +
                "Use createProxyAuthenticator() for HttpClient or getProxyAuthorizationHeader() for HttpURLConnection instead.");
    }

    public static Authenticator createProxyAuthenticator() {
        if (!useSystemProxy() && useProxyAuth()) {
            final String username = getProxyAuthUsername();
            final char[] passwordChars = getProxyAuthPassword();

            if (username != null && !username.isEmpty() && passwordChars != null && passwordChars.length > 0) {
                Authenticator authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(getProxyServer())) {
                            char[] passwordCopy = Arrays.copyOf(passwordChars, passwordChars.length);
                            return new PasswordAuthentication(username, passwordCopy);
                        }
                        return null;
                    }
                };

                log.info("Proxy authentication set up");
                Arrays.fill(passwordChars, '\0');
                return authenticator;
            } else {
                if (passwordChars != null) {
                    Arrays.fill(passwordChars, '\0');
                }
            }
        }

        return null;
    }

    public static String getProxyAuthorizationHeader() {
        if (!useSystemProxy() && useProxyAuth()) {
            final String username = getProxyAuthUsername();
            final char[] passwordChars = getProxyAuthPassword();

            if (username != null && !username.isEmpty() && passwordChars != null && passwordChars.length > 0) {
                String auth = username + ":" + new String(passwordChars);
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                String authHeader = "Basic " + encodedAuth;

                Arrays.fill(passwordChars, '\0');
                return authHeader;
            } else {
                if (passwordChars != null) {
                    Arrays.fill(passwordChars, '\0');
                }
            }
        }

        return null;
    }

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

            if (host.equalsIgnoreCase(bypass)) {
                return true;
            }

            if (bypass.startsWith(".") && host.toLowerCase().endsWith(bypass.toLowerCase())) {
                return true;
            }

            if (bypass.startsWith("*.")) {
                String suffix = bypass.substring(1);
                if (host.toLowerCase().endsWith(suffix.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void setupSystemWideProxy() {
        try {
            validateProxySettings();

            if (!useSystemProxy()) {
                String proxyHost = getProxyServer();
                int proxyPort = getProxyPort();

                if (proxyHost != null && !proxyHost.isEmpty()) {
                    System.setProperty("http.proxyHost", proxyHost);
                    System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                    System.setProperty("https.proxyHost", proxyHost);
                    System.setProperty("https.proxyPort", String.valueOf(proxyPort));

                    if (useProxyAuth()) {
                        String username = getProxyAuthUsername();
                        char[] password = getProxyAuthPassword();

                        if (username != null && !username.isEmpty() && password.length > 0) {
                            System.setProperty("http.proxyUser", username);
                            System.setProperty("http.proxyPassword", new String(password));
                            System.setProperty("https.proxyUser", username);
                            System.setProperty("https.proxyPassword", new String(password));
                            Arrays.fill(password, '\0');
                        }
                    }

                    String nonProxyHosts = String.join("|", getProxyBypass());
                    System.setProperty("http.nonProxyHosts", nonProxyHosts);

                    // Set up authenticator
                    Authenticator.setDefault(createProxyAuthenticator());

                    // Test connection without throwing exceptions
                    boolean isProxyWorking = testProxyConnection();
                    if (!isProxyWorking) {
                        log.warn("Proxy connection test failed but continuing with current settings");
                    }

                    log.info("Proxy configured: {}:{}", proxyHost, proxyPort);
                }
            } else {
                // Clear proxy settings
                clearProxySettings();
                log.info("Using system proxy settings");
            }

            log.info("Proxy configuration completed");
        } catch (Exception e) {
            // Log error but don't throw exception to prevent app from failing
            log.error("Failed to setup proxy: {}. Continuing without proxy.", e.getMessage());
            clearProxySettings();
        }
    }

    private static void clearProxySettings() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");
        System.clearProperty("http.nonProxyHosts");
        Authenticator.setDefault(null);
    }

    private static boolean testProxyConnection() {
        try {
            // Create proxy configuration
            Proxy proxy = createProxy();
            if (proxy == null) {
                return true; // If no proxy is configured, return success
            }

            // Get auth credentials
            String authHeader = getProxyAuthorizationHeader();

            // Build HttpClient with proxy settings
            HttpClient client = HttpClient.newBuilder()
                    .proxy(ProxySelector.of((InetSocketAddress) proxy.address()))
                    .authenticator(createProxyAuthenticator())
                    .connectTimeout(Duration.ofSeconds(5))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            // Create test request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("http://httpbin.org/status/200"))
                    .timeout(Duration.ofSeconds(5))
                    .GET();

            // Add proxy authentication if available
            if (authHeader != null) {
                requestBuilder.header("Proxy-Authorization", authHeader);
            }

            // Send request and verify response
            HttpResponse<Void> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 407) {
                log.error("Proxy authentication failed - check username and password");
                return false;
            }

            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Proxy connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private static class PasswordEncryption {
        private static final String ALGORITHM = "AES/GCM/NoPadding";
        private static final String KEYSTORE_TYPE = "PKCS12";
        private static final String KEY_ALIAS = "swaggerific-proxy-key";
        private static final String KEYSTORE_FILENAME = "swaggerific-keystore.p12";
        private static final String KEYSTORE_PASSWORD_FILE = ".keystore-password";
        private static final int GCM_TAG_LENGTH = 128;
        private static final int GCM_IV_LENGTH = 12;
        private static final int KEYSTORE_PASSWORD_LENGTH = 32;
        private static volatile String keystorePassword = null;

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

                ByteBuffer combined = ByteBuffer.allocate(iv.length + encryptedData.length);
                combined.put(iv);
                combined.put(encryptedData);

                return Base64.getEncoder().encodeToString(combined.array());
            } catch (Exception e) {
                log.error("Encryption failed", e);
                return "";
            }
        }

        static String decrypt(String encrypted) {
            if (encrypted == null || encrypted.isEmpty()) {
                return "";
            }

            try {
                byte[] combined = Base64.getDecoder().decode(encrypted);
                if (combined.length < GCM_IV_LENGTH) {
                    throw new IllegalArgumentException("Invalid encrypted data");
                }

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

        private static SecretKey getOrCreateSecretKey() throws Exception {
            KeyStore keyStore = loadOrCreateKeyStore();

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, SecureRandom.getInstanceStrong());
                SecretKey key = keyGen.generateKey();

                KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(key);
                keyStore.setEntry(KEY_ALIAS, entry,
                        new KeyStore.PasswordProtection(getKeystorePassword()));

                saveKeyStore(keyStore);
                return key;
            }

            return (SecretKey) keyStore.getKey(KEY_ALIAS, getKeystorePassword());
        }

        private static byte[] generateIv() throws NoSuchAlgorithmException {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            return iv;
        }

        private static char[] getKeystorePassword() {
            if (keystorePassword != null) {
                return keystorePassword.toCharArray();
            }

            synchronized (PasswordEncryption.class) {
                if (keystorePassword != null) {
                    return keystorePassword.toCharArray();
                }

                Path passwordFile = getKeystorePath().getParent().resolve(KEYSTORE_PASSWORD_FILE);

                // Try to load existing password
                if (Files.exists(passwordFile)) {
                    try {
                        keystorePassword = Files.readString(passwordFile, StandardCharsets.UTF_8).trim();
                        return keystorePassword.toCharArray();
                    } catch (IOException e) {
                        log.warn("Failed to read keystore password file, generating new password", e);
                    }
                }

                // Generate new random password
                try {
                    byte[] randomBytes = new byte[KEYSTORE_PASSWORD_LENGTH];
                    SecureRandom.getInstanceStrong().nextBytes(randomBytes);
                    keystorePassword = Base64.getEncoder().encodeToString(randomBytes);

                    // Ensure parent directory exists
                    Files.createDirectories(passwordFile.getParent());

                    // Save password with restricted permissions
                    Files.writeString(passwordFile, keystorePassword, StandardCharsets.UTF_8);
                    try {
                        Files.setPosixFilePermissions(passwordFile,
                                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
                    } catch (UnsupportedOperationException e) {
                        // Windows systems don't support POSIX permissions
                        log.debug("POSIX file permissions not supported on this system");
                    }

                    return keystorePassword.toCharArray();
                } catch (Exception e) {
                    log.error("Failed to generate and save keystore password", e);
                    throw new RuntimeException("Could not generate keystore password", e);
                }
            }
        }

        private static KeyStore loadOrCreateKeyStore() throws Exception {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            Path keystorePath = getKeystorePath();

            if (Files.exists(keystorePath)) {
                try (InputStream is = Files.newInputStream(keystorePath)) {
                    keyStore.load(is, getKeystorePassword());
                } catch (java.io.IOException e) {
                    log.warn("Keystore integrity check failed, deleting and recreating keystore: {}", e.getMessage());
                    Files.delete(keystorePath);
                    keyStore.load(null, getKeystorePassword());
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
