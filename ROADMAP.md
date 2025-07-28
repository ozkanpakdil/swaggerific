# Swaggerific Roadmap

## Project Overview

Swaggerific is an open-source UI for HTTP utilities, specifically designed for calling Swagger/REST functions and displaying results. The project aims to provide a comprehensive, user-friendly interface for API testing and interaction, with a focus on Swagger/OpenAPI integration.

## Vision

To become the best open-source UI for HTTP utilities, offering a modern, feature-rich alternative to commercial API testing tools with seamless Swagger/OpenAPI integration.

## Currently Implemented Features

### Core Functionality

- [x] **Swagger/OpenAPI Integration**
  - [x] Support for both Swagger 2.0 and OpenAPI 3.0 formats
  - [x] Automatic parsing and display of API endpoints in a tree view
  - [x] Extraction of API paths, methods, parameters, and other details

- [x] **HTTP Request Handling**
  - [x] Support for all HTTP methods (GET, POST, PUT, DELETE, etc.)
  - [x] Request parameter handling via form fields or JSON editor
  - [x] Custom header management
  - [x] Response formatting and display (JSON/XML)

- [x] **User Interface**
  - [x] Modern JavaFX UI with responsive design
  - [x] Tree view of endpoints for easy navigation
  - [x] Tab-based interface for handling multiple API requests
  - [x] Dockable debug console
  - [x] Syntax highlighting for request and response bodies
  - [x] Auto-completion for brackets and quotes in the JSON editor

### Advanced Features

- [x] **Pre-Request Scripts**
  - [x] JavaScript execution environment using GraalVM
  - [x] Postman-like API with `pm` object for scripting
  - [x] Variable management (`pm.variables.get()` and `pm.variables.set()`)
  - [x] Header modification (`pm.request.headers`)
  - [x] HTTP requests within scripts (`pm.sendRequest()`)
  - [x] Console logging (`console.log()`, `console.warn()`, `console.error()`)
  - [x] Variables persistence between script executions

- [x] **Proxy Support**
  - [x] System proxy integration
  - [x] Custom proxy configuration
  - [x] Proxy authentication
  - [x] Bypass rules for specific hosts

- [x] **Session Management**
  - [x] Persistence of tree structure between sessions
  - [x] Authorization settings storage
  - [x] Window position and size memory

- [x] **Customization**
  - [x] Keyboard shortcuts customization
  - [x] Font size and family selection
  - [x] UI component visibility toggles (tree, filter, status bar)

### Cross-Platform Support

- [x] **Runtime Environment**
  - [x] Java Development Kit (JDK) 21 or higher
  - [x] JavaFX (included in the distribution)
  - [x] GraalVM integration for JavaScript execution

- [x] **Distribution**
  - [x] Cross-platform run scripts
  - [x] Maven-based distribution packaging
  - [x] GitHub Actions for automated builds and releases

## Planned Features

### Short-term Goals

- [ ] **Enhanced Authentication**
  - [ ] OAuth 2.0 flow support
  - [ ] JWT token handling
  - [ ] API key management

- [ ] **Request Collections**
  - [ ] Save and organize requests in collections
  - [ ] Import/export collections
  - [ ] Share collections between team members

- [x] **Environment Variables**
  - [x] Define multiple environments (dev, staging, production)
  - [x] Switch between environments easily
  - [x] Environment-specific variables

- [ ] **Response Testing**
  - [ ] Assertions for response validation
  - [ ] Test scripts (similar to pre-request scripts but for response validation)
  - [ ] Test reports and history

### Medium-term Goals

- [ ] **Mock Server**
  - [ ] Create mock responses based on OpenAPI definitions
  - [ ] Customizable response templates
  - [ ] Delay and error simulation

- [ ] **Performance Testing**
  - [ ] Run multiple requests in parallel
  - [ ] Measure and display response times
  - [ ] Generate performance reports

- [ ] **Collaboration Features**
  - [ ] Team workspaces
  - [ ] Shared environments and variables
  - [ ] Comments and documentation

- [ ] **Enhanced UI**
  - [ ] Dark mode support
  - [ ] Customizable themes
  - [ ] Split view for request and response

### Long-term Vision

- [ ] **API Documentation**
  - [ ] Generate documentation from OpenAPI definitions
  - [ ] Interactive documentation with request examples
  - [ ] Export documentation to various formats

- [ ] **API Monitoring**
  - [ ] Schedule API health checks
  - [ ] Alerts for API failures
  - [ ] Performance monitoring over time

- [ ] **Integration with CI/CD**
  - [ ] Run API tests as part of CI/CD pipelines
  - [ ] Generate test reports for CI/CD systems
  - [ ] Automate API testing in development workflows

- [ ] **Plugin System**
  - [ ] Extend functionality with plugins
  - [ ] Community-contributed plugins
  - [ ] Custom request and response processors

## Development Roadmap

### Current Focus (2025 Q3)

- [x] Implement environment variables support
  - Added Environment and EnvironmentVariable classes
  - Created UI for managing environments and variables
  - Integrated with pre-request scripts via pm.environment API
  - Added variable substitution in requests (URL, parameters, headers, body)
- [ ] Stabilize and enhance the pre-request scripts feature
- [ ] Improve cross-platform compatibility
- [x] Enhance GraalVM integration for better JavaScript support
  - Enhanced JavaScript API with additional methods and properties
  - Implemented better error handling with line numbers and stack traces
  - Added support for JavaScript Promises
  - Added utility methods for JSON, string operations, and Base64 encoding/decoding
  - Enhanced console object with more methods (info, debug, trace, assert, table)
- [ ] Fix known issues and improve error handling

### Next Steps (2025 Q4)

- [ ] Implement request collections
- [x] Add environment variables support
- [ ] Enhance authentication mechanisms
- [ ] Improve test capabilities

### Future Directions (2026)

- [ ] Develop mock server functionality
- [ ] Add performance testing capabilities
- [ ] Implement collaboration features
- [ ] Enhance UI with additional themes and customization options

## Contributing

Swaggerific welcomes contributions from the community. Whether you're fixing bugs, adding features, or improving documentation, your help is appreciated. Please see the [CONTRIBUTING.md](CONTRIBUTING.md) file for guidelines on how to contribute.

## Feedback and Feature Requests

If you have suggestions for new features or improvements, please open an issue on the [GitHub repository](https://github.com/ozkanpakdil/swaggerific/issues/new/choose).