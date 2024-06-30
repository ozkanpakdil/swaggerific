package com.mascix.swaggerific.ui.component;

import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.scene.control.TreeItem;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class TreeItemOperationLeaf extends TreeItem {
    private List<Parameter> methodParameters;
    private List<String> queryItems;
    private String uri;
}
