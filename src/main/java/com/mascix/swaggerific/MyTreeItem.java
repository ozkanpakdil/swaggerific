package com.mascix.swaggerific;

import io.swagger.v3.oas.models.PathItem;
import javafx.scene.control.TreeItem;
import lombok.Data;

@Data
public class MyTreeItem extends TreeItem<String> {
    PathItem bindPathItem;
}
