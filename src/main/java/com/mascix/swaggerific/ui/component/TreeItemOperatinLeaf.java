package com.mascix.swaggerific.ui.component;

import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.scene.control.TreeItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TreeItemOperatinLeaf extends TreeItem {
    private List<Parameter> methodParameters;
    private List<String> queryItems;
    private String uri;
}
