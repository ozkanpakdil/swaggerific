package com.mascix.swaggerific.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mascix.swaggerific.ui.MainController;
import com.mascix.swaggerific.ui.RequestHeader;
import com.mascix.swaggerific.ui.STextField;
import com.mascix.swaggerific.ui.TreeItemOperatinLeaf;
import io.swagger.v3.core.util.Json;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class HttpUtility {

    @SneakyThrows
    public void postRequest(CodeArea codeJsonRequest, CodeArea codeJsonResponse, TextField txtAddress, GridPane boxRequestParams, ObjectMapper mapper, TableView<RequestHeader> tableHeaders) {
        URI uri = getUri(txtAddress, boxRequestParams);
        log.info("uri:{}", uri);
        String[] headers = getHeaders(tableHeaders);
        //TODO not finished, make sure data send in body
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .headers(headers)
                .POST(HttpRequest.BodyPublishers.ofString(codeJsonRequest.getText()))
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        codeJsonResponse.replaceText(
                Json.pretty(mapper.readTree(httpResponse.body()))
        );
    }

    private String[] getHeaders(TableView<RequestHeader> tableHeaders) {
        List<String> headers = new ArrayList<>();
        tableHeaders.getItems().forEach(m -> {
            headers.add(m.getName());
            headers.add(m.getValue());
        });
        return headers.toArray(new String[headers.size()]);
    }

    @SneakyThrows
    public void getRequest(TreeView treePaths, CodeArea codeJsonResponse, TextField txtAddress, GridPane boxRequestParams,
                           ObjectMapper mapper, TableView<RequestHeader> tableHeaders, MainController parent) {
        TreeItemOperatinLeaf selectedItem = (TreeItemOperatinLeaf) treePaths.getSelectionModel().getSelectedItem();
        URI uri = getUri(txtAddress, boxRequestParams);
        HttpClient client = HttpClient.newHttpClient();

        String[] headers = getHeaders(tableHeaders);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .headers(headers)
                .build();

        log.info("req:{}", request);

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        try {
            codeJsonResponse.replaceText(
                    Json.pretty(mapper.readTree(httpResponse.body()))
            );
        } catch (Exception ex) {
            log.error("response does not look like a json", ex);
            parent.codeResponseXmlSettings(codeJsonResponse);
            codeJsonResponse.replaceText(
                    prettyPrintByTransformer(httpResponse.body(), 4, false)
            );
        }
    }

    public String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {
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
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }

    URI getUri(TextField txtAddress, GridPane boxRequestParams) {
        String queryParams = boxRequestParams.getChildren().stream()
                .filter(n -> n instanceof STextField && ((STextField) n).getIn().equals("query"))
                .map(n -> ((STextField) n).getParamName() + "=" + ((STextField) n).getText())
                .collect(Collectors.joining("&"));

        String address = txtAddress.getText();
        AtomicReference<String> finalAddress = new AtomicReference<>(address);

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
}
