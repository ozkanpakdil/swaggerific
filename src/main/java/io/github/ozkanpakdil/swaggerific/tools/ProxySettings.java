package io.github.ozkanpakdil.swaggerific.tools;

import io.github.ozkanpakdil.swaggerific.SwaggerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    // Default values
    private static final boolean DEFAULT_USE_SYSTEM_PROXY = true;
    private static final String DEFAULT_PROXY_TYPE = "HTTP";
    private static final String DEFAULT_PROXY_SERVER = "";
    private static final int DEFAULT_PROXY_PORT = 8080;
    private static final boolean DEFAULT_PROXY_AUTH = false;
    private static final String DEFAULT_PROXY_AUTH_USERNAME = "";
    private static final String DEFAULT_PROXY_AUTH_PASSWORD = "";
    private static final String DEFAULT_PROXY_BYPASS = "localhost,127.0.0.1";

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

    public static String getProxyAuthPassword() {
        String encryptedPassword = userPrefs.get(PROXY_AUTH_PASSWORD, "");
        return encryptedPassword.isEmpty() ? "" : PasswordEncryption.decrypt(encryptedPassword);
    }

    public static void saveSettings(boolean useSystemProxy, String proxyType, String proxyServer,
            int proxyPort, boolean proxyAuth, String proxyAuthUsername,
            String proxyAuthPassword, String proxyBypass) {
        userPrefs.putBoolean(USE_SYSTEM_PROXY, useSystemProxy);
        userPrefs.put(PROXY_TYPE, proxyType);
        userPrefs.put(PROXY_SERVER, proxyServer);
        userPrefs.putInt(PROXY_PORT, proxyPort);
        userPrefs.putBoolean(PROXY_AUTH, proxyAuth);
        userPrefs.put(PROXY_AUTH_USERNAME, proxyAuthUsername);
        // Encrypt password before saving
        String encryptedPassword = proxyAuthPassword.isEmpty() ? "" :
                PasswordEncryption.encrypt(proxyAuthPassword);
        userPrefs.put(PROXY_AUTH_PASSWORD, encryptedPassword);
        userPrefs.put(PROXY_BYPASS, proxyBypass);

        log.info("Proxy settings saved. Using system proxy: {}", useSystemProxy);
        if (!useSystemProxy) {
            log.info("Custom proxy configured: {}:{}", proxyServer, proxyPort);
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

        Proxy.Type type = Proxy.Type.HTTP;

        return new Proxy(type, new InetSocketAddress(server, port));
    }

    /**
     * Sets up proxy authentication if needed.
     */
    public static void setupProxyAuthentication() {
        if (!useSystemProxy() && useProxyAuth()) {
            final String username = getProxyAuthUsername();
            final String password = getProxyAuthPassword();

            if (username != null && !username.isEmpty() && password != null) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(getProxyServer())) {
                            return new PasswordAuthentication(username, password.toCharArray());
                        }
                        return null;
                    }
                });

                log.info("Proxy authentication set up for user: {}", username);
            }
        }
    }

    /**
     * Checks if a host should bypass the proxy.
     */
    public static boolean shouldBypassProxy(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }

        List<String> bypassList = getProxyBypass();
        for (String bypass : bypassList) {
            if (host.contains(bypass.trim())) {
                return true;
            }
        }

        return false;
    }

    private static class PasswordEncryption {
        private static final String ENCRYPT_KEY = "SwaggerificProxy!"; // Simple key for example
        private static final String ALGORITHM = "AES";

        static String encrypt(String value) {
            try {
                SecretKeySpec key = createSecretKey(ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8));
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encryptedBytes = cipher.doFinal(value.getBytes());
                return Base64.getEncoder().encodeToString(encryptedBytes);
            } catch (Exception e) {
                log.error("Failed to encrypt proxy password", e);
                return "";
            }
        }

        static String decrypt(String encrypted) {
            try {
                SecretKeySpec key = createSecretKey(ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8));
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key);
                byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
                return new String(decryptedBytes);
            } catch (Exception e) {
                log.error("Failed to decrypt proxy password", e);
                return "";
            }
        }

        private static SecretKeySpec createSecretKey(byte[] key) throws NoSuchAlgorithmException {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] keyBytes = sha.digest(key);
            return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), ALGORITHM);
        }
    }
}