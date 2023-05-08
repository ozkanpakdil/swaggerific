package com.mascix.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {
    ObjectMapper mapper = new ObjectMapper();

    MainController testee = new MainController();

    void initializeFromInternet() {
        String webApiUrl = "https://petstore.swagger.io/v2/swagger.json";
        try {
            JsonNode jsonNode = mapper.readTree(new URL(webApiUrl));
            jsonNode.fieldNames().forEachRemaining(c -> {
                System.out.println(c);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initJsonFromFileReadTree() {
        File file = new File("src/test/resources/petstore-swagger.json");
        try {
            JsonNode jsonNode = mapper.readTree(file);
            jsonNode.fieldNames().forEachRemaining(c -> {
                System.out.println(c);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initJsonFromFile() throws IOException {
        File file = new File("src/test/resources/petstore-swagger.json");
        HashMap<String, Object> jsonNode = (HashMap) mapper.readValue(file, Map.class);
        assertEquals(((ArrayList) jsonNode.get("tags")).size(), 3);
        assertEquals(((HashMap) jsonNode.get("paths")).size(), 14);
    }

    @Test
    void initJsonFromFileWithoutSwagger() throws IOException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File file = new File("src/test/resources/petstore-swagger.json");
        SwaggerModal read = mapper.readValue(file, SwaggerModal.class);
        read.getTags().forEach(it -> {
            TreeItem<String> tag = new TreeItem<>();
            tag.setValue(it.getName());
        });
        read.getPaths().forEach((it, pathItem) -> {
            TreeItem<String> tag = new TreeItem<>();
            tag.setValue(pathItem.get$ref());
        });
    }
}