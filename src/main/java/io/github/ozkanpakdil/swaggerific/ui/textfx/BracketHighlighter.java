package io.github.ozkanpakdil.swaggerific.ui.textfx;

import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BracketHighlighter {
    // Use CodeArea reference for all RichTextFX/JavaFX API calls (macOS compatibility)
    private final CodeArea codeArea;
    private final List<BracketPair> bracketPairList = new ArrayList<>();
    private final List<String> matchStyle = List.of("match");
    private static final String BRACKET_PAIRS_REGEX = "[(){}\\[\\]<>]";

    /**
     * Parameterized constructor
     *
     * @param area the code area
     */
    public BracketHighlighter(CustomCodeArea area) {
        // Keep a reference to CustomCodeArea for custom hooks (insertion listener)
        this.codeArea = area; // CustomCodeArea extends CodeArea
        area.addTextInsertionListener((start, end, text) -> clearBracket());
        this.codeArea.caretPositionProperty()
                .addListener((obs, oldVal, newVal) -> Platform.runLater(() -> highlightBracket(newVal)));
    }

    /**
     * Highlight the matching bracket around the given caret position. Works when the caret is directly
     * after an opening/closing bracket or directly before one.
     *
     * @param caret the caret position
     */
    private void highlightBracket(int caret) {
        // Decide which character index to examine: prefer the character just before the caret if it is a bracket,
        // otherwise check the character at the caret (i.e., caret is before a bracket).
        Integer index = null;
        if (caret > 0) {
            String prev = codeArea.getText(caret - 1, caret);
            if (isBracket(prev)) index = caret - 1;
        }
        if (index == null && caret < codeArea.getLength()) {
            String next = codeArea.getText(caret, caret + 1);
            if (isBracket(next)) index = caret;
        }

        if (index == null) {
            clearBracket();
            return;
        }

        Integer matchIndex = getMatchingBracket(index);
        if (matchIndex != null) {
            // clear previously highlighted pair (if any) then apply new highlight
            clearBracket();
            List<String> startStyleAtPos = List.copyOf(codeArea.getStyleAtPosition(index));
            List<String> endStyleAtPos = List.copyOf(codeArea.getStyleAtPosition(matchIndex));
            BracketPair pair = new BracketPair(index, matchIndex, startStyleAtPos, endStyleAtPos);
            styleBrackets(pair, matchStyle);
            bracketPairList.add(pair);
        } else {
            clearBracket();
        }
    }

    private boolean isBracket(String ch) {
        return ch != null && ch.length() == 1 && ch.matches(BRACKET_PAIRS_REGEX);
    }

    /**
     * Find the matching bracket location, starting from a known bracket at index.
     *
     * @param index index of the known bracket
     * @return null or position of matching bracket
     */
    private Integer getMatchingBracket(int index) {
        if (index < 0 || index >= codeArea.getLength()) return null;

        char initialBracket = codeArea.getText(index, index + 1).charAt(0);
        String bracketPairs = "(){}[]<>";
        int pos = bracketPairs.indexOf(initialBracket); // "(){}[]<>"
        if (pos < 0) return null;

        // even positions are opening, odd are closing
        int dir = (pos % 2 == 0) ? +1 : -1; // +1 search forward, -1 backward
        char match = bracketPairs.charAt(pos + dir);

        int i = index + dir;
        int count = 1;
        while (i > -1 && i < codeArea.getLength()) {
            char c = codeArea.getText(i, i + 1).charAt(0);
            if (c == initialBracket) count++;
            else if (c == match) count--;
            if (count == 0) return i;
            i += dir;
        }
        return null;
    }

    /**
     * Highlight the matching bracket at current caret position
     */
    public void highlightBracket() {
        highlightBracket(codeArea.getCaretPosition());
    }

    /**
     * Clear the existing highlighted bracket styles
     */
    public void clearBracket() {
        Iterator<BracketPair> iterator = this.bracketPairList.iterator();
        while (iterator.hasNext()) {
            BracketPair pair = iterator.next();
            // Restore each bracket's original styles individually
            styleBracket(pair.start, pair.startStyleAtPosition());
            styleBracket(pair.end, pair.endStyleAtPosition());
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
            if (isBracket(text)) {
                codeArea.setStyle(pos, pos + 1, styles);
            }
        }
    }

    record BracketPair(int start, int end, List<String> startStyleAtPosition, List<String> endStyleAtPosition) {
    }
}