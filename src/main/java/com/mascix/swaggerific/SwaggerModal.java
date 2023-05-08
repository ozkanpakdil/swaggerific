package com.mascix.swaggerific;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;

@Data
public class SwaggerModal extends OpenAPI {
    String swagger;
    String host;
    String basePath;
    String[] schemes;
}
