#!/bin/bash

# Swaggerific Runner Script
# This script runs the Swaggerific application without requiring Maven

# Exit on error
set -e

# Script location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or later"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d'.' -f1)

if [[ "$JAVA_MAJOR_VERSION" -lt 17 ]]; then
    echo "Error: Java 17 or later is required (found version $JAVA_VERSION)"
    echo "Please install a newer version of Java"
    exit 1
fi

VERSION=$(grep -m 1 "<version>" pom.xml | sed -e 's/<version>\(.*\)<\/version>/\1/' -e 's/[[:space:]]//g')
APP_NAME="swaggerific"
PACKAGE_NAME="${APP_NAME}-${VERSION}"

# Check if the JAR file exists
JAR_FILE="$SCRIPT_DIR/target/${PACKAGE_NAME}.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found: $JAR_FILE"

    # Check if we can build it
    if [ -f "$SCRIPT_DIR/pom.xml" ]; then
        echo "Attempting to build the project..."

        # Check if Maven is installed
        if command -v mvn &> /dev/null; then
            echo "Building with Maven..."
            mvn clean package -DskipTests

            if [ ! -f "$JAR_FILE" ]; then
                echo "Build failed: JAR file still not found"
                exit 1
            fi

            echo "Build successful!"
        else
            echo "Maven not found. Cannot build the project."
            echo "Please install Maven or download a pre-built JAR file."
            exit 1
        fi
    else
        echo "pom.xml not found. Cannot build the project."
        echo "Please download a pre-built JAR file."
        exit 1
    fi
fi

# Find JavaFX modules in the local Maven repository
JAVAFX_VERSION=$(grep -m 1 "<javafx.version>" "$SCRIPT_DIR/pom.xml" | sed -e 's/<javafx.version>\(.*\)<\/javafx.version>/\1/' -e 's/[[:space:]]//g')
echo "Using JavaFX version: $JAVAFX_VERSION"

# Determine OS-specific JavaFX modules
OS_NAME=$(uname -s)
ARCH=$(uname -m)

case "$OS_NAME" in
    Linux*)     JAVAFX_OS="linux" ;;
    Darwin*)    
        if [ "$ARCH" = "arm64" ]; then
            JAVAFX_OS="mac-aarch64"
        else
            JAVAFX_OS="mac"
        fi
        ;;
    MINGW*|MSYS*|CYGWIN*) JAVAFX_OS="win" ;;
    *)          JAVAFX_OS="linux" ;;
esac
echo "Detected OS: $JAVAFX_OS (Architecture: $ARCH)"

# Find Maven repository location
if [ -d "$HOME/.m2/repository" ]; then
    M2_REPO="$HOME/.m2/repository"
elif [ -d "/root/.m2/repository" ]; then
    M2_REPO="/root/.m2/repository"
else
    echo "Maven repository not found. Please ensure JavaFX dependencies are available."
    M2_REPO="$HOME/.m2/repository"  # Default location
fi

# Build module path for JavaFX
JAVAFX_MODULES=(
    "javafx-base"
    "javafx-controls"
    "javafx-fxml"
    "javafx-graphics"
    "javafx-media"
    "javafx-web"
    "javafx-swing"
)

MODULE_PATH=""
for module in "${JAVAFX_MODULES[@]}"; do
    module_jar="$M2_REPO/org/openjfx/$module/$JAVAFX_VERSION/$module-$JAVAFX_VERSION-$JAVAFX_OS.jar"
    if [ -f "$module_jar" ]; then
        if [ -z "$MODULE_PATH" ]; then
            MODULE_PATH="$module_jar"
        else
            MODULE_PATH="$MODULE_PATH:$module_jar"
        fi
    else
        echo "Warning: JavaFX module not found: $module_jar"
    fi
done

# Run the application
echo "Starting Swaggerific..."
java --module-path "$MODULE_PATH" \
     --add-modules=javafx.controls,javafx.web,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.swing \
     --add-exports=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED \
     --enable-native-access=javafx.graphics \
     --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED \
     --add-opens=java.base/java.nio=ALL-UNNAMED \
     -jar "$JAR_FILE"

exit 0
