package io.github.ozkanpakdil.swaggerific.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpRequest;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpService;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for HTTP operations. This class handles HTTP requests and responses
 * without any UI dependencies.
 */
public class HttpUtility {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HttpUtility.class);
    private final HttpService httpService;

    /**
     * Constructor with ObjectMapper.
     *
     * @param mapper the ObjectMapper to use for JSON processing
     */
    public HttpUtility(ObjectMapper mapper) {
        this.httpService = new HttpServiceImpl(mapper);
    }

    /**
     * Default constructor.
     */
    public HttpUtility() {
        this(Json.mapper());
    }

    /**
     * Determines if the response is JSON based on Content-Type or content inspection.
     */
    public boolean isJsonResponse(HttpResponse response) {
        String contentType = response.contentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            return contentType.contains("json") ||
                    contentType.contains("application/javascript");
        }
        // Fallback to content inspection
        String body = response.body();
        return body != null && !body.isEmpty() &&
                (body.trim().startsWith("{") || body.trim().startsWith("["));
    }

    /**
     * Determines if the response is XML based on Content-Type or content inspection.
     */
    public boolean isXmlResponse(HttpResponse response) {
        String contentType = response.contentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            return contentType.contains("xml") ||
                    contentType.contains("text/html");
        }
        // Fallback to content inspection
        String body = response.body();
        return body != null && !body.isEmpty() &&
                body.trim().startsWith("<");
    }

    /**
     * Formats JSON string if possible.
     *
     * @param jsonString the JSON string to format
     * @return the formatted JSON string, or the original string if formatting fails
     */
    public String formatJson(String jsonString) {
        try {
            return Json.pretty(Json.mapper().readTree(jsonString));
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", e.getMessage());
            return jsonString;
        }
    }

    /**
     * Formats XML string if possible.
     *
     * @param xmlString the XML string to format
     * @return the formatted XML string, or the original string if formatting fails
     */
    public String formatXml(String xmlString) {
        try {
            return HttpServiceImpl.prettyPrintXml(xmlString, 4, true);
        } catch (Exception e) {
            log.warn("Failed to format XML: {}", e.getMessage());
            return xmlString;
        }
    }

    /**
     * Builds a URI from a base URI and request parameters.
     *
     * @param uri         the base URI
     * @param queryParams map of query parameter names to values
     * @param pathParams  map of path parameter names to values
     * @return the complete URI
     */
    public URI buildUri(String uri, Map<String, String> queryParams, Map<String, String> pathParams) {
        StringBuilder queryParamsBuilder = new StringBuilder();

        // Process query parameters
        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                queryParamsBuilderAppend(queryParamsBuilder, key, value);
            }
        });

        String queryParamsString = queryParamsBuilder.toString();
        AtomicReference<String> finalAddress = new AtomicReference<>(uri);

        // Process path parameters
        pathParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty() && finalAddress.get().contains("{" + key + "}")) {
                String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
                finalAddress.set(finalAddress.get().replace("{" + key + "}", encoded));
            }
        });

        if (!queryParamsString.isEmpty()) {
            finalAddress.set(finalAddress.get() + "?" + queryParamsString);
        }

        // Special handling for localhost without port
        try {
            URI parsedUri = URI.create(finalAddress.get());
            String host = parsedUri.getHost();
            int port = parsedUri.getPort();
            
            // If this is localhost or 127.0.0.1 without a port, check if we need to add one
            if ((host != null && (host.equals("localhost") || host.equals("127.0.0.1"))) && port == -1) {
                log.info("Detected localhost without port in URI: {}", finalAddress.get());
                
                // Try to extract port from the original URI if it exists
                URI originalUri = URI.create(uri);
                int originalPort = originalUri.getPort();
                
                if (originalPort != -1) {
                    // Reconstruct the URI with the port
                    String scheme = parsedUri.getScheme();
                    String path = parsedUri.getPath();
                    String query = parsedUri.getQuery();
                    
                    StringBuilder uriWithPort = new StringBuilder();
                    uriWithPort.append(scheme).append("://").append(host).append(":").append(originalPort);
                    
                    if (path != null) {
                        uriWithPort.append(path);
                    }
                    
                    if (query != null) {
                        uriWithPort.append("?").append(query);
                    }
                    
                    log.info("Fixed URI with port: {}", uriWithPort);
                    return URI.create(uriWithPort.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing URI: {}", e.getMessage());
        }

        return URI.create(finalAddress.get());
    }

    private void queryParamsBuilderAppend(StringBuilder queryParamsBuilder, String key, String value) {
        if (!queryParamsBuilder.isEmpty()) {
            queryParamsBuilder.append("&");
        }
        queryParamsBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /**
     * Sends an HTTP request and returns the response.
     *
     * @param targetUri   the target URI
     * @param httpMethod  the HTTP method
     * @param headers     the HTTP headers
     * @param body        the request body
     * @param queryParams the query parameters
     * @param pathParams  the path parameters
     * @return the HTTP response
     */
    public HttpResponse sendRequest(String targetUri, PathItem.HttpMethod httpMethod,
                                    Map<String, String> headers, String body,
                                    Map<String, String> queryParams, Map<String, String> pathParams) {
        try {
            URI uri = buildUri(targetUri, queryParams, pathParams);

            log.info("Headers before creating request: {}", headers);

            HttpRequest request = new HttpRequest.Builder()
                    .uri(uri)
                    .method(httpMethod.name())
                    .headers(headers)
                    .body(body)
                    .build();

            log.info("Sending {} request to {} with headers: {}", httpMethod.name(), uri, request.headers());
            return httpService.sendRequest(request);
        } catch (Exception e) {
            log.error("Error preparing request: {}", e.getMessage(), e);
            return new HttpResponse.Builder()
                    .error("Error preparing request: " + e.getMessage())
                    .build();
        }
    }

    public HttpResponse sendRequest(String string, PathItem.HttpMethod httpMethod) {
        return sendRequest(string, httpMethod, null, null, Collections.emptyMap(), Collections.emptyMap());
    }
}
