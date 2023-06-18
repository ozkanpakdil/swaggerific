package com.mascix.swaggerific.ui.component;

import javafx.scene.control.TextField;
import lombok.Data;

@Data
public class STextField extends TextField {
    private String paramName;
    private String in;
}
