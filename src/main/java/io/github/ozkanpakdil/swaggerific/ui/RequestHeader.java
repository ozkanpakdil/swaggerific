package io.github.ozkanpakdil.swaggerific.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Objects;

public class RequestHeader {
    private Boolean checked;
    private String name;
    private String value;

    // No-args constructor
    public RequestHeader() {
    }

    // All-args constructor
    public RequestHeader(Boolean checked, String name, String value) {
        this.checked = checked;
        this.name = name;
        this.value = value;
    }

    // Builder Pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean checked;
        private String name;
        private String value;

        public Builder checked(Boolean checked) {
            this.checked = checked;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public RequestHeader build() {
            return new RequestHeader(checked, name, value);
        }
    }

    // Getters and Setters
    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // JavaFX Property for binding
    public BooleanProperty checkedProperty() {
        return new SimpleBooleanProperty(checked);
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RequestHeader that = (RequestHeader) o;
        return Objects.equals(checked, that.checked) &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checked, name, value);
    }

    // toString method
    @Override
    public String toString() {
        return "RequestHeader{" +
                "checked=" + checked +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
