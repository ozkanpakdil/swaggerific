package com.mascix.swaggerific.ui.textfx;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonColorize {
    static final Pattern JSON_REGEX = Pattern.compile("(?<JSONCURLY>[{}])|" +
            "(?<JSONPROPERTY>\".*\")\\s*:\\s*|" +
            "(?<JSONVALUE>\".*\")|" +
            "\\[(?<JSONARRAY>.*)]|" +
            "(?<JSONNUMBER>\\d+.?\\d*)|" +
            "(?<JSONBOOL>true|false)|" +
            "(?<JSONNULL>null)" +
            "(?<TEXT>.*)");

    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = JsonColorize.JSON_REGEX.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass = null;
            if (matcher.group("JSONPROPERTY") != null) {
                styleClass = "json_property";
            } else if (matcher.group("JSONARRAY") != null) {
                styleClass = "json_array";
            } else if (matcher.group("JSONCURLY") != null) {
                styleClass = "json_curly";
            } else if (matcher.group("JSONBOOL") != null) {
                styleClass = "json_bool";
            } else if (matcher.group("JSONNULL") != null) {
                styleClass = "json_null";
            } else if (matcher.group("JSONNUMBER") != null) {
                styleClass = "json_number";
            } else if (matcher.group("JSONVALUE") != null) {
                styleClass = "json_value";
            } else if (matcher.group("TEXT") != null) {
                styleClass = "text";
            }
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
