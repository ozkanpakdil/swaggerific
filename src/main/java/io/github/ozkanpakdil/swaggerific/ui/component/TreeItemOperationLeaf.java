package io.github.ozkanpakdil.swaggerific.ui.component;

import io.swagger.v3.oas.models.parameters.Parameter;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.Objects;

public class TreeItemOperationLeaf extends TreeItem<String> {
    private List<Parameter> methodParameters;
    private List<String> queryItems;
    private String uri;

    public TreeItemOperationLeaf() {
        super();
    }

    public TreeItemOperationLeaf(String value) {
        super(value);
    }

    public TreeItemOperationLeaf(String value, List<Parameter> methodParameters, List<String> queryItems, String uri) {
        super(value);
        this.methodParameters = methodParameters;
        this.queryItems = queryItems;
        this.uri = uri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String value;
        private List<Parameter> methodParameters;
        private List<String> queryItems;
        private String uri;

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder methodParameters(List<Parameter> methodParameters) {
            this.methodParameters = methodParameters;
            return this;
        }

        public Builder queryItems(List<String> queryItems) {
            this.queryItems = queryItems;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public TreeItemOperationLeaf build() {
            return new TreeItemOperationLeaf(value, methodParameters, queryItems, uri);
        }
    }

    // Getters and Setters
    public List<Parameter> getMethodParameters() {
        return methodParameters;
    }

    public void setMethodParameters(List<Parameter> methodParameters) {
        this.methodParameters = methodParameters;
    }

    public List<String> getQueryItems() {
        return queryItems;
    }

    public void setQueryItems(List<String> queryItems) {
        this.queryItems = queryItems;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TreeItemOperationLeaf that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(methodParameters, that.methodParameters) &&
                Objects.equals(queryItems, that.queryItems) &&
                Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), methodParameters, queryItems, uri);
    }

    // toString method
    @Override
    public String toString() {
        return "TreeItemOperationLeaf{" +
                "value='" + getValue() + '\'' +
                ", methodParameters=" + methodParameters +
                ", queryItems=" + queryItems +
                ", uri='" + uri + '\'' +
                '}';
    }
}
