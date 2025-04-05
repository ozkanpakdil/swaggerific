package io.github.ozkanpakdil.swaggerific.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.tools.exceptions.XmlFormattingException;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import io.github.ozkanpakdil.swaggerific.ui.RequestHeader;
import io.github.ozkanpakdil.swaggerific.ui.component.STextField;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.PathItem;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Making the http calls.
 * TODO: needs SOLIDification....
 *  * parameters should get rid of the UI elements.
 *  * HEAD, OPTIONS, PATCH,TRACE request can be one function.
 */
public class HttpUtility {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HttpUtility.class);

    private static void sendRequestAndShowResponse(ObjectMapper mapper, MainController parent, URI uri,
            String[] headers, HttpClient client, HttpRequest.Builder request) {
        if (headers.length > 0)
            request.headers(headers);
        HttpRequest httpRequest = request.build();
        try {
            log.info("{} headers:{} , uri:{}", httpRequest.method(), mapper.writeValueAsString(headers), uri);
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (!httpResponse.body().startsWith("<")) {
                parent.getCodeJsonResponse().replaceText(
                        Json.pretty(mapper.readTree(httpResponse.body()))
                );
            } else {
                log.error("response does not look like a json,{},{}", httpResponse.statusCode(), httpResponse.body());
                parent.codeResponseXmlSettings(parent.getCodeJsonResponse(), "/css/xml-highlighting.css");
                parent.getCodeJsonResponse().replaceText(
                        prettyPrintByTransformer(httpResponse.body(), 4, true)
                );
            }
            parent.getCodeRawJsonResponse().setText(httpResponse.body());

        } catch (IOException | InterruptedException e) {
            log.error("Error in POST request:{}", e.getMessage(), e);
            parent.openDebugConsole();
            parent.getCodeJsonResponse().replaceText(e.getMessage());
        }
    }

    private String[] getHeaders(TableView<RequestHeader> tableHeaders) {
        List<String> headers = new ArrayList<>();
        tableHeaders.getItems().forEach(m -> {
            if (Boolean.TRUE.equals(m.getChecked())) {
                headers.add(m.getName());
                headers.add(m.getValue());
            }
        });
        return headers.toArray(String[]::new);
    }

    public static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {
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

    URI getUri(String uri, GridPane boxRequestParams) {
        String queryParams = boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof STextField ns && ns.getIn().equals("query"))
                .map(n -> ((STextField) n).getParamName() + "=" + ((STextField) n).getText())
                .collect(Collectors.joining("&"));

        AtomicReference<String> finalAddress = new AtomicReference<>(uri);

        boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof STextField && finalAddress.get().contains("{"))
                .forEach(n -> {
                    STextField node = (STextField) n;
                    finalAddress.set(finalAddress.get().replaceAll("\\{" + node.getParamName() + "}", node.getText()));
                });

        if (!queryParams.isEmpty()) {
            finalAddress.set(finalAddress.get() + "?" + queryParams);
        }

        return URI.create(finalAddress.get());
    }

    public void request(ObjectMapper mapper, MainController parent, String targetUri, PathItem.HttpMethod httpMethod) {
        URI uri = getUri(targetUri, parent.getBoxRequestParams());

        String[] headers = getHeaders(parent.getTableHeaders());
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(uri)
                .method(httpMethod.name(),
                        HttpRequest.BodyPublishers.ofString(parent.getCodeJsonRequest().getText()));

        sendRequestAndShowResponse(mapper, parent, uri, headers, client, request);
    }
}
