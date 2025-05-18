package io.github.ozkanpakdil.swaggerific.tools.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.tools.ProxySettings;
import io.github.ozkanpakdil.swaggerific.tools.exceptions.XmlFormattingException;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the HttpService interface.
 * This class provides methods for making HTTP requests without UI dependencies.
 */
public class HttpServiceImpl implements HttpService {
    private static final Logger log = LoggerFactory.getLogger(HttpServiceImpl.class);
    private final ObjectMapper mapper;
    private final HttpClient client;

    /**
     * Constructor with ObjectMapper.
     *
     * @param mapper the ObjectMapper to use for JSON processing
     */
    public HttpServiceImpl(ObjectMapper mapper) {
        this.mapper = mapper;
        this.client = createHttpClient();
    }

    /**
     * Default constructor.
     */
    public HttpServiceImpl() {
        this(Json.mapper());
    }

    /**
     * Creates an HttpClient with the appropriate proxy configuration.
     * 
     * @return a configured HttpClient
     */
    private HttpClient createHttpClient() {
        // Set up proxy authentication if needed
        ProxySettings.setupProxyAuthentication();

        // Create HttpClient builder
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL);

        // Add proxy if configured
        final Proxy proxy = ProxySettings.createProxy();
        if (proxy != null && !ProxySettings.useSystemProxy()) {
            // Create a custom proxy selector
            ProxySelector proxySelector = new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    // Check if this host should bypass the proxy
                    if (uri != null && ProxySettings.shouldBypassProxy(uri.getHost())) {
                        log.debug("Bypassing proxy for host: {}", uri.getHost());
                        return Collections.singletonList(Proxy.NO_PROXY);
                    }

                    log.debug("Using proxy for host: {}", uri != null ? uri.getHost() : "unknown");
                    return Collections.singletonList(proxy);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    log.error("Proxy connection failed for URI: {}", uri, ioe);
                }
            };

            // Set the proxy selector
            builder.proxy(proxySelector);
        }

        return builder.build();
    }

    @Override
    public HttpResponse sendRequest(HttpRequest request) {
        try {
            String[] headerArray = new String[request.headers().size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : request.headers().entrySet()) {
                headerArray[i++] = entry.getKey();
                headerArray[i++] = entry.getValue();
            }

            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(request.uri())
                    .method(request.method(),
                            request.body() != null ? BodyPublishers.ofString(request.body()) : BodyPublishers.noBody());

            if (headerArray.length > 0) {
                requestBuilder.headers(headerArray);
            }

            java.net.http.HttpRequest httpRequest = requestBuilder.build();
            log.info("{} headers:{} , uri:{}", httpRequest.method(), mapper.writeValueAsString(headerArray), request.uri());
            java.net.http.HttpResponse<String> httpResponse = client.send(httpRequest, BodyHandlers.ofString());

            String contentType = httpResponse.headers().firstValue("Content-Type").orElse("application/json");

            Map<String, String> responseHeaders = httpResponse.headers().map().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.join(", ", e.getValue())
                    ));

            return new HttpResponse.Builder()
                    .statusCode(httpResponse.statusCode())
                    .headers(responseHeaders)
                    .body(httpResponse.body())
                    .contentType(contentType)
                    .build();

        } catch (InterruptedException e) {
            log.error("Thread interrupted during request", e);
            Thread.currentThread().interrupt();
            return new HttpResponse.Builder()
                    .error("Request interrupted: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error in request: {}", e.getMessage(), e);
            return new HttpResponse.Builder()
                    .error(e.getMessage())
                    .build();
        }
    }

    @Override
    public HttpResponse get(URI uri, Map<String, String> headers) {
        return request(uri, "GET", headers, null);
    }

    @Override
    public HttpResponse post(URI uri, Map<String, String> headers, String body) {
        return request(uri, "POST", headers, body);
    }

    @Override
    public HttpResponse put(URI uri, Map<String, String> headers, String body) {
        return request(uri, "PUT", headers, body);
    }

    @Override
    public HttpResponse delete(URI uri, Map<String, String> headers) {
        return request(uri, "DELETE", headers, null);
    }

    @Override
    public HttpResponse request(URI uri, String method, Map<String, String> headers, String body) {
        HttpRequest request = new HttpRequest.Builder()
                .uri(uri)
                .method(method)
                .headers(headers)
                .body(body)
                .build();
        return sendRequest(request);
    }

    /**
     * Pretty prints XML string.
     *
     * @param xmlString       the XML string to format
     * @param indent          the indentation level
     * @param ignoreDeclaration whether to ignore XML declaration
     * @return the formatted XML string
     * @throws XmlFormattingException if an error occurs during formatting
     */
    public static String prettyPrintXml(String xmlString, int indent, boolean ignoreDeclaration) {
        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = dbf.newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new XmlFormattingException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }
}
