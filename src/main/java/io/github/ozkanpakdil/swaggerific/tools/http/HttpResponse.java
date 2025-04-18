package io.github.ozkanpakdil.swaggerific.tools.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Model record for HTTP responses.
 * This record encapsulates all the information received in an HTTP response.
 */
public record HttpResponse(
        int statusCode,
        Map<String, String> headers,
        String body,
        String contentType,
        boolean isError,
        String errorMessage
) {
    public HttpResponse {
        // Ensure headers is never null
        headers = headers != null ? headers : new HashMap<>();
    }

    /**
     * Builder class for HttpResponse.
     */
    public static class Builder {
        private int statusCode;
        private Map<String, String> headers = new HashMap<>();
        private String body;
        private String contentType;
        private boolean isError;
        private String errorMessage;

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers != null ? headers : new HashMap<>();
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder error(String errorMessage) {
            this.isError = true;
            this.errorMessage = errorMessage;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(statusCode, headers, body, contentType, isError, errorMessage);
        }
    }
}