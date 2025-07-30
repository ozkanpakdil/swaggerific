package io.github.ozkanpakdil.swaggerific;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A simple HTTP server for serving JSON responses in tests.
 * This is a lightweight alternative to MockServer that starts faster
 * and has minimal overhead.
 */
public class SimpleHttpServer {
    private static final Logger log = LoggerFactory.getLogger(SimpleHttpServer.class);
    private final HttpServer server;
    private final Map<String, ResponseConfig> pathResponses = new HashMap<>();
    private final int port;

    /**
     * Creates a new SimpleHttpServer on a random available port.
     *
     * @throws IOException if the server cannot be created
     */
    public SimpleHttpServer() throws IOException {
        this(0); // Use port 0 to get a random available port
    }

    /**
     * Creates a new SimpleHttpServer on the specified port.
     *
     * @param port the port to listen on, or 0 for a random port
     * @throws IOException if the server cannot be created
     */
    public SimpleHttpServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(2)); // Small thread pool is sufficient for tests
        server.createContext("/", new DefaultHandler());
        this.port = server.getAddress().getPort();
        log.info("Created SimpleHttpServer on port {}", this.port);
    }

    /**
     * Starts the server.
     */
    public void start() {
        server.start();
        log.info("Started SimpleHttpServer on port {}", port);
    }

    /**
     * Stops the server.
     */
    public void stop() {
        server.stop(0);
        log.info("Stopped SimpleHttpServer on port {}", port);
    }

    /**
     * Gets the port the server is listening on.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Configures a response for a specific path.
     *
     * @param path the path to respond to (e.g., "/petstore-swagger.json")
     * @param responseBody the response body to return
     * @param contentType the content type of the response (e.g., "application/json")
     * @param statusCode the HTTP status code to return
     */
    public void addResponse(String path, String responseBody, String contentType, int statusCode) {
        ResponseConfig config = new ResponseConfig(responseBody, contentType, statusCode);
        pathResponses.put(path, config);
        log.info("Added response for path: {}", path);
    }

    /**
     * Configuration for a response.
     */
    private static class ResponseConfig {
        private final String body;
        private final String contentType;
        private final int statusCode;

        public ResponseConfig(String body, String contentType, int statusCode) {
            this.body = body;
            this.contentType = contentType;
            this.statusCode = statusCode;
        }
    }

    /**
     * Default handler for all requests.
     */
    private class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            log.info("Received request for path: {}", path);

            ResponseConfig config = pathResponses.get(path);
            if (config == null) {
                // No specific response configured for this path
                String response = "Not found: " + path;
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                log.warn("No response configured for path: {}", path);
                return;
            }

            // Send the configured response
            byte[] responseBytes = config.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", config.contentType);
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
            exchange.sendResponseHeaders(config.statusCode, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
            log.info("Sent response for path: {} (status: {}, size: {} bytes)", 
                    path, config.statusCode, responseBytes.length);
        }
    }
}