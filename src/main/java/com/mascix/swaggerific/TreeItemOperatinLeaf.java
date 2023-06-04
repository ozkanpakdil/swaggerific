package com.mascix.swaggerific;

import javafx.scene.control.TreeItem;
import lombok.Data;

import java.util.List;

@Data
public class TreeItemOperatinLeaf extends TreeItem<String> {
    List parameters;
}
