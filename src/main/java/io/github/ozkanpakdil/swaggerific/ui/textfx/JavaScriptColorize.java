package io.github.ozkanpakdil.swaggerific.ui.textfx;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides syntax highlighting for JavaScript code.
 */
public class JavaScriptColorize {
    // Define regex patterns for JavaScript syntax elements
    private static final String KEYWORD_PATTERN = "\\b(var|let|const|function|return|if|else|for|while|do|switch|case|break|continue|new|this|typeof|instanceof|null|undefined|true|false|try|catch|finally|throw|class|extends|super|import|export|from|as|async|await)\\b";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`([^`\\\\]|\\\\.)*`";
    private static final String COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b";
    private static final String FUNCTION_PATTERN = "\\b([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(";
    private static final String BRACKET_PATTERN = "[\\[\\]{}()]";
    private static final String OPERATOR_PATTERN = "[+\\-*/=<>!&|^~%]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String PROPERTY_PATTERN = "\\.[a-zA-Z_$][a-zA-Z0-9_$]*";

    // Combine all patterns into a single regex with named groups
    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
            + "|(?<FUNCTION>" + FUNCTION_PATTERN + ")"
            + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
            + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")"
            + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
            + "|(?<PROPERTY>" + PROPERTY_PATTERN + ")"
    );

    /**
     * Computes syntax highlighting for the given JavaScript code.
     *
     * @param text The JavaScript code to highlight
     * @return StyleSpans containing the style information for the text
     */
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass = null;
            if (matcher.group("KEYWORD") != null) {
                styleClass = "js_keyword";
            } else if (matcher.group("STRING") != null) {
                styleClass = "js_string";
            } else if (matcher.group("COMMENT") != null) {
                styleClass = "js_comment";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "js_number";
            } else if (matcher.group("FUNCTION") != null) {
                styleClass = "js_function";
            } else if (matcher.group("BRACKET") != null) {
                styleClass = "js_bracket";
            } else if (matcher.group("OPERATOR") != null) {
                styleClass = "js_operator";
            } else if (matcher.group("SEMICOLON") != null) {
                styleClass = "js_semicolon";
            } else if (matcher.group("PROPERTY") != null) {
                styleClass = "js_property";
            }

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}