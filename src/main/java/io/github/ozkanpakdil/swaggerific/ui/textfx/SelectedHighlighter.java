package io.github.ozkanpakdil.swaggerific.ui.textfx;

import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SelectedHighlighter {
    // Use CodeArea-typed reference for RichTextFX/JavaFX APIs to avoid platform linkage issues
    private final CodeArea codeArea;
    private final List<SelectedTextCoordination> bracketPairList = new ArrayList<>();
    private final List<String> matchStyle = Arrays.asList("match", "loop");

    public SelectedHighlighter(CustomCodeArea area) {
        // Keep a reference to CustomCodeArea for custom hooks
        this.codeArea = area; // CustomCodeArea extends CodeArea
        area.addTextInsertionListener((start, end, text) -> clearHighlighted());
        this.codeArea.caretPositionProperty()
                .addListener((obs, oldVal, newVal) -> Platform.runLater(this::highlightSelectedText));
    }

    public void highlightAllOccurrencesOfSelectedText() {
        String selectedText = codeArea.getSelectedText().toLowerCase();
        String text = codeArea.getText().toLowerCase();

        int index = 0;
        while ((index = text.indexOf(selectedText, index)) != -1) {
            SelectedTextCoordination pair = new SelectedTextCoordination(index, index + selectedText.length(),
                    List.copyOf(codeArea.getStyleAtPosition(index)));
            codeArea.setStyle(pair.start, pair.end, matchStyle);
            index += selectedText.length();
            bracketPairList.add(pair);
        }
    }

    public void highlightSelectedText() {
        clearHighlighted();
        String selectedText = codeArea.getSelectedText();
        if (!selectedText.isEmpty()) {
            highlightAllOccurrencesOfSelectedText();
        }
    }

    public void clearHighlighted() {
        Iterator<SelectedTextCoordination> iterator = this.bracketPairList.iterator();
        while (iterator.hasNext()) {
            SelectedTextCoordination next = iterator.next();
            styleBrackets(next, next.styleAtPosition());
            iterator.remove();
        }
    }

    private void styleBrackets(SelectedTextCoordination pair, List<String> styles) {
        codeArea.setStyle(pair.start, pair.end, styles);
    }

    record SelectedTextCoordination(int start, int end, List<String> styleAtPosition) {
    }
}