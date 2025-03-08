package com.mascix.swaggerific.ui.component;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;

public class TreeFilter {

    private TreeItem<String> originalTreeItemRoot;

    public void filterTreeItems(TreeItem<String> root, String filterText) {
        if (originalTreeItemRoot == null) {
            originalTreeItemRoot = cloneTreeItem(root);
        }

        if (filterText == null || filterText.isEmpty()) {
            root.getChildren().setAll(originalTreeItemRoot.getChildren());
            root.getChildren().forEach(child -> child.setExpanded(true));
            return;
        }

        List<TreeItem<String>> filteredItems = new ArrayList<>();
        for (TreeItem<String> child : originalTreeItemRoot.getChildren()) {
            TreeItem<String> filteredChild = filterTreeItem(child, filterText);
            if (filteredChild != null) {
                filteredItems.add(filteredChild);
            }
        }

        root.getChildren().setAll(filteredItems);
        expandAll(root);
    }

    private TreeItem<String> cloneTreeItem(TreeItem<String> item) {
        TreeItem<String> clonedItem = new TreeItem<>(item.getValue());
        for (TreeItem<String> child : item.getChildren()) {
            clonedItem.getChildren().add(cloneTreeItem(child));
        }
        return clonedItem;
    }

    private TreeItem<String> filterTreeItem(TreeItem<String> item, String filterText) {
        TreeItem<String> filteredItem = new TreeItem<>(item.getValue());
        for (TreeItem<String> child : item.getChildren()) {
            TreeItem<String> filteredChild = filterTreeItem(child, filterText);
            if (filteredChild != null) {
                filteredItem.getChildren().add(filteredChild);
            }
        }

        if (!filteredItem.getChildren().isEmpty() || item.getValue().contains(filterText)) {
            return filteredItem;
        }

        return null;
    }

    public void expandAll(TreeItem<String> item) {
        item.setExpanded(true);
        for (TreeItem<String> child : item.getChildren()) {
            expandAll(child);
        }
    }

    public void collapseAll(TreeItem<String> item) {
        if (item != null) {
            for (TreeItem<String> child : item.getChildren()) {
                collapseAll(child);
            }
            if (item != originalTreeItemRoot) {
                item.setExpanded(false);
            }
        }
    }
}