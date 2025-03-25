package io.github.ozkanpakdil.swaggerific.data;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.Arrays;
import java.util.Objects;

public class SwaggerModal extends OpenAPI {
    String swagger;
    String host;
    String basePath;
    String[] schemes;

    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String[] getSchemes() {
        return schemes;
    }

    public void setSchemes(String[] schemes) {
        this.schemes = schemes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        SwaggerModal that = (SwaggerModal) o;
        return Objects.equals(swagger, that.swagger) && Objects.equals(host,
                that.host) && Objects.equals(basePath, that.basePath) && Objects.deepEquals(schemes,
                that.schemes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), swagger, host, basePath, Arrays.hashCode(schemes));
    }
}
