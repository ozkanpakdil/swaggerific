package com.mascix.swaggerific.data;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SwaggerModal extends OpenAPI {
    String swagger;
    String host;
    String basePath;
    String[] schemes;
}
