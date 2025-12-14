package io.github.ozkanpakdil.swaggerific.ui.textfx;

import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomCodeArea extends CodeArea {
    private final List<TextInsertionListener> insertionListeners;

    // Simple fold model storing the original inner text between braces
    private static final String FOLD_PLACEHOLDER = " … ";
    private final List<FoldRegion> folds = new ArrayList<>();

    /**
     * Returns true if the given paragraph starts with a foldable JSON object (i.e., first non-whitespace is '{'
     * and there is a matching closing brace).
     */
    public boolean isParagraphFoldable(int paragraph) {
        int idx = findFirstOpenBraceInParagraph(paragraph);
        if (idx < 0) return false;
        int close = findMatchingClosingBrace(idx);
        return close > idx + 1;
    }

    /** Returns true if the paragraph's first foldable region is currently folded. */
    public boolean isParagraphFolded(int paragraph) {
        int idx = findFirstOpenBraceInParagraph(paragraph);
        if (idx < 0 || idx >= getLength()) return false;
        // check placeholder after '{'
        return safeSubstring(idx + 1, idx + 1 + FOLD_PLACEHOLDER.length()).equals(FOLD_PLACEHOLDER);
    }

    /** Toggle folding for the foldable region that starts on the given paragraph. */
    public void toggleFoldAtParagraph(int paragraph) {
        int idx = findFirstOpenBraceInParagraph(paragraph);
        if (idx < 0 || idx >= getLength()) return;
        int closeIdx = findMatchingClosingBrace(idx);
        if (closeIdx < 0 || closeIdx <= idx + 1) return;
        // If already folded at this region, unfold
        FoldRegion existing = findExistingFoldAt(idx + 1, closeIdx);
        if (existing != null) {
            if (safeSubstring(idx + 1, idx + 1 + FOLD_PLACEHOLDER.length()).equals(FOLD_PLACEHOLDER)) {
                super.replaceText(idx + 1, idx + 1 + FOLD_PLACEHOLDER.length(), existing.originalInner);
            }
            folds.remove(existing);
            return;
        }
        // Fold
        String inner = safeSubstring(idx + 1, closeIdx);
        super.replaceText(idx + 1, closeIdx, FOLD_PLACEHOLDER);
        folds.add(new FoldRegion(idx + 1, inner));
    }

    public CustomCodeArea() {
        this.insertionListeners = new ArrayList<>();
    }

    public void addTextInsertionListener(TextInsertionListener listener) {
        insertionListeners.add(listener);
    }

    public void removeTextInsertionListener(TextInsertionListener listener) {
        insertionListeners.remove(listener);
    }

    @Override
    public void replaceText(int start, int end, String text) {
        // notify all listeners
        for (TextInsertionListener listener : insertionListeners) {
            listener.codeInserted(start, end, text);
        }

        // call super
        super.replaceText(start, end, text);
    }

    /**
    * Toggle folding at the caret. If inside a JSON object {...}, collapse inner content to a placeholder.
    * If already folded, it restores the original content.
    */
    public void toggleFoldAtCaret() {
        int caret = getCaretPosition();
        int openIdx = findEnclosingOpenBrace(caret);
        if (openIdx < 0) return;
        int closeIdx = findMatchingClosingBrace(openIdx);
        if (closeIdx < 0 || closeIdx <= openIdx + 1) return;

        // If already folded at this region, unfold
        FoldRegion existing = findExistingFoldAt(openIdx + 1, closeIdx);
        if (existing != null) {
            // verify placeholder still exists
            if (safeSubstring(openIdx + 1, openIdx + 1 + FOLD_PLACEHOLDER.length()).equals(FOLD_PLACEHOLDER)) {
                super.replaceText(openIdx + 1, openIdx + 1 + FOLD_PLACEHOLDER.length(), existing.originalInner);
            }
            folds.remove(existing);
            return;
        }

        // Fold: replace inner content with placeholder
        String inner = safeSubstring(openIdx + 1, closeIdx);
        super.replaceText(openIdx + 1, closeIdx, FOLD_PLACEHOLDER);
        folds.add(new FoldRegion(openIdx + 1, inner));
    }

    public void foldAllTopLevel() {
        // Fold only top-level JSON objects (depth 1)
        String text = getText();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                if (++depth == 1) {
                    int openIdx = i;
                    int closeIdx = findMatchingClosingBrace(openIdx);
                    if (closeIdx > openIdx + 1) {
                        String inner = safeSubstring(openIdx + 1, closeIdx);
                        super.replaceText(openIdx + 1, closeIdx, FOLD_PLACEHOLDER);
                        folds.add(new FoldRegion(openIdx + 1, inner));
                        // adjust i to after placeholder
                        i = openIdx + 1 + FOLD_PLACEHOLDER.length() - 1;
                    }
                }
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            } else if (c == '"') {
                // skip string content
                i = skipString(text, i);
            }
        }
    }

    public void unfoldAll() {
        // Unfold from last to first to preserve indices
        folds.stream()
                .sorted(Comparator.comparingInt(fr -> -fr.start))
                .forEach(fr -> {
                    if (safeSubstring(fr.start, fr.start + FOLD_PLACEHOLDER.length()).equals(FOLD_PLACEHOLDER)) {
                        super.replaceText(fr.start, fr.start + FOLD_PLACEHOLDER.length(), fr.originalInner);
                    }
                });
        folds.clear();
    }

    private FoldRegion findExistingFoldAt(int startInner, int closeIdx) {
        for (FoldRegion fr : folds) {
            if (fr.start == startInner) return fr;
        }
        return null;
    }

    private String safeSubstring(int start, int end) {
        start = Math.max(0, Math.min(start, getLength()));
        end = Math.max(0, Math.min(end, getLength()));
        if (end < start) return "";
        return getText(start, end);
    }

    private int findEnclosingOpenBrace(int fromIdx) {
        // search backwards, track string state
        boolean inString = false;
        for (int i = Math.min(fromIdx - 1, getLength() - 1); i >= 0; i--) {
            char c = getText(i, i + 1).charAt(0);
            if (c == '"') {
                if (!isEscaped(i)) inString = !inString;
            }
            if (!inString && c == '{') {
                int close = findMatchingClosingBrace(i);
                if (close > fromIdx || (close == fromIdx && fromIdx > i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findMatchingClosingBrace(int openIdx) {
        boolean inString = false;
        int depth = 0;
        for (int i = openIdx; i < getLength(); i++) {
            char c = getText(i, i + 1).charAt(0);
            if (c == '"') {
                if (!isEscaped(i)) inString = !inString;
            }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private int skipString(String text, int i) {
        int j = i + 1;
        while (j < text.length()) {
            char ch = text.charAt(j);
            if (ch == '"' && !isEscaped(text, j)) return j;
            j++;
        }
        return j - 1;
    }

    private boolean isEscaped(int index) { // use current area text
        return isEscaped(getText(), index);
    }

    private boolean isEscaped(String text, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && text.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 1;
    }

    // Find the index of the first '{' in the given paragraph, ignoring characters inside strings
    private int findFirstOpenBraceInParagraph(int paragraph) {
        if (paragraph < 0) return -1;
        final int start;
        final int end;
        try {
            start = getAbsolutePosition(paragraph, 0);
            end = getAbsolutePosition(paragraph, getParagraphLength(paragraph));
        } catch (Exception ex) {
            return -1;
        }
        boolean inString = false;
        for (int i = start; i < end && i < getLength(); i++) {
            char c = getText(i, i + 1).charAt(0);
            if (c == '"') {
                if (!isEscaped(i)) inString = !inString;
            }
            if (!inString && c == '{') return i;
        }
        return -1;
    }

    private static class FoldRegion {
        final int start; // start index of inner content (after '{') at the time of folding
        final String originalInner;
        FoldRegion(int start, String originalInner) {
            this.start = start;
            this.originalInner = originalInner;
        }
    }
}
