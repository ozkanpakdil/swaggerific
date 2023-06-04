package com.mascix.swaggerific;

import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.scene.control.TreeItem;
import lombok.Data;

import java.util.List;

@Data
public class TreeItemOperatinLeaf extends TreeItem<String> {
    private List<Parameter> parameters;
}
