package com.mascix.swaggerific.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
public class RequestHeader {
    Boolean checked;
    String name;
    String value;
    public BooleanProperty checkedProperty() {
        return new SimpleBooleanProperty(checked);
    }
}
