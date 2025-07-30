# Contributing to Swaggerific

Thank you for your interest in contributing to Swaggerific! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
  - [Development Environment Setup](#development-environment-setup)
  - [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
  - [Branching Strategy](#branching-strategy)
  - [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Issue Reporting](#issue-reporting)
- [Feature Requests](#feature-requests)
- [License](#license)

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for everyone. Please:

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

Unacceptable behavior will not be tolerated.

## Getting Started

### Development Environment Setup

1. **Prerequisites**:
   - Java Development Kit (JDK) 21 or higher
   - Maven 3.6+
   - Git

2. **Fork and Clone**:
   ```bash
   git clone https://github.com/YOUR-USERNAME/swaggerific.git
   cd swaggerific
   ```

3. **Set up dependencies**:
   ```bash
   mvn clean install
   ```

### Project Structure

Swaggerific follows a standard Maven project structure:

- `src/main/java`: Java source code
- `src/main/resources`: Resources (CSS, FXML, images)
- `src/test/java`: Test source code
- `src/test/resources`: Test resources

Key packages include:
- `io.github.ozkanpakdil.swaggerific.ui`: UI components
- `io.github.ozkanpakdil.swaggerific.data`: Data models and services
- `io.github.ozkanpakdil.swaggerific.tools`: Utility classes

## Development Workflow

### Branching Strategy

1. Create a branch from `main` for your work:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

2. Make your changes, following the coding standards.

3. Keep your branch updated with the main branch:
   ```bash
   git fetch origin
   git rebase origin/main
   ```

### Commit Guidelines

- Use clear, descriptive commit messages
- Reference issue numbers when applicable
- Keep commits focused on a single logical change
- Format: `[type]: Short description (fixes #1234)`
  - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Example:
```
feat: Add environment variables support (fixes #42)
```

## Pull Request Process

1. Ensure your code follows the project's coding standards
2. Update documentation if necessary
3. Include tests for new features or bug fixes
4. Ensure all tests pass locally
5. Create a pull request to the `main` branch
6. Fill in the PR template with all required information
7. Request a review from maintainers
8. Address any feedback from code reviews

## Coding Standards

Swaggerific follows standard Java coding conventions:

1. **Architecture**:
   - Follow SOLID principles
   - Maintain clean separation between UI and service layers
   - Use modular design with well-defined interfaces
   - Follow the MVC pattern with FXML for views and controllers for logic

2. **JavaFX Patterns**:
   - Use FXML for UI definition where appropriate
   - Follow the controller pattern for UI logic
   - Use property binding for reactive UI updates
   - Leverage JavaFX CSS for styling

3. **Code Style**:
   - Use meaningful variable and method names
   - Include JavaDoc comments for public APIs
   - Write clear and concise code with appropriate logging
   - Follow Java naming conventions:
     - Classes: `PascalCase`
     - Methods/Variables: `camelCase`
     - Constants: `UPPER_SNAKE_CASE`

4. **Cross-Platform Compatibility**:
   - Ensure code works on Windows, macOS, and Linux
   - Use platform-independent APIs when possible
   - Handle platform-specific issues with appropriate abstractions

## Testing

- Write unit tests for all new features and bug fixes
- Ensure existing tests pass before submitting a PR
- Use JUnit 5 for testing
- Follow test naming convention: `methodName_testCase_expectedResult`
- Mock external dependencies when appropriate

To run tests:
```bash
mvn test
```

## Issue Reporting

When reporting issues, please use the issue templates provided and include:

1. A clear and descriptive title
2. Steps to reproduce the issue
3. Expected behavior
4. Actual behavior
5. Screenshots if applicable
6. Your environment details:
   - OS
   - Java version
   - Swaggerific version

## Feature Requests

Feature requests are welcome! Please use the feature request template and provide:

1. A clear description of the feature
2. The problem it solves
3. How it aligns with the project's goals
4. Any implementation ideas you have

## License

By contributing to Swaggerific, you agree that your contributions will be licensed under the project's [GNU General Public License v3.0](LICENSE).

---

Thank you for contributing to Swaggerific! Your efforts help make this project better for everyone.