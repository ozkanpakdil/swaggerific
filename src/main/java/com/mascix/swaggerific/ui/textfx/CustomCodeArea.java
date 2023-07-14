package com.mascix.swaggerific.ui.textfx;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.EditableStyledDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomCodeArea  extends CodeArea {
    private final List<TextInsertionListener> insertionListeners;

    public CustomCodeArea(EditableStyledDocument<Collection<String>, String, Collection<String>> document) {
        super(document);
        this.insertionListeners = new ArrayList<>();
    }

    public CustomCodeArea() {
        this.insertionListeners = new ArrayList<>();
    }

    public CustomCodeArea(String text) {
        super(text);
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
}
