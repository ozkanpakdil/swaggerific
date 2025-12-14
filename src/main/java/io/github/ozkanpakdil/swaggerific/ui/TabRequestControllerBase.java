package io.github.ozkanpakdil.swaggerific.ui;

import io.github.ozkanpakdil.swaggerific.ui.component.TreeItemOperationLeaf;

/**
 * Minimal abstraction so MainController can work with different implementations
 * (RichTextFX-based on JVM vs. TextArea-based on native-image).
 */
public interface TabRequestControllerBase {
    void initializeController(MainController parent, String uri, TreeItemOperationLeaf leaf);
    boolean isDirty();
}
