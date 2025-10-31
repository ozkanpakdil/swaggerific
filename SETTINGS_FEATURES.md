### Settings and Features Documentation

This document tracks features, especially Settings, their implementation status, behavior, and persistence details. It will be continuously updated as features are implemented or refined.

Legend:
- [x] Implemented and persisted in user settings
- [~] Partially implemented (UI present, some functionality missing)
- [ ] Planned (UI placeholder or future work)

---

#### General Settings
- [x] Font Family
  - UI: General > Font Family (ComboBox)
  - Behavior: Preview shows selected font. On close/save, selected font is persisted.
  - Persistence: Preferences key `selected.font`
  - Notes: Applied live when closing the Settings window and persisted for next startup.

- [x] Font Size
  - UI: General > Fontsize (TextField)
  - Behavior: On close/save, font size is persisted.
  - Persistence: Preferences key `font.size`
  - Notes: Applied live when closing the Settings window and persisted for next startup.

- [x] Trim keys and values in request body
  - UI: ToggleSwitch `Trim keys and values in request body`
  - Behavior: When enabled, request body is trimmed before sending.
  - Persistence: Preferences key `http.trimRequestBody`
  - Applies: TabRequestController just before sending the request.

- [x] SSL certificate verification
  - UI: ToggleSwitch `SSL certificate verification`
  - Behavior: When disabled, the app trusts all certificates and disables hostname verification. When enabled, default verification applies.
  - Persistence: Preferences key `http.ssl.verify` (true means verification is ON). Mirrored to `disableSslValidation` used by ProxySettings.
  - Applies: HttpServiceImpl client creation; all HTTP requests. Triggers HttpClient recreation.

- [x] Request timeout (ms)
  - UI: TextField `Request timeout (ms)` (press Enter to save)
  - Behavior: Sets connection timeout for HTTP client.
  - Validation: Must be >= 0 ms.
  - Persistence: Preferences key `http.requestTimeoutMs`
  - Applies: HttpServiceImpl HttpClient connect timeout. Triggers HttpClient recreation.

- [x] Max response size (bytes)
  - UI: TextField `Max response size (bytes)` (press Enter to save)
  - Behavior: Enforced in HttpServiceImpl. Responses larger than the configured size are truncated and annotated; a response header `x-swaggerific-truncated: true` is added.
  - Validation: Must be >= 1024 bytes.
  - Persistence: Preferences key `http.maxResponseSizeBytes`
  - Applies: Response handling in HttpServiceImpl.

- [x] Send no-cache header
  - UI: ToggleSwitch `Send no-cache header`
  - Behavior: Auto-injects Cache-Control/Pragma/Expires headers when sending a request (if not already present).
  - Persistence: Preferences key `http.header.noCache`
  - Applies: At request assembly in TabRequestController.

- [x] Send swagger token header
  - UI: ToggleSwitch `Send swagger token header`
  - Behavior: Auto-injects header `X-Swagger-Token: true` when enabled (if not already present).
  - Persistence: Preferences key `http.header.swaggerToken`
  - Applies: At request assembly in TabRequestController.

- [x] Automatically follow redirects
  - UI: ToggleSwitch `Automatically follow redirects`
  - Behavior: Controls HttpClient redirect policy (NORMAL vs NEVER).
  - Persistence: Preferences key `http.followRedirects`
  - Applies: HttpServiceImpl client creation. Triggers HttpClient recreation.

- [x] Always open sidebar item in new tab
  - UI: ToggleSwitch `Always open sidebar item in new tab`
  - Behavior: When enabled, selecting an endpoint in the sidebar always opens a new tab; otherwise the tab for the same endpoint is reused.
  - Persistence: Preferences key `ui.sidebar.openInNewTab`
  - Applies: MainController.handleTreeViewItemClick reads the pref and creates a unique tab id when needed.

- [x] Always ask when closing unsaved tabs
  - UI: ToggleSwitch `Always ask when closing unsaved tabs`
  - Behavior: If enabled and a request tab has unsaved changes, a confirmation dialog appears when closing the tab.
  - Persistence: Preferences key `ui.askWhenClosingUnsaved`
  - Applies: MainController attaches a onCloseRequest handler per tab and checks TabRequestController.isDirty().

- [x] Send anonymous usage data
  - UI: ToggleSwitch `Send anonymous usage data`
  - Behavior: Opt-in anonymous telemetry. When enabled, the app may send minimal, non-PII usage events (startup, request completed) with short timeouts. No request bodies, URLs, or headers are transmitted; only method and status code for requests.
  - Persistence: Preferences keys `analytics.sendAnonymousUsage` (boolean), `analytics.anonymousId` (UUID generated on first use)
  - Endpoint: Disabled by default; can be provided via system property `swaggerific.telemetry.endpoint` (e.g., http://example/telemetry). If not set, telemetry is a no-op.
  - Applies: Runtime via TelemetryService (startup in SwaggerApplication; after responses in TabRequestController).

- [x] Restore Defaults
  - UI: Button `Back to default`
  - Behavior: Resets all above settings to defaults and recreates HTTP clients to apply network-related changes immediately.
  - Notes: Also clears font preferences.

---

#### HTTP/Proxy/Network
- [x] Central HTTP client configuration
  - Applies request timeout and redirect policy from preferences.
  - Honors SSL verification toggle via ProxySettings and trust-all SSL context.
  - Recreates clients when relevant preferences change.

- [x] Max response size enforcement
  - Enforced in HttpServiceImpl: responses larger than the configured size are truncated to the byte limit, `x-swaggerific-truncated: true` header is added, and a notice is appended to the body. Applies to all requests.

- [x] Localhost convenience logic
  - Improved handling for localhost/127.0.0.1 URIs in builder and tests. Avoids proxy, ensures direct connection.

- [x] Proxy Settings
  - UI: Settings > Proxy
  - Behavior: Configure how the app connects to the network for outgoing API requests.
    - Use system proxy: If enabled, defers to OS/browser proxy settings.
    - Custom proxy: Type (HTTP/HTTPS), Server, Port.
    - Proxy authentication: Toggle + username/password (password stored encrypted on disk in non-production modes; in production stored via Preferences).
    - Proxy bypass: Comma-separated hosts to skip proxy (e.g., localhost,127.0.0.1).
    - Disable SSL certificate validation: Trust-all certificates and disable hostname verification (for development/testing only).
  - Persistence: Stored via ProxySettings using user Preferences or a file in app settings dir (depending on environment). Keys include `useSystemProxy`, `proxyType`, `proxyServer`, `proxyPort`, `proxyAuth`, `proxyAuthUsername`, encrypted password, `proxyBypass`, and `disableSslValidation`.
  - Applies: SwaggerApplication.initializeProxySettings() installs a ProxySelector and HttpServiceImpl clients are recreated. HttpServiceImpl also respects `disableSslValidation` to create a trust-all SSL context.

---

#### Request Assembly
- [x] Environment variables resolution
  - Applies in URL, headers, parameters, and body when Pre-request Script controller is present.

- [x] Header injection from settings
  - No-cache and Swagger token toggles merge into outgoing headers without overriding user-provided ones.

- [x] Body trimming from settings
  - Applies just before sending.

- [x] Authentication merging
  - Authorization controller applies auth headers if configured.

---

#### Other Settings Sections
- [x] Themes
  - UI: Settings > Themes
  - Behavior: Choose Light or Dark theme. When "Apply immediately" is enabled, switching updates the UI instantly. Otherwise, setting persists and applies on next app start.
  - Accessibility: When Dark is selected, an additional stylesheet (css/dark-fixes.css) is applied to improve contrast/readability of the navigation tree and lists. The stylesheet now enforces lighter text for tree/list/table cells, higher-contrast hover/selection, and visible disclosure arrows in dark mode.
  - Dark editor fix: Pretty JSON editor (RichTextFX CodeArea) and Raw TextArea now adopt dark backgrounds and light text in dark mode, overriding the previous white editor background. Line-number gutter and JSON token colors are adjusted for readability. The caret current-line highlight has been softened to a subtle translucent tint so the line remains readable.
  - Persistence: Preferences keys `ui.theme` (values: `light` or `dark`), `ui.theme.applyImmediately` (boolean)
  - Applies: Global JavaFX user agent stylesheet via Atlantafx Primer themes + dark-fixes when in dark mode.

- [x] Shortcuts
  - UI: Settings > Shortcuts
  - Behavior: Edit keyboard shortcuts for common actions. Click a field then press the desired key combination to capture it. Save applies immediately across open windows; Reset restores defaults.
  - Actions supported in this increment:
    - Send Request (default: Ctrl+Enter) -> key `shortcut.btnSendRequest`
    - Toggle Debug Console (default: Ctrl+D) -> key `shortcut.flipDebugConsole`
    - JSON: Toggle fold at caret (default: Ctrl+-) -> key `shortcut.json.toggleFoldAtCaret`
    - JSON: Fold all top-level (default: Ctrl+9) -> key `shortcut.json.foldTop`
    - JSON: Unfold all (default: Ctrl+0) -> key `shortcut.json.unfoldAll`
  - Persistence: Stored in Preferences under keys with `shortcut.` prefix.
  - Applies: SwaggerApplication scans scenes and applies custom shortcuts to MenuItems/Buttons; installs scene-level accelerators for JSON folding actions on the focused Pretty editor (CustomCodeArea). Live-apply via Settings Save.

- [x] Data
  - UI: Settings > Data
  - Behavior: "Save request/response history" toggle and "History retention (days)" numeric field. When enabled, requests and responses are stored as JSON files under `~/.swaggerific/history`. On startup, entries older than the configured retention are purged automatically.
  - Persistence: Preferences keys `ui.data.saveHistory` (boolean), `ui.data.historyRetentionDays` (int, days)
  - Applies: Runtime via HistoryService (save on each request completion; purge at startup).

- [x] Addons
  - UI: Settings > Addons
  - Behavior: "Enable experimental addons" toggle, currently persists preference only.
  - Persistence: Preferences key `ui.addons.enabled` (boolean)
  - Applies: Future integrations can check this flag to enable experimental features.

- [x] Certificates
  - UI: Settings > Certificates
  - Behavior: "Enable custom CA bundle" toggle and path field for CA bundle (.pem). When enabled and a valid PEM is provided, HttpServiceImpl builds an SSLContext from the bundle and uses it for HTTPS requests. If disabled, default trust store applies. If "Disable SSL certificate validation" is enabled under Proxy, that takes precedence and uses trust-all (dev/test only).
  - Persistence: Preferences keys `certs.caBundleEnabled` (boolean), `certs.caBundlePath` (string)
  - Applies: HttpServiceImpl client creation. PEMs with multiple certs are supported.

- [x] Proxy
  - UI: Settings > Proxy
  - Behavior: Configure how the app connects to the network for outgoing API requests.
    - Use system proxy: If enabled, defers to OS/browser proxy settings.
    - Custom proxy: Type (HTTP/HTTPS), Server, Port.
    - Proxy authentication: Toggle + username/password (password stored encrypted on disk in non-production modes; in production stored via Preferences).
    - Proxy bypass: Comma-separated hosts to skip proxy (e.g., localhost,127.0.0.1).
    - Disable SSL certificate validation: Trust-all certificates and disable hostname verification (for development/testing only).
  - Persistence: Stored via ProxySettings using user Preferences or a file in app settings dir (depending on environment). Keys include `useSystemProxy`, `proxyType`, `proxyServer`, `proxyPort`, `proxyAuth`, `proxyAuthUsername`, encrypted password, `proxyBypass`, and `disableSslValidation`.
  - Applies: SwaggerApplication.initializeProxySettings() installs a ProxySelector and HttpServiceImpl clients are recreated. HttpServiceImpl also respects `disableSslValidation` to create a trust-all SSL context.

- [x] Update
  - UI: Settings > Update
  - Behavior: "Check for updates on startup" toggle stores preference. "Update channel" selects Stable or Beta.
  - Runtime: When enabled, the app performs a lightweight, privacy‑preserving check on startup against the GitHub Releases API (5s timeout). Channel controls whether to query the latest stable release or the releases feed (beta). Result is logged and can be surfaced in UI later.
  - Persistence: Preferences keys `ui.update.checkOnStartup` (boolean), `ui.update.channel` ("stable"|"beta").
  - Applies: On app startup via UpdateChecker.

- [x] About
  - UI: Settings > About
  - Behavior: Shows application name and version read from application.properties and provides links to Homepage and Issues.
  - Persistence: None required.

---

#### Persistence Summary
All implemented settings are saved under the user Preferences node for `io.github.ozkanpakdil.swaggerific.SwaggerApplication` and loaded on application start or when opening the General settings panel. Network-affecting settings trigger HttpClient recreation immediately.

Keys in use:
- `selected.font`
- `font.size`
- `http.trimRequestBody`
- `http.ssl.verify`
- `http.requestTimeoutMs`
- `http.maxResponseSizeBytes`
- `http.header.noCache`
- `http.header.swaggerToken`
- `http.followRedirects`
- `ui.sidebar.openInNewTab`
- `ui.askWhenClosingUnsaved`
- `analytics.sendAnonymousUsage`
- Mirror: `disableSslValidation` (consumed by ProxySettings)

---

#### Next Steps
- Shortcuts: consider exposing additional actions (e.g., open Settings, open Debug Console dock) and add sensible defaults.
- Theming: continue dark-mode polish for any remaining controls if reported by users.
