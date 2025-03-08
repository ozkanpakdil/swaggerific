package com.mascix.swaggerific.ui.component;

import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;

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
        // adding methods to leaves
        filteredItems.forEach(child -> child.getChildren().forEach(grandChild ->
                findAndAddLeaves(originalTreeItemRoot, grandChild)
        ));

        expandCollapseAll(root, true);
    }

    private void findAndAddLeaves(TreeItem<String> originalItem, TreeItem<String> currentNode) {
        originalItem
                .getChildren()
                .forEach(child -> {
                    if (child instanceof TreeItemOperationLeaf &&
                            child.getParent().getValue().equals(currentNode.getValue())) {
                        currentNode.getChildren().add(child);
                    }
                    findAndAddLeaves(child, currentNode);
                });
    }

    private TreeItem<String> cloneTreeItem(TreeItem<String> item) {
        TreeItem<String> clonedItem = getTreeItem(item);
        for (TreeItem<String> child : item.getChildren()) {
            clonedItem.getChildren().add(cloneTreeItem(child));
        }
        return clonedItem;
    }

    private TreeItem<String> filterTreeItem(TreeItem<String> item, String filterText) {
        TreeItem<String> filteredItem = getTreeItem(item);

        for (TreeItem<String> child : item.getChildren()) {
            TreeItem<String> filteredChild = filterTreeItem(child, filterText);
            if (filteredChild != null) {
                filteredItem.getChildren().add(filteredChild);
            }
        }

        if (!filteredItem.getChildren().isEmpty() ||
                item.getValue().toLowerCase().contains(filterText.toLowerCase())) {
            return filteredItem;
        }

        return null;
    }

    private static @NotNull TreeItem<String> getTreeItem(TreeItem<String> item) {
        TreeItem<String> clonedItem;
        if (item instanceof TreeItemOperationLeaf leaf) {
            clonedItem = TreeItemOperationLeaf.builder()
                    .uri(leaf.getUri())
                    .methodParameters(leaf.getMethodParameters())
                    .queryItems(leaf.getQueryItems())
                    .build();
            clonedItem.setValue(item.getValue());
        } else {
            clonedItem = new TreeItem<>(item.getValue());
        }
        return clonedItem;
    }

    public void expandCollapseAll(@NotNull TreeItem<String> item, boolean expand) {
        item.setExpanded(expand);
        for (TreeItem<String> child : item.getChildren()) {
            expandCollapseAll(child, expand);
        }
    }
}