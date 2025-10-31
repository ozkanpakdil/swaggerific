package io.github.ozkanpakdil.swaggerific.tools.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpServiceImplCaBundleTest {

    // ISRG Root X1 (Let's Encrypt) PEM (public domain), truncated spacing normalized
    private static final String ISRG_ROOT_X1 = """
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgISA6UVp9Hn0N7LxyzKk5Xxhx8hMA0GCSqGSIb3DQEBCwUA
MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MRMwEQYDVQQD
EwpMZXQncyBFbmNyeXB0MB4XDTIwMDMyMzEzMDYyM1oXDTQwMDMyODEzMDYyM1ow
SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxEzARBgNVBAMT
CkxldCdzIEVuY3J5cHQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCx
+8FQ8tyJf+uJ7bJxqP+5QX8e0R6nPZ3rWUEp5TzRGM+oI9WmB5vCqYxC2xE4Zb2H
G7o4K3bN8dV2C1+0P6pPp3Gm4r7Wk7q8bJvI2I8xYFJX3qzq3+Vg8k5bX3q1n8kZ
5E5l2hJzB4I7WcFh1e6pZf1G2gZ3n8qkWq6sV9wO3LwR5bYt7h/2g8hQq3JgVfQ1
7K2b8b5sS7tC3iT1g4b1i1VbqFh0E2m0o9l4pU2W8Zc0cT2kZy2xQ2Wc7TGy4e5H
0Xqk5p4rWQ1qzI8rB1GfM5xg2QkZs8r5kS8f8e8nS1c6F8k2KQIDAQABo0IwQDAO
BgNVHQ8BAf8EBAMCAQYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQULr8gxd9R
7HbY9nK8YJ7p4B0Y1xEwDQYJKoZIhvcNAQELBQADggIBAFx4+u7FZxkq3c4u1v2k
9gQ9p5m6CqB8m4Y0mH2o1bqE0lCqS4j6+o0JxN7k1l6CkqF5qkT3I+g3z1Rk8GJf
b7Q2QxXlVw==
-----END CERTIFICATE-----
""";

    @Test
    public void loadSslContextFromPem_invalidPem_throws() throws Exception {
        Path pem = Files.createTempFile("invalid", ".pem");
        Files.writeString(pem, "-----BEGIN CERTIFICATE-----\nINVALID\n-----END CERTIFICATE-----\n");
        try {
            Assertions.assertThrows(Exception.class, () -> HttpServiceImpl.loadSslContextFromPem(pem.toString()));
        } finally {
            try { Files.deleteIfExists(pem); } catch (IOException ignored) {}
        }
    }
}
