package io.github.ozkanpakdil.swaggerific.ui.component;

import javafx.scene.control.TextField;

import java.util.Objects;

public class STextField extends TextField {
    private String paramName;
    private String in;

    public STextField() {
        super();
    }

    public STextField(String text) {
        super(text);
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof STextField that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(paramName, that.paramName) &&
                Objects.equals(in, that.in);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), paramName, in);
    }

    @Override
    public String toString() {
        return "STextField{" +
                "text=" + getText() +
                ", paramName='" + paramName + '\'' +
                ", in='" + in + '\'' +
                '}';
    }
}
