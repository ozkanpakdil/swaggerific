package com.mascix.swaggerific.ui.textfx;

import javafx.application.Platform;

import java.util.*;

public class SelectedHighlighter {
    private final CustomCodeArea codeArea;
    private final List<BracketPair> bracketPairList = new ArrayList<>();
    private final List<String> loopStyle = Collections.singletonList("loop");
    private final List<String> matchStyle = Arrays.asList("match", "loop");

    public SelectedHighlighter(CustomCodeArea codeArea) {
        this.codeArea = codeArea;
        this.codeArea.addTextInsertionListener((start, end, text) -> clearHighlighted());
        this.codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(() -> highlightSelectedText(newVal)));
    }

    private void highlightSelectedText(int newVal) {
        this.clearHighlighted();
        String selectedText = codeArea.getSelectedText();
        if (selectedText.length() > 0) {
            int start = codeArea.getSelection().getStart();
            int end = codeArea.getSelection().getEnd();
            if (start < end) {
                BracketPair pair = new BracketPair(start, end);
                styleBrackets(pair, matchStyle);
                this.bracketPairList.add(pair);
            }
            highlightAllOccurrencesOfSelectedText();
        } else
            clearHighlighted();
    }

    public void highlightAllOccurrencesOfSelectedText() {
        String selectedText = codeArea.getSelectedText();
        String text = codeArea.getText();

        int index = 0;
        while ((index = text.indexOf(selectedText, index)) != -1) {
            codeArea.setStyle(index, index + selectedText.length(), matchStyle);
            index += selectedText.length();
        }
    }

    public void highlightSelectedText() {
        this.highlightSelectedText(codeArea.getCaretPosition());
    }

    public void clearHighlighted() {
        Iterator<BracketPair> iterator = this.bracketPairList.iterator();
        while (iterator.hasNext()) {
            styleBrackets(iterator.next(), loopStyle);
            iterator.remove();
        }
    }

    private void styleBrackets(BracketPair pair, List<String> styles) {
        styleBracket(pair.start, styles);
        styleBracket(pair.end, styles);
    }

    private void styleBracket(int pos, List<String> styles) {
        if (pos < codeArea.getLength()) {
            codeArea.setStyle(pos, pos + 1, styles);
        }
    }

    static class BracketPair {
        private final int start;
        private final int end;

        public BracketPair(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

    }
}