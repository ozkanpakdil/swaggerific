package io.github.ozkanpakdil.swaggerific.tools.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class for HTTP requests.
 * This class encapsulates all the information needed to make an HTTP request.
 */
public final class HttpRequest {
    private URI uri;
    private String method;
    private Map<String, String> headers;
    private String body;

    /**
     * Default constructor.
     */
    public HttpRequest() {
        this.headers = new HashMap<>();
    }

    /**
     * Constructor with all parameters.
     *
     * @param uri     the URI to send the request to
     * @param method  the HTTP method (GET, POST, etc.)
     * @param headers the HTTP headers
     * @param body    the request body
     */
    public HttpRequest(URI uri, String method, Map<String, String> headers, String body) {
        this.uri = uri;
        this.method = method;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body;
    }

    /**
     * Gets the URI.
     *
     * @return the URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Sets the URI.
     *
     * @param uri the URI to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Gets the HTTP method.
     *
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the HTTP method.
     *
     * @param method the HTTP method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the HTTP headers.
     *
     * @return the HTTP headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Sets the HTTP headers.
     *
     * @param headers the HTTP headers to set
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }

    /**
     * Gets the request body.
     *
     * @return the request body
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the request body.
     *
     * @param body the request body to set
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Builder class for HttpRequest.
     */
    public static class Builder {
        private URI uri;
        private String method;
        private Map<String, String> headers = new HashMap<>();
        private String body;

        /**
         * Sets the URI.
         *
         * @param uri the URI to set
         * @return the builder
         */
        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Sets the HTTP method.
         *
         * @param method the HTTP method to set
         * @return the builder
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the HTTP headers.
         *
         * @param headers the HTTP headers to set
         * @return the builder
         */
        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Adds an HTTP header.
         *
         * @param name  the header name
         * @param value the header value
         * @return the builder
         */
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * Sets the request body.
         *
         * @param body the request body to set
         * @return the builder
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Builds the HttpRequest.
         *
         * @return the HttpRequest
         */
        public HttpRequest build() {
            if (uri == null || method == null) {
                throw new IllegalStateException("URI and method must be set");
            }
            return new HttpRequest(uri, method, new HashMap<>(headers), body);
        }
    }
}