# Swaggerific Development Guidelines

This document provides essential information for developers working on the Swaggerific project.

## Build/Configuration Instructions

### Prerequisites
- Java Development Kit (JDK) 21 or later
- Maven 3.6 or later

### Building the Project
1. Clone the repository
2. Build the project using Maven:
   ```
   mvn clean package
   ```
3. Run the application:
   ```
   mvn javafx:run
   ```

### Native Image Building
The project uses GluonFX for native image building:

1. Set the target platform (Windows is default, for Linux use the `linux` profile):
   ```
   mvn -Plinux gluonfx:build
   ```
2. Build the native image:
   ```
   mvn gluonfx:build
   ```
3. Run the native image:
   ```
   mvn gluonfx:run
   ```

## Testing Information

### Test Framework
- JUnit 5 is used as the primary testing framework
- TestFX is used for JavaFX UI testing
- MockServer is used for mocking HTTP responses

### Running Tests
1. Run all tests:
   ```
   mvn test
   ```
2. Run a specific test class:
   ```
   mvn test -Dtest=ClassName
   ```
3. Run a specific test method:
   ```
   mvn test -Dtest=ClassName#methodName
   ```

### Test Categories
1. **Unit Tests**: Regular JUnit tests for testing individual components
2. **UI Tests**: TestFX tests for testing JavaFX UI components
   - These tests use `@ExtendWith(ApplicationExtension.class)` and `@Start` methods
   - Most UI tests are platform-specific and use `@EnabledOnOs({ OS.WINDOWS })`

### Adding New Tests
1. Create a new test class in the appropriate package under `src/test/java`
2. For UI tests:
   - Extend with `ApplicationExtension`
   - Add `@Start` method to initialize JavaFX components
   - Use `FxRobot` for simulating user interactions
   - Use `FxAssert` for assertions on UI components
3. For unit tests:
   - Use standard JUnit 5 annotations and assertions
   - Mock external dependencies as needed

### Example Test
Here's a simple example of a unit test for string utilities:

```java
@ExtendWith(ApplicationExtension.class)
@EnabledOnOs({ OS.WINDOWS })
class StringUtilsTest {
    @Test
    void testIsNullOrEmpty() {
        assertTrue(StringUtils.isNullOrEmpty(null));
        assertTrue(StringUtils.isNullOrEmpty(""));
        assertFalse(StringUtils.isNullOrEmpty(" "));
        assertFalse(StringUtils.isNullOrEmpty("test"));
    }
}
```

## Additional Development Information

### Project Structure
- JavaFX application using FXML for UI definition
- Uses Maven for build management
- Follows standard Maven directory structure

### Code Style
- Java code follows standard Java conventions
- Use JavaDoc comments for public classes and methods
- UI components are defined in FXML files with corresponding controller classes

### Debugging
- For UI tests, you can use the `getScreenShotOfTheTest` method to capture screenshots
- Add debug logging with appropriate log levels

### Working with Swagger/OpenAPI
- The application parses Swagger/OpenAPI JSON files
- Test resources include sample Swagger files in `src/test/resources`
- Mock HTTP responses can be configured using MockServer

### Dependencies
- JavaFX for UI
- Jackson for JSON processing
- JUnit and TestFX for testing
- Various UI component libraries (BootstrapFX, AtlantaFX, etc.)