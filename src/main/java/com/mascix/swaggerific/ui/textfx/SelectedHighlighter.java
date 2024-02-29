package com.mascix.swaggerific.ui.textfx;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SelectedHighlighter {
    private final CustomCodeArea codeArea;
    private final List<SelectedTextCoordination> bracketPairList = new ArrayList<>();
    private final List<String> matchStyle = Arrays.asList("match", "loop");

    public SelectedHighlighter(CustomCodeArea codeArea) {
        this.codeArea = codeArea;
        this.codeArea.addTextInsertionListener((start, end, text) -> clearHighlighted());
        this.codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(this::highlightSelectedText));
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