package io.github.ozkanpakdil.swaggerific.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpRequest;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpResponse;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpService;
import io.github.ozkanpakdil.swaggerific.tools.http.HttpServiceImpl;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import io.github.ozkanpakdil.swaggerific.ui.RequestHeader;
import io.github.ozkanpakdil.swaggerific.ui.component.STextField;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * UI adapter for HTTP operations. This class bridges the UI components with the HTTP service.
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
        this(new ObjectMapper());
    }

    /**
     * Processes the HTTP response and updates the UI.
     *
     * @param parent the MainController to update
     * @param response the HTTP response
     */
    private void processResponse(MainController parent, HttpResponse response) {
        try {
            if (response.isError()) {
                log.warn("Error in HTTP response: {}", response.getErrorMessage());
                parent.openDebugConsole();
                parent.getCodeJsonResponse().replaceText("Error in request: " + response.getErrorMessage() +
                        "\n\nPlease check your request parameters and try again. If the problem persists, " +
                        "check the server status or network connection.");
                return;
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response body received");
                parent.getCodeJsonResponse().replaceText(
                        "The server returned an empty response with status code: " + response.getStatusCode() +
                                "\n\nThis might be expected for some operations, or it could indicate an issue with the request."
                );
            } else if (isJsonResponse(response)) {
                try {
                    parent.getCodeJsonResponse().replaceText(
                            Json.pretty(Json.mapper().readTree(responseBody))
                    );
                    log.info("Successfully processed JSON response with status code: {}", response.getStatusCode());
                } catch (Exception e) {
                    log.warn("Failed to parse JSON response: {}", e.getMessage());
                    // If JSON parsing fails, show the raw response
                    parent.getCodeJsonResponse().replaceText(
                            "Warning: Could not format as JSON. Showing raw response:\n\n" + responseBody
                    );
                }
            } else if (isXmlResponse(response)) {
                log.info("Processing XML response with status code: {}", response.getStatusCode());
                parent.codeResponseXmlSettings(parent.getCodeJsonResponse(), "/css/xml-highlighting.css");
                try {
                    parent.getCodeJsonResponse().replaceText(
                            HttpServiceImpl.prettyPrintXml(responseBody, 4, true)
                    );
                } catch (Exception e) {
                    log.warn("Failed to format XML response: {}", e.getMessage());
                    // If XML formatting fails, show the raw response
                    parent.getCodeJsonResponse().replaceText(
                            "Warning: Could not format as XML. Showing raw response:\n\n" + responseBody
                    );
                }
            } else {
                // Fallback to raw response
                log.info("Processing raw response with status code: {}", response.getStatusCode());
                parent.getCodeJsonResponse().replaceText(responseBody);
            }

            // Always set the raw response
            parent.getCodeRawJsonResponse().setText(responseBody);

            // Show status code in UI
            log.info("Request completed with status code: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("Error processing response: {}", e.getMessage(), e);
            parent.openDebugConsole();
            parent.getCodeJsonResponse().replaceText(
                    "Error processing response: " + e.getMessage() +
                            "\n\nThis is an application error. Please report this issue with the steps to reproduce it."
            );
        }
    }

    /**
     * Determines if the response is JSON based on Content-Type or content inspection.
     */
    private boolean isJsonResponse(HttpResponse response) {
        String contentType = response.getContentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            return contentType.contains("json") ||
                    contentType.contains("application/javascript");
        }
        // Fallback to content inspection
        String body = response.getBody();
        return body != null && !body.isEmpty() &&
                (body.trim().startsWith("{") || body.trim().startsWith("["));
    }

    /**
     * Determines if the response is XML based on Content-Type or content inspection.
     */
    private boolean isXmlResponse(HttpResponse response) {
        String contentType = response.getContentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            return contentType.contains("xml") ||
                    contentType.contains("text/html");
        }
        // Fallback to content inspection
        String body = response.getBody();
        return body != null && !body.isEmpty() &&
                body.trim().startsWith("<");
    }

    /**
     * Converts UI table headers to a map.
     *
     * @param tableHeaders the TableView containing headers
     * @return a map of header names to values
     */
    private Map<String, String> getHeadersMap(TableView<RequestHeader> tableHeaders) {
        Map<String, String> headers = new HashMap<>();
        tableHeaders.getItems().forEach(m -> {
            if (Boolean.TRUE.equals(m.getChecked())) {
                headers.put(m.getName(), m.getValue());
            }
        });
        return headers;
    }

    /**
     * Builds a URI from a base URI and request parameters.
     *
     * @param uri the base URI
     * @param boxRequestParams the GridPane containing request parameters
     * @return the complete URI
     */
    URI getUri(String uri, GridPane boxRequestParams) {
        // Process query parameters from both STextField and ComboBox components
        StringBuilder queryParamsBuilder = new StringBuilder();

        // Process STextField query parameters
        boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof STextField ns && ns.getIn().equals("query"))
                .forEach(n -> {
                    STextField node = (STextField) n;
                    if (!queryParamsBuilder.isEmpty()) {
                        queryParamsBuilder.append("&");
                    }
                    queryParamsBuilder.append(node.getParamName()).append("=").append(node.getText());
                });

        // Process ComboBox query parameters
        boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof ComboBox && n.getUserData() instanceof STextField)
                .forEach(n -> {
                    ComboBox<?> comboBox = (ComboBox<?>) n;
                    STextField paramInfo = (STextField) comboBox.getUserData();

                    if ("query".equals(paramInfo.getIn()) && comboBox.getValue() != null) {
                        if (!queryParamsBuilder.isEmpty()) {
                            queryParamsBuilder.append("&");
                        }
                        queryParamsBuilder.append(paramInfo.getParamName()).append("=").append(comboBox.getValue());
                    }
                });

        String queryParams = queryParamsBuilder.toString();
        AtomicReference<String> finalAddress = new AtomicReference<>(uri);

        // Process path parameters from STextField components
        boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof STextField && finalAddress.get().contains("{"))
                .forEach(n -> {
                    STextField node = (STextField) n;
                    String encoded = URLEncoder.encode(node.getText(), StandardCharsets.UTF_8);
                    finalAddress.set(
                            finalAddress.get().replace("{" + Pattern.quote(node.getParamName()) + "}", encoded)
                    );
                });

        // Process path parameters from ComboBox components
        boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof ComboBox && n.getUserData() instanceof STextField && finalAddress.get().contains("{"))
                .forEach(n -> {
                    ComboBox<?> comboBox = (ComboBox<?>) n;
                    STextField paramInfo = (STextField) comboBox.getUserData();

                    if (comboBox.getValue() != null) {
                        finalAddress.set(finalAddress.get()
                                .replaceAll("\\{" + paramInfo.getParamName() + "}", comboBox.getValue().toString()));
                    }
                });

        if (!queryParams.isEmpty()) {
            finalAddress.set(finalAddress.get() + "?" + queryParams);
        }

        return URI.create(finalAddress.get());
    }

    /**
     * Sends an HTTP request and updates the UI with the response.
     *
     * @param parent the MainController to update
     * @param targetUri the target URI
     * @param httpMethod the HTTP method
     */
    public void request(MainController parent, String targetUri, PathItem.HttpMethod httpMethod) {
        try {
            URI uri = getUri(targetUri, parent.getBoxRequestParams());
            Map<String, String> headers = getHeadersMap(parent.getTableHeaders());
            String body = parent.getCodeJsonRequest().getText();

            HttpRequest request = new HttpRequest.Builder()
                    .uri(uri)
                    .method(httpMethod.name())
                    .headers(headers)
                    .body(body)
                    .build();

            HttpResponse response = httpService.sendRequest(request);
            processResponse(parent, response);
        } catch (Exception e) {
            log.error("Error preparing request: {}", e.getMessage(), e);
            parent.openDebugConsole();
            parent.getCodeJsonResponse().replaceText("Error preparing request: " + e.getMessage());
        }
    }
}
