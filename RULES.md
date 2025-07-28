# Swaggerific AI Interaction Rules

## Project Overview

Swaggerific is an open-source UI for HTTP utilities, specifically designed for calling Swagger/REST functions and displaying results. The project aims to provide a comprehensive, user-friendly interface for API testing and interaction, with a focus on Swagger/OpenAPI integration.

The vision is to become the best open-source UI for HTTP utilities, offering a modern, feature-rich alternative to commercial API testing tools with seamless Swagger/OpenAPI integration.

## Tech Stack

### Core Technologies

- **Java**: JDK 21 or higher is required
- **JavaFX**: The primary UI framework
  - JavaFX Controls, Graphics, FXML, Web, Base, Media, and Swing components
  - AtlantaFX for modern theming (currently using PrimerLight theme)
  - ControlsFX for additional UI controls
  - FormsFX for form handling
  - ValidatorFX for input validation
  - Ikonli for icon support (FontAwesome and FontAwesome5)
  - BootstrapFX for Bootstrap-like styling
  - RichTextFX for enhanced text editing with syntax highlighting
  - DockFX for dockable UI components

### API and Data Handling

- **Swagger/OpenAPI**: Core libraries for parsing API specifications
  - io.swagger.v3.oas.models
  - io.swagger.v3.core
- **Jackson**: For JSON data binding and processing
- **Java HTTP Client**: For making API requests
- **Jakarta WS RS**: For RESTful web services support

### Scripting and Automation

- **GraalVM**: For JavaScript execution in pre-request scripts
  - org.graalvm.sdk
  - org.graalvm.truffle
  - Java Scripting API (javax.script)

### Utilities and Support

- **Logging**: SLF4J with Logback implementation
- **Apache Commons Lang3**: For utility functions
- **AspectJ Runtime**: For aspect-oriented programming
- **Java Preferences API**: For storing user preferences

### Build and Distribution

- **Maven**: For project building and dependency management
- **GitHub Actions**: For CI/CD and automated releases

## UI/UX Guidelines

When working on Swaggerific, adhere to these UI/UX principles:

### Visual Design

1. **Modern and Clean Interface**
   - Use the AtlantaFX theming system (currently PrimerLight)
   - Maintain consistent spacing and alignment
   - Follow JavaFX styling conventions with CSS

2. **Responsive Design**
   - UI components should adapt to window resizing
   - Use appropriate layout managers (VBox, HBox, GridPane, etc.)
   - Support different screen resolutions and DPI settings

3. **User Customization**
   - Support font size and family customization
   - Allow keyboard shortcut customization
   - Provide toggles for UI component visibility (tree, filter, status bar)

4. **Visual Feedback**
   - Provide loading indicators for long-running operations
   - Use appropriate error messages and dialogs
   - Highlight active elements and selections

### Interaction Design

1. **Intuitive Navigation**
   - Tree view for API endpoints with clear hierarchy
   - Tab-based interface for multiple requests
   - Dockable components for flexible workspace arrangement

2. **Efficient Workflows**
   - Support keyboard shortcuts for common actions
   - Provide context menus for quick access to relevant functions
   - Minimize the number of clicks required for common tasks

3. **Error Handling**
   - User-friendly error messages
   - Guidance for resolving issues
   - Detailed logging for debugging

4. **Accessibility**
   - Support screen readers where possible
   - Ensure keyboard navigation works properly
   - Maintain adequate contrast ratios

## Development Constraints

When developing for Swaggerific, adhere to these technical constraints:

1. **Architecture**
   - Follow SOLID principles
   - Maintain clean separation between UI and HTTP service layers
   - Use modular design with well-defined interfaces
   - Follow the MVC pattern with FXML for views and controllers for logic

2. **JavaFX Patterns**
   - Use FXML for UI definition where appropriate
   - Follow the controller pattern for UI logic
   - Use property binding for reactive UI updates
   - Leverage JavaFX CSS for styling

3. **Cross-Platform Compatibility**
   - Ensure code works on Windows, macOS, and Linux
   - Use platform-independent APIs when possible
   - Handle platform-specific issues with appropriate abstractions

4. **Performance Considerations**
   - Run long operations in background threads
   - Use Platform.runLater() for UI updates from non-JavaFX threads
   - Optimize memory usage for large responses
   - Consider startup time and resource usage

5. **Code Style**
   - Follow Java coding conventions
   - Use meaningful variable and method names
   - Include JavaDoc comments for public APIs
   - Write clear and concise code with appropriate logging

## AI Interaction Guidelines

When requesting AI assistance for Swaggerific development, follow these guidelines:

### General Approach

1. **Be Specific About Context**
   - Mention you're working on Swaggerific, a JavaFX-based Swagger/REST client
   - Specify which part of the application you're working on (UI, HTTP service, scripting, etc.)
   - Reference relevant files or components

2. **Clarify Technical Requirements**
   - Specify that solutions should use JavaFX for UI components
   - Mention the need for cross-platform compatibility
   - Indicate if the solution should follow existing patterns in the codebase

3. **Emphasize Visual Quality**
   - Request visually appealing solutions that match the existing design
   - Ask for CSS styling that aligns with the AtlantaFX theme system
   - Specify if animations or transitions are desired

### Request Formulation

1. **Structure Your Requests**
   - Start with a clear problem statement
   - Provide relevant context and constraints
   - Specify expected outcomes or acceptance criteria

2. **Include Relevant Details**
   - Java/JavaFX version (JDK 21+)
   - Existing code snippets or file references
   - UI/UX requirements or mockups

3. **Specify Output Format**
   - Ask for complete code solutions with imports
   - Request CSS styling where appropriate
   - Ask for explanations of complex logic or patterns

## Common Request Patterns

Here are examples of effective AI requests for Swaggerific development:

### UI Component Development

```
I need to create a new JavaFX component for Swaggerific that displays API response headers in a collapsible tree view. The component should:
- Match the existing AtlantaFX PrimerLight theme
- Support expanding/collapsing header groups
- Allow copying header values to clipboard
- Handle large responses efficiently

Please provide the FXML definition and controller code with appropriate styling.
```

### Feature Implementation

```
I'm implementing environment variables support for Swaggerific (a JavaFX Swagger client). I need:
1. A data model for storing environment variables
2. A UI for managing environments and variables
3. Integration with the existing request system to substitute variables

The solution should follow our SOLID architecture with clean separation between UI and service layers.
```

### Bug Fixing

```
I'm encountering an issue in Swaggerific where the syntax highlighting in the JSON editor stops working after switching tabs. Here's the relevant code:

[code snippet]

How can I fix this while ensuring it works across all platforms (Windows, macOS, Linux) with JDK 21?
```

### UI/UX Improvements

```
I want to improve the visual feedback when API requests are loading in Swaggerific. Currently, we only show a small spinner in the status bar.

Please suggest a more visually appealing loading indicator that:
- Doesn't block the UI
- Clearly shows that a request is in progress
- Fits with our AtlantaFX PrimerLight theme
- Works well with our JavaFX application
```

## Conclusion

By following these guidelines, AI assistance can be more effectively leveraged to develop and enhance Swaggerific. These rules ensure that AI-generated solutions align with the project's technical requirements, architectural patterns, and UI/UX standards.

Remember that Swaggerific aims to be the best open-source UI for HTTP utilities, with a focus on visual appeal, user experience, and seamless Swagger/OpenAPI integration. All AI-assisted development should contribute to this vision.