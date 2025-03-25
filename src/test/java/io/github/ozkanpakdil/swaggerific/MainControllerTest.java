package io.github.ozkanpakdil.swaggerific;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.swaggerific.data.SwaggerModal;
import io.github.ozkanpakdil.swaggerific.ui.MainController;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MainControllerTest {
    ObjectMapper mapper = new ObjectMapper();

    MainController testee = new MainController();

    void initializeFromInternet() {
        String webApiUrl = "https://petstore.swagger.io/v2/swagger.json";
        try {
            JsonNode jsonNode = mapper.readTree(new URL(webApiUrl));
            jsonNode.fieldNames().forEachRemaining(System.out::println);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initJsonFromFileReadTree() {
        File file = new File("src/test/resources/petstore-swagger.json");
        try {
            JsonNode jsonNode = mapper.readTree(file);
            jsonNode.fieldNames().forEachRemaining(System.out::println);
            List<String> enumList = StreamSupport.stream(jsonNode
                            .path("paths")
                            .path("/pet/findByStatus")
                            .path("get")
                            .path("parameters")
                            .get(0)
                            .path("items")
                            .path("enum")
                            .spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toList());
            System.out.println(enumList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void initJsonFromFile() throws IOException {
        File file = new File("src/test/resources/petstore-swagger.json");
        HashMap jsonNode = (HashMap) mapper.readValue(file, Map.class);
        assertEquals(((ArrayList<?>) jsonNode.get("tags")).size(), 3);
        assertEquals(((HashMap<?, ?>) jsonNode.get("paths")).size(), 14);
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

    @Test
    void stringEndsWithTest() {
        String t = "swagger.json";
        assertNotEquals(t.endsWith("swagger.json"), t.endsWith("(swagger.json|openapi.json)"));
        assertEquals(t.endsWith("swagger.json"), t.matches("(swagger.json|openapi.json)"));
    }
}