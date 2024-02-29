package com.mascix.swaggerific.ui.textfx;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BracketHighlighter {
    private final CustomCodeArea codeArea;
    private final List<BracketPair> bracketPairList = new ArrayList<>();
    private final List<String> matchStyle = List.of("match");
    private static final String bracketPairsRegex = "[(){}\\[\\]<>]";

    /**
     * Parameterized constructor
     *
     * @param codeArea the code area
     */
    public BracketHighlighter(CustomCodeArea codeArea) {
        this.codeArea = codeArea;
        this.codeArea.addTextInsertionListener((start, end, text) -> clearBracket());
        this.codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> Platform.runLater(() -> highlightBracket(newVal)));
    }

    /**
     * Highlight the matching bracket at new caret position
     *
     * @param newVal the new caret position
     */
    private void highlightBracket(int newVal) {
        this.clearBracket();
        String prevChar = (newVal > 0) ? codeArea.getText(newVal - 1, newVal) : "";
        if (prevChar.matches(bracketPairsRegex)) --newVal;
        Integer other = getMatchingBracket(newVal);

        if (other != null) {
            BracketPair pair = new BracketPair(newVal, other, List.copyOf(codeArea.getStyleAtPosition(newVal)));
            styleBrackets(pair, matchStyle);
            this.bracketPairList.add(pair);
        }
    }

    /**
     * Find the matching bracket location.
     *
     * @param index to start searching from
     * @return null or position of matching bracket
     */
    private Integer getMatchingBracket(int index) {
        if (index == codeArea.getLength()) return null;

        char initialBracket = codeArea.getText(index, index + 1).charAt(0);
        String bracketPairs = "(){}[]<>";
        int bracketTypePosition = bracketPairs.indexOf(initialBracket); // "(){}[]<>"
        if (bracketTypePosition < 0) return null;

        // even numbered bracketTypePositions are opening brackets, and odd positions are closing
        // if even (opening bracket) then step forwards, otherwise step backwards
        int stepDirection = (bracketTypePosition % 2 == 0) ? +1 : -1;

        // the matching bracket to look for, the opposite of initialBracket
        char match = bracketPairs.charAt(bracketTypePosition + stepDirection);

        index += stepDirection;
        int bracketCount = 1;

        while (index > -1 && index < codeArea.getLength()) {
            char code = codeArea.getText(index, index + 1).charAt(0);
            if (code == initialBracket) bracketCount++;
            else if (code == match) bracketCount--;
            if (bracketCount == 0) return index;
            else index += stepDirection;
        }

        return null;
    }

    /**
     * Highlight the matching bracket at current caret position
     */
    public void highlightBracket() {
        clearBracket();
        this.highlightBracket(codeArea.getCaretPosition());
    }

    /**
     * Clear the existing highlighted bracket styles
     */
    public void clearBracket() {
        Iterator<BracketPair> iterator = this.bracketPairList.iterator();
        while (iterator.hasNext()) {
            BracketPair next = iterator.next();
            styleBrackets(next, next.styleAtPosition());
            iterator.remove();
        }
    }

    private void styleBrackets(BracketPair pair, List<String> styles) {
        styleBracket(pair.start, styles);
        styleBracket(pair.end, styles);
    }

    private void styleBracket(int pos, List<String> styles) {
        if (pos < codeArea.getLength()) {
            String text = codeArea.getText(pos, pos + 1);
            if (text.matches(bracketPairsRegex)) {
                codeArea.setStyle(pos, pos + 1, styles);
            }
        }
    }

    record BracketPair(int start, int end, List<String> styleAtPosition) {
    }
}