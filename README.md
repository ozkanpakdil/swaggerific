# swaggerific

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ozkanpakdil_swaggerific&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ozkanpakdil_swaggerific) ![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/ozkanpakdil/swaggerific?labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit%20Reviews)

Overall Downloads on github: ![All downloads of github releases](https://img.shields.io/github/downloads/ozkanpakdil/swaggerific/total)

You can download the latest version from [Github releases ![Download count of latest releases](https://img.shields.io/github/downloads/ozkanpakdil/swaggerific/latest/total.svg)](https://github.com/ozkanpakdil/swaggerific/releases) Follow [here](https://bsky.app/profile/swaggerific.bsky.social) for new releases.

A user interface (UI) designed to interact with APIs using Swagger or OpenAPI jsons.

# Cool interview  
[![swaggerefic](http://img.youtube.com/vi/3_T0LDZ-Wt4/0.jpg)](http://www.youtube.com/watch?v=3_T0LDZ-Wt4 "How to use swaggerific and interview with ozkan pakdil")

## Features:
- The UI is built using JavaFX with modern styling and responsive design
- Comprehensive error handling with user-friendly error messages
- Retrieve and load Swagger/OpenAPI JSON to fetch API documentation
- Display a tree view of endpoints on the left-hand side for easy navigation
- Select specific endpoints and view their details
- Support for all HTTP methods (GET, POST, PUT, DELETE, etc.)
- Input data via form fields or JSON editor with syntax highlighting
- Execute API requests and view formatted responses (JSON/XML)
- Dropdown/combobox selection for parameters with enumerated values
- Auto-completion for brackets and quotes in the JSON editor
- Syntax highlighting for request and response bodies
- Raw response view for debugging purposes

## Architecture:
- SOLID principles applied throughout the codebase
- Clean separation between UI and HTTP service layers
- Modular design with well-defined interfaces
- Comprehensive error handling and logging
- Unit and UI tests for critical functionality

## Upcoming Features:
- Authentication support (OAuth, API keys, Basic Auth)
- Request history and favorites
- Environment variables and request chaining
- Team collaboration features
- Performance metrics and analytics

# look
![using-get](https://github.com/ozkanpakdil/swaggerific/assets/604405/748eb2a8-3578-45e3-ac95-e8246ef27785)

# Running Without Maven

## Using the Run Script
We've added a convenient bash script to run the application without requiring Maven:

```shell
# Make the script executable (first time only)
chmod +x run.sh

# Run the application
./run.sh
```

The script will:
1. Check if Java 17+ is installed
2. Look for the JAR file in the target directory
3. Build the project if the JAR doesn't exist (requires Maven)
4. Run the application with all necessary JVM arguments

## Distribution Package
For easy distribution to users, you can create a zip package:

```shell
# Make the script executable (first time only)
chmod +x package.sh

# Create the distribution package
./package.sh
```

This will create a zip file in the `dist` directory containing:
- The application JAR
- Run scripts for Linux/macOS (run.sh) and Windows (run.bat)
- README with instructions
- Required resources

Users can simply download the zip, extract it, and run the application using the provided scripts.

# Native Build
```shell
mvn gluonfx:build gluonfx:nativerun
```
# Update Reflection Classes
There are classes needs to be in the list of reflection list of the compiler(which was in the pom.xml before check 'git history'), that list can be generated from command below, that will update the jsons under META-INF/native-image  
```shell
./mvnw gluonfx:runagent
```

https://petstore.swagger.io/#/pet

 [Open source Support](https://jb.gg/OpenSourceSupport) by [JetBrains](https://www.jetbrains.com)
