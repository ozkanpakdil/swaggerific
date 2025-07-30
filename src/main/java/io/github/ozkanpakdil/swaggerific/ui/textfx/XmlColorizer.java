package io.github.ozkanpakdil.swaggerific.ui.textfx;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlColorizer {
    private static final Pattern XML_TAG = Pattern.compile("(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
            + "|(?<COMMENT><!--[^<>]+-->)");

    private final Pattern attributes = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");

    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = XML_TAG.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else {
                if (matcher.group("ELEMENT") != null) {
                    int groupAttributesSection = 4;
                    String attributesText = matcher.group(groupAttributesSection);

                    int groupOpenBracket = 2;
                    String tagmark = "tagmark";
                    spansBuilder.add(Collections.singleton(tagmark),
                            matcher.end(groupOpenBracket) - matcher.start(groupOpenBracket));
                    int groupElementName = 3;
                    spansBuilder.add(Collections.singleton("anytag"),
                            matcher.end(groupElementName) - matcher.end(groupOpenBracket));

                    if (!attributesText.isEmpty()) {
                        lastKwEnd = 0;

                        Matcher amatcher = attributes.matcher(attributesText);
                        while (amatcher.find()) {
                            spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);
                            int groupAttributeName = 1;
                            spansBuilder.add(Collections.singleton("attribute"),
                                    amatcher.end(groupAttributeName) - amatcher.start(groupAttributeName));
                            int groupEqualSymbol = 2;
                            spansBuilder.add(Collections.singleton(tagmark),
                                    amatcher.end(groupEqualSymbol) - amatcher.end(groupAttributeName));
                            int groupAttributeValue = 3;
                            spansBuilder.add(Collections.singleton("avalue"),
                                    amatcher.end(groupAttributeValue) - amatcher.end(groupEqualSymbol));
                            lastKwEnd = amatcher.end();
                        }
                        if (attributesText.length() > lastKwEnd)
                            spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
                    }

                    lastKwEnd = matcher.end(groupAttributesSection);

                    int groupCloseBracket = 5;
                    spansBuilder.add(Collections.singleton(tagmark), matcher.end(groupCloseBracket) - lastKwEnd);
                }
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
