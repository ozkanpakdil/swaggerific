package io.github.ozkanpakdil.swaggerific.ui.component;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class TextAreaAppender extends OutputStreamAppender<ILoggingEvent> {

    // Avoid doing too much work at once. Tune this value as needed.
    private static final int MAX_APPEND = 100;

    private final Queue<ILoggingEvent> eventQueue = new ArrayDeque<>();
    private boolean notify = true;

    private final TextArea textArea;
    private final boolean open = true;

    public TextAreaAppender(TextArea textArea) {
        this.textArea = textArea;
        setOutputStream(new TextAreaOutputStream());
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (open) {
            if (Platform.isFxApplicationThread()) {
                appendEvent(event);
            } else {
                synchronized (eventQueue) {
                    eventQueue.add(event);
                    if (notify) {
                        notify = false;
                        notifyFxThread();
                    }
                }
            }
        }
    }

    private void notifyFxThread() {
        try {
            Platform.runLater(this::processQueue);
        } catch (Exception ex) {
            addError("Error processing log queue", ex);
        }
    }

    private void processQueue() {
        List<ILoggingEvent> events = new ArrayList<>();
        synchronized (eventQueue) {
            while (!eventQueue.isEmpty() && events.size() < MAX_APPEND) {
                events.add(eventQueue.remove());
            }

            if (eventQueue.isEmpty()) {
                notify = true;
            } else {
                notifyFxThread();
            }
        }
        events.forEach(this::appendEvent);
    }

    private void appendEvent(ILoggingEvent event) {
        super.append(event);
    }

    private class TextAreaOutputStream extends OutputStream {
        @Override
        public void write(int b) {
            textArea.appendText(String.valueOf((char) b));
            textArea.setScrollTop(Double.MAX_VALUE);
        }
    }
}