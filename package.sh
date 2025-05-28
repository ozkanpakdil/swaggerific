#!/bin/bash

# Swaggerific Packaging Script
# This script packages the Swaggerific application for distribution

# Exit on error
set -e

# Script location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Version from pom.xml
VERSION=$(grep -m 1 "<version>" pom.xml | sed -e 's/<version>\(.*\)<\/version>/\1/' -e 's/[[:space:]]//g')
# JavaFX version from pom.xml
JAVAFX_VERSION=$(grep -m 1 "<javafx.version>" pom.xml | sed -e 's/<javafx.version>\(.*\)<\/javafx.version>/\1/' -e 's/[[:space:]]//g')
APP_NAME="swaggerific"
PACKAGE_NAME="${APP_NAME}-${VERSION}"
DIST_DIR="$SCRIPT_DIR/dist"
PACKAGE_DIR="$DIST_DIR/$PACKAGE_NAME"

echo "Packaging $APP_NAME version $VERSION"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Build the project
echo "Building the project..."
mvn clean package -DskipTests

# Create distribution directory
echo "Creating distribution package..."
mkdir -p "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/lib"

# Copy files to package directory
cp "$SCRIPT_DIR/target/${APP_NAME}-${VERSION}.jar" "$PACKAGE_DIR/"

# Copy JavaFX modules to lib directory
echo "Copying JavaFX modules (version $JAVAFX_VERSION)..."

# Determine OS-specific JavaFX modules for each platform
PLATFORMS=("linux" "mac" "mac-aarch64" "win")
JAVAFX_MODULES=(
    "javafx-base"
    "javafx-controls"
    "javafx-fxml"
    "javafx-graphics"
    "javafx-media"
    "javafx-web"
    "javafx-swing"
)

# Find Maven repository location
if [ -d "$HOME/.m2/repository" ]; then
    M2_REPO="$HOME/.m2/repository"
elif [ -d "/root/.m2/repository" ]; then
    M2_REPO="/root/.m2/repository"
else
    echo "Maven repository not found. Cannot package JavaFX modules."
    exit 1
fi

# Copy JavaFX modules for all platforms
for PLATFORM in "${PLATFORMS[@]}"; do
    mkdir -p "$PACKAGE_DIR/lib/$PLATFORM"
    for MODULE in "${JAVAFX_MODULES[@]}"; do
        MODULE_JAR="$M2_REPO/org/openjfx/$MODULE/$JAVAFX_VERSION/$MODULE-$JAVAFX_VERSION-$PLATFORM.jar"
        if [ -f "$MODULE_JAR" ]; then
            cp "$MODULE_JAR" "$PACKAGE_DIR/lib/$PLATFORM/"
            echo "  Copied $MODULE for $PLATFORM"
        else
            echo "  Warning: JavaFX module not found: $MODULE_JAR"
            # Try to download the missing module
            echo "  Attempting to download missing module..."
            mvn dependency:get -Dartifact=org.openjfx:$MODULE:$JAVAFX_VERSION:jar:$PLATFORM -DremoteRepositories=https://repo1.maven.org/maven2 -quiet
            if [ -f "$MODULE_JAR" ]; then
                cp "$MODULE_JAR" "$PACKAGE_DIR/lib/$PLATFORM/"
                echo "  Successfully downloaded and copied $MODULE for $PLATFORM"
            else
                echo "  Failed to download $MODULE for $PLATFORM"
            fi
        fi
    done
done
# Create a modified run.sh for the package
cat > "$PACKAGE_DIR/run.sh" << EOF
#!/bin/bash

# Swaggerific Runner Script
# This script runs the Swaggerific application without requiring Maven

# Exit on error
set -e

# Script location
SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
cd "\$SCRIPT_DIR"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or later"
    exit 1
fi

# Check Java version
JAVA_VERSION=\$(java -version 2>&1 | awk -F '"' '/version/ {print \$2}')
JAVA_MAJOR_VERSION=\$(echo "\$JAVA_VERSION" | cut -d'.' -f1)

if [[ "\$JAVA_MAJOR_VERSION" -lt 17 ]]; then
    echo "Error: Java 17 or later is required (found version \$JAVA_VERSION)"
    echo "Please install a newer version of Java"
    exit 1
fi

# Use JavaFX modules from the lib directory
JAVAFX_VERSION="${JAVAFX_VERSION}"
echo "Using JavaFX version: \$JAVAFX_VERSION"

# Determine OS-specific JavaFX modules
OS_NAME=\$(uname -s)
ARCH=\$(uname -m)

case "\$OS_NAME" in
    Linux*)     JAVAFX_OS="linux" ;;
    Darwin*)    
        if [ "\$ARCH" = "arm64" ]; then
            JAVAFX_OS="mac-aarch64"
        else
            JAVAFX_OS="mac"
        fi
        ;;
    MINGW*|MSYS*|CYGWIN*) JAVAFX_OS="win" ;;
    *)          JAVAFX_OS="linux" ;;
esac
echo "Detected OS: \$JAVAFX_OS (Architecture: \$ARCH)"

# Build module path for JavaFX using local lib directory
JAVAFX_MODULES=(
    "javafx-base"
    "javafx-controls"
    "javafx-fxml"
    "javafx-graphics"
    "javafx-media"
    "javafx-web"
    "javafx-swing"
)

# Check if we have the modules for this OS
if [ ! -d "\$SCRIPT_DIR/lib/\$JAVAFX_OS" ]; then
    echo "Error: JavaFX modules for \$JAVAFX_OS not found in \$SCRIPT_DIR/lib/\$JAVAFX_OS"
    echo "Please run the application on the same OS type used for packaging or download the appropriate package."
    exit 1
fi

MODULE_PATH=""
for module in "\${JAVAFX_MODULES[@]}"; do
    module_jar="\$SCRIPT_DIR/lib/\$JAVAFX_OS/\$module-\$JAVAFX_VERSION-\$JAVAFX_OS.jar"
    if [ -f "\$module_jar" ]; then
        if [ -z "\$MODULE_PATH" ]; then
            MODULE_PATH="\$module_jar"
        else
            MODULE_PATH="\$MODULE_PATH:\$module_jar"
        fi
    else
        echo "Warning: JavaFX module not found: \$module_jar"
    fi
done

# If no modules were found, exit with error
if [ -z "\$MODULE_PATH" ]; then
    echo "Error: No JavaFX modules found in \$SCRIPT_DIR/lib/\$JAVAFX_OS"
    echo "Please ensure the package includes the required JavaFX modules."
    exit 1
fi

# Run the application
echo "Starting Swaggerific..."
java --module-path "\$MODULE_PATH" \\
     --add-modules=javafx.controls,javafx.web,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.swing \\
     --add-exports=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \\
     --add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \\
     --add-exports=javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED \\
     --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \\
     --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED \\
     --add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED \\
     -jar "${APP_NAME}-${VERSION}.jar"

exit 0
EOF

# Copy additional files
cp "$SCRIPT_DIR/README.md" "$PACKAGE_DIR/" 2>/dev/null || echo "README.md not found, skipping"
cp -r "$SCRIPT_DIR/src/main/resources" "$PACKAGE_DIR/resources" 2>/dev/null || echo "Resources directory not found, skipping"

# Make scripts executable
chmod +x "$PACKAGE_DIR/run.sh"

# Create README if it doesn't exist
if [ ! -f "$PACKAGE_DIR/README.md" ]; then
    cat > "$PACKAGE_DIR/README.md" << EOF
# Swaggerific

An alternative to famous REST client Postman, Swaggerific is written in Java and fully open source.

## Requirements

- Java 17 or later

## Running the Application

1. Extract the zip file
2. Run the application using the provided script:
   - On Linux/macOS: \`./run.sh\`
   - On Windows: Double-click \`run.bat\` or run it from Command Prompt

## Included Components

This package includes:
- Swaggerific application (${VERSION})
- JavaFX modules (${JAVAFX_VERSION}) for Linux, macOS, and Windows
- Run scripts for all platforms

The application will automatically use the appropriate JavaFX modules for your operating system.

## More Information

Visit the project repository: https://github.com/ozkanpakdil/swaggerific
EOF
fi

# Create Windows batch file
cat > "$PACKAGE_DIR/run.bat" << EOF
@echo off
echo Starting Swaggerific...

REM Set JavaFX version
set JAVAFX_VERSION=${JAVAFX_VERSION}
echo Using JavaFX version: %JAVAFX_VERSION%

REM Set OS-specific suffix
set JAVAFX_OS=win

REM Build module path for JavaFX using local lib directory
set MODULE_PATH=
set MODULES_TO_ADD=javafx.controls,javafx.web,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.swing

REM Check if we have the modules for this OS
if not exist "lib\%JAVAFX_OS%" (
    echo Error: JavaFX modules for %JAVAFX_OS% not found in lib\%JAVAFX_OS%
    echo Please run the application on Windows or download the appropriate package.
    pause
    exit /b 1
)

REM Add JavaFX modules to module path
call :AddModule "javafx-base"
call :AddModule "javafx-controls"
call :AddModule "javafx-fxml"
call :AddModule "javafx-graphics"
call :AddModule "javafx-media"
call :AddModule "javafx-web"
call :AddModule "javafx-swing"

REM Check if any modules were found
if "%MODULE_PATH%"=="" (
    echo Error: No JavaFX modules found in lib\%JAVAFX_OS%
    echo Please ensure the package includes the required JavaFX modules.
    pause
    exit /b 1
)

REM Run the application
echo Starting Swaggerific...
java --module-path "%MODULE_PATH%" ^
     --add-modules=%MODULES_TO_ADD% ^
     --add-exports=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
     --add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED ^
     --add-exports=javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED ^
     --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED ^
     --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
     --add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED ^
     -jar "${APP_NAME}-${VERSION}.jar"
pause
exit /b

:AddModule
set MODULE=%~1
set MODULE_JAR=lib\%JAVAFX_OS%\%MODULE%-%JAVAFX_VERSION%-%JAVAFX_OS%.jar
if exist "%MODULE_JAR%" (
    if "%MODULE_PATH%"=="" (
        set MODULE_PATH=%MODULE_JAR%
    ) else (
        set MODULE_PATH=%MODULE_PATH%;%MODULE_JAR%
    )
) else (
    echo Warning: JavaFX module not found: %MODULE_JAR%
)
exit /b
EOF

# Create zip file
echo "Creating zip archive..."
cd "$DIST_DIR"
zip -r "${PACKAGE_NAME}.zip" "$PACKAGE_NAME"

echo "Package created: $DIST_DIR/${PACKAGE_NAME}.zip"
echo "Users can download, unzip, and run the application using the provided scripts."
