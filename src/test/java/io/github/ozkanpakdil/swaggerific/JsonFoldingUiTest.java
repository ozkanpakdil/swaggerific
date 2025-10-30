package io.github.ozkanpakdil.swaggerific;

import io.github.ozkanpakdil.swaggerific.ui.textfx.CustomCodeArea;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

@ExtendWith(ApplicationExtension.class)
public class JsonFoldingUiTest {

    private CustomCodeArea area;

    @Start
    private void start(Stage stage) {
        area = new CustomCodeArea();
        Parent root = area;
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    @Test
    public void testToggleFoldAtCaret() throws Exception {
        String json = "{\n  \"a\": 1,\n  \"b\": {\n    \"c\": 2\n  }\n}";
        FxToolkit.setupFixture(() -> {
            area.replaceText(json);
            // place caret after top-level opening brace
            area.moveTo(1);
            area.toggleFoldAtCaret();
        });
        // after folding, placeholder should exist
        String text = area.getText();
        Assertions.assertTrue(text.contains(" … "), "Expected placeholder after folding");

        FxToolkit.setupFixture(() -> {
            area.toggleFoldAtCaret(); // unfold
        });
        String unfolded = area.getText();
        Assertions.assertTrue(unfolded.contains("\"b\""), "Expected original text after unfolding");
    }
}
