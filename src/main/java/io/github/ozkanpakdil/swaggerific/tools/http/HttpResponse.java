package io.github.ozkanpakdil.swaggerific.tools.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for HTTP responses.
 * This class encapsulates all the information received in an HTTP response.
 */
public class HttpResponse {
    private int statusCode;
    private Map<String, String> headers;
    private String body;
    private String contentType;
    private boolean isError;
    private String errorMessage;

    /**
     * Default constructor.
     */
    public HttpResponse() {
        this.headers = new HashMap<>();
    }

    /**
     * Constructor with all parameters.
     *
     * @param statusCode  the HTTP status code
     * @param headers     the HTTP headers
     * @param body        the response body
     * @param contentType the content type of the response
     */
    public HttpResponse(int statusCode, Map<String, String> headers, String body, String contentType) {
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : new HashMap<>();
        this.body = body;
        this.contentType = contentType;
        this.isError = false;
    }

    /**
     * Constructor for error responses.
     *
     * @param errorMessage the error message
     */
    public HttpResponse(String errorMessage) {
        this.headers = new HashMap<>();
        this.isError = true;
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param statusCode the HTTP status code to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
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
     * Gets the response body.
     *
     * @return the response body
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the response body.
     *
     * @param body the response body to set
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType the content type to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Checks if the response is an error.
     *
     * @return true if the response is an error, false otherwise
     */
    public boolean isError() {
        return isError;
    }

    /**
     * Sets whether the response is an error.
     *
     * @param error true if the response is an error, false otherwise
     */
    public void setError(boolean error) {
        isError = error;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage the error message to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

        /**
         * Sets the HTTP status code.
         *
         * @param statusCode the HTTP status code to set
         * @return the builder
         */
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
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
         * Sets the response body.
         *
         * @param body the response body to set
         * @return the builder
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the content type.
         *
         * @param contentType the content type to set
         * @return the builder
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the response as an error.
         *
         * @param errorMessage the error message
         * @return the builder
         */
        public Builder error(String errorMessage) {
            this.isError = true;
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Builds the HttpResponse.
         *
         * @return the HttpResponse
         */
        public HttpResponse build() {
            HttpResponse response = new HttpResponse();
            response.statusCode = this.statusCode;
            response.headers = this.headers;
            response.body = this.body;
            response.contentType = this.contentType;
            response.isError = this.isError;
            response.errorMessage = this.errorMessage;
            return response;
        }
    }
}