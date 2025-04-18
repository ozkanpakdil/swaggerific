package io.github.ozkanpakdil.swaggerific.tools.http;

import java.net.URI;
import java.util.Map;

/**
 * Interface for HTTP operations.
 * This interface defines the contract for sending HTTP requests and receiving responses.
 */
public interface HttpService {
    /**
     * Sends an HTTP request and returns the response.
     *
     * @param request the HTTP request to send
     * @return the HTTP response
     */
    HttpResponse sendRequest(HttpRequest request);

    /**
     * Convenience method to send a GET request.
     *
     * @param uri     the URI to send the request to
     * @param headers the HTTP headers
     * @return the HTTP response
     */
    HttpResponse get(URI uri, Map<String, String> headers);

    /**
     * Convenience method to send a POST request.
     *
     * @param uri     the URI to send the request to
     * @param headers the HTTP headers
     * @param body    the request body
     * @return the HTTP response
     */
    HttpResponse post(URI uri, Map<String, String> headers, String body);

    /**
     * Convenience method to send a PUT request.
     *
     * @param uri     the URI to send the request to
     * @param headers the HTTP headers
     * @param body    the request body
     * @return the HTTP response
     */
    HttpResponse put(URI uri, Map<String, String> headers, String body);

    /**
     * Convenience method to send a DELETE request.
     *
     * @param uri     the URI to send the request to
     * @param headers the HTTP headers
     * @return the HTTP response
     */
    HttpResponse delete(URI uri, Map<String, String> headers);

    /**
     * Convenience method to send a request with any HTTP method.
     *
     * @param uri     the URI to send the request to
     * @param method  the HTTP method
     * @param headers the HTTP headers
     * @param body    the request body
     * @return the HTTP response
     */
    HttpResponse request(URI uri, String method, Map<String, String> headers, String body);
}
