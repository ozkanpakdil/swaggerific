package io.github.ozkanpakdil.swaggerific.tools.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.tools.exceptions.XmlFormattingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
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
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Default constructor.
     */
    public HttpServiceImpl() {
        this(new ObjectMapper());
    }

    @Override
    public HttpResponse sendRequest(HttpRequest request) {
        try {
            // Convert headers from Map to String array
            String[] headerArray = new String[request.getHeaders().size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                headerArray[i++] = entry.getKey();
                headerArray[i++] = entry.getValue();
            }

            // Build the Java HTTP request
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(request.getUri())
                    .method(request.getMethod(),
                            request.getBody() != null ? BodyPublishers.ofString(request.getBody()) : BodyPublishers.noBody());

            // Add headers if present
            if (headerArray.length > 0) {
                requestBuilder.headers(headerArray);
            }

            // Send the request
            java.net.http.HttpRequest httpRequest = requestBuilder.build();
            log.info("{} headers:{} , uri:{}", httpRequest.method(), mapper.writeValueAsString(headerArray), request.getUri());
            java.net.http.HttpResponse<String> httpResponse = client.send(httpRequest, BodyHandlers.ofString());

            // Determine content type
            String contentType = "application/json";
            if (httpResponse.headers().firstValue("Content-Type").isPresent()) {
                contentType = httpResponse.headers().firstValue("Content-Type").get();
            }

            // Create response headers map
            Map<String, String> responseHeaders = httpResponse.headers().map().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.join(", ", e.getValue())
                    ));

            // Create and return the response
            return new HttpResponse.Builder()
                    .statusCode(httpResponse.statusCode())
                    .headers(responseHeaders)
                    .body(httpResponse.body())
                    .contentType(contentType)
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
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
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