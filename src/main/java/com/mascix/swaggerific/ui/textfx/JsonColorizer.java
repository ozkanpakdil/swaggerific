package com.mascix.swaggerific.ui.textfx;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonColorizer {
    static final Pattern JSON_REGEX = Pattern.compile("(?<JSONCURLY>\\{|\\})|" +
            "(?<JSONPROPERTY>\\\".*\\\")\\s*:\\s*|" +
            "(?<JSONVALUE>\\\".*\\\")|" +
            "\\[(?<JSONARRAY>.*)\\]|" +
            "(?<JSONNUMBER>\\d+.?\\d*)|" +
            "(?<JSONBOOL>true|false)|" +
            "(?<JSONNULL>null)" +
            "(?<TEXT>.*)");

    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = JsonColorizer.JSON_REGEX.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass
                    = matcher.group("JSONPROPERTY") != null ? "json_property"
                    : matcher.group("JSONARRAY") != null ? "json_array"
                    : matcher.group("JSONCURLY") != null ? "json_curly"
                    : matcher.group("JSONBOOL") != null ? "json_bool"
                    : matcher.group("JSONNULL") != null ? "json_null"
                    : matcher.group("JSONNUMBER") != null ? "json_number"
                    : matcher.group("JSONVALUE") != null ? "json_value"
                    : matcher.group("TEXT") != null ? "text"
                    : null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
