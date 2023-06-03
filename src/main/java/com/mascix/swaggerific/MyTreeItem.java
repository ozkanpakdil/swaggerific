package com.mascix.swaggerific;

import javafx.scene.control.TreeItem;
import lombok.Data;

import java.util.List;

@Data
public class MyTreeItem extends TreeItem<String> {
    List bindPathItem;
}
