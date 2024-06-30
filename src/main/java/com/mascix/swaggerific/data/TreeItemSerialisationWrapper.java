package com.mascix.swaggerific.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mascix.swaggerific.ui.component.TreeItemOperationLeaf;
import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.scene.control.TreeItem;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeItemSerialisationWrapper<T extends Serializable> implements Serializable {
    private transient TreeItem<T> item;
    private static final ObjectMapper mapper = new ObjectMapper();

    public TreeItemSerialisationWrapper(TreeItem<T> item) {
        this.item = item;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(mapper.writeValueAsString(serializeTreeItem(item)));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        String json = (String) in.readObject();
        this.item = deserializeTreeItem(mapper.readValue(json, Map.class));
    }

    private Map<String, Object> serializeTreeItem(TreeItem<T> treeItem) {
        Map<String, Object> map = new HashMap<>();
        map.put("value", treeItem.getValue());
        if (treeItem instanceof TreeItemOperationLeaf) {
            TreeItemOperationLeaf leaf = (TreeItemOperationLeaf) treeItem;
            map.put("queryItems", leaf.getQueryItems());
            map.put("methodParameters", leaf.getMethodParameters());
            map.put("uri", leaf.getUri());
            map.put("isLeaf", true);
        } else {
            map.put("isLeaf", false);
        }
        List<Map<String, Object>> children = treeItem.getChildren().stream()
                .map(this::serializeTreeItem)
                .toList();
        map.put("children", children);
        return map;
    }

    private TreeItem<T> deserializeTreeItem(Map<String, Object> map) {
        TreeItem<T> treeItem;
        if ((Boolean) map.get("isLeaf")) {
            TreeItemOperationLeaf leaf = TreeItemOperationLeaf.builder().build();
            leaf.setValue(map.get("value"));
            leaf.setQueryItems((List<String>) map.get("queryItems"));
            leaf.setMethodParameters(convertToParameters((List<Map<String, Object>>) map.get("methodParameters")));
            leaf.setUri((String) map.get("uri"));
            treeItem = leaf;
        } else {
            treeItem = new TreeItem<>((T) map.get("value"));
        }
        List<Map<String, Object>> children = (List<Map<String, Object>>) map.get("children");
        if (children != null) {
            for (Map<String, Object> childMap : children) {
                treeItem.getChildren().add(deserializeTreeItem(childMap));
            }
        }
        return treeItem;
    }

    private List<Parameter> convertToParameters(List<Map<String, Object>> parameterMaps) {
        if (parameterMaps == null)
            return null;
        return parameterMaps.stream()
                .map(this::convertToParameter)
                .toList();
    }

    private Parameter convertToParameter(Map<String, Object> map) {
        Parameter parameter = new Parameter();
        if (map != null) {
            parameter.setName((String) map.get("name"));
            parameter.setIn((String) map.get("in"));
        }
        return parameter;
    }

    private Object readResolve() throws ObjectStreamException {
        return item;
    }
}