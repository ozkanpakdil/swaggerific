package com.mascix.swaggerific.tools;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mascix.swaggerific.ui.STextField;
import com.mascix.swaggerific.ui.TreeItemOperatinLeaf;
import io.swagger.v3.core.util.Json;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class HttpUtility {

    @SneakyThrows
    public void postRequest(CodeArea codeJsonRequest, CodeArea codeJsonResponse, TextField txtAddress, GridPane boxRequestParams, ObjectMapper mapper) {
        URI uri = getUri(txtAddress, boxRequestParams);
        log.info("uri:{}", uri);
        //TODO not finished, make sure data send in body
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(codeJsonRequest.getText()))
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        codeJsonResponse.replaceText(
                Json.pretty(mapper.readTree(httpResponse.body()))
        );
    }

    @SneakyThrows
    public void getRequest(TreeView treePaths, CodeArea codeJsonResponse, TextField txtAddress, GridPane boxRequestParams, ObjectMapper mapper) {
        TreeItemOperatinLeaf selectedItem = (TreeItemOperatinLeaf) treePaths.getSelectionModel().getSelectedItem();
        URI uri = getUri(txtAddress, boxRequestParams);
        log.info("uri:{}", uri);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        codeJsonResponse.replaceText(
                Json.pretty(mapper.readTree(httpResponse.body()))
        );
    }

    URI getUri(TextField txtAddress, GridPane boxRequestParams) {
        final String[] queryParams = {""};
        final String[] adr = {txtAddress.getText()};
        boxRequestParams.getChildren().stream().forEach(n -> {
            if (n instanceof STextField) {
                STextField node = (STextField) n;
                if (node.getIn().equals("query")) {
                    queryParams[0] += node.getParamName() + "=" + node.getText() + "&";
                }
                if (adr[0].contains("{"))
                    adr[0] = adr[0].replaceAll("\\{" + node.getParamName() + "\\}", node.getText());
            }
        });
        if (!queryParams[0].isEmpty())
            adr[0] += "?" + queryParams[0];
        URI uri = URI.create(adr[0]);
        return uri;
    }
}
