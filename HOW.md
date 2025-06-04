# Java HTTP Client Proxy Configuration

When running the application with proxy settings, you might need to set the following JVM system properties:

## `-Djdk.http.auth.proxying.disabledSchemes=""`

This system property enables all authentication schemes for HTTP proxy authentication. By default, some authentication
schemes might be disabled for security reasons. Setting this to an empty string enables all authentication schemes,
which might be necessary when working with proxies that require specific authentication methods.

## `-Djdk.http.auth.tunneling.disabledSchemes=""`

Similar to the above property, this enables all authentication schemes for HTTPS tunneling through HTTP proxies (CONNECT
requests). This is particularly important when accessing HTTPS resources through an authenticated proxy.

## `-Djdk.internal.httpclient.debug=true`

This enables debug logging for the Java HTTP Client, which can be helpful for troubleshooting proxy connection issues.
It will output detailed information about:

- Connection establishment
- Authentication attempts
- Proxy tunneling
- Request/response headers
- SSL/TLS handshaking

You can add these properties when running the application:

```bash
java -Djdk.http.auth.proxying.disabledSchemes="" \
     -Djdk.http.auth.tunneling.disabledSchemes="" \
     -Djdk.internal.httpclient.debug=true \
     -jar swaggerific.jar
```

