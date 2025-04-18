package io.github.ozkanpakdil.swaggerific.tools.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public record HttpRequest(URI uri, String method, Map<String, String> headers, String body) {
    public static final class Builder {
        private URI uri;
        private String method;
        private Map<String, String> headers = new HashMap<>();
        private String body;

        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public HttpRequest build() {
            if (uri == null || method == null) {
                throw new IllegalStateException("URI and method must be set");
            }
            return new HttpRequest(uri, method, headers, body);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}