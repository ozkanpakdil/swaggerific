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

# Use JavaFX modules from the lib directory
JAVAFX_VERSION="${javafx.version}"
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
if [ ! -d "$SCRIPT_DIR/lib/$JAVAFX_OS" ]; then
    echo "Error: JavaFX modules for $JAVAFX_OS not found in $SCRIPT_DIR/lib/$JAVAFX_OS"
    echo "Please run the application on the same OS type used for packaging or download the appropriate package."
    exit 1
fi

MODULE_PATH=""
for module in "${JAVAFX_MODULES[@]}"; do
    module_jar="$SCRIPT_DIR/lib/$JAVAFX_OS/$module-$JAVAFX_VERSION-$JAVAFX_OS.jar"
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

# Add GraalVM modules to module path
GRAALVM_VERSION="${graalvm.version}"
GRAALVM_MODULES=(
    "graal-sdk-${GRAALVM_VERSION}.jar"
    "js-${GRAALVM_VERSION}.jar"
    "polyglot-${GRAALVM_VERSION}.jar"
    "js-scriptengine-${GRAALVM_VERSION}.jar"
    "truffle-api-${GRAALVM_VERSION}.jar"
    "truffle-compiler-${GRAALVM_VERSION}.jar"
    "truffle-runtime-${GRAALVM_VERSION}.jar"
    "jniutils-${GRAALVM_VERSION}.jar"
)

for module in "${GRAALVM_MODULES[@]}"; do
    module_jar="$SCRIPT_DIR/lib/$module"
    if [ -f "$module_jar" ]; then
        if [ -z "$MODULE_PATH" ]; then
            MODULE_PATH="$module_jar"
        else
            MODULE_PATH="$MODULE_PATH:$module_jar"
        fi
    else
        echo "Warning: GraalVM module not found: $module_jar"
    fi
done

# If no modules were found, exit with error
if [ -z "$MODULE_PATH" ]; then
    echo "Error: No JavaFX modules found in $SCRIPT_DIR/lib/$JAVAFX_OS"
    echo "Please ensure the package includes the required JavaFX modules."
    exit 1
fi

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
     --add-exports=org.graalvm.truffle.runtime/com.oracle.truffle.runtime=ALL-UNNAMED \
     --enable-native-access=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     -Dpolyglot.engine.WarnInterpreterOnly=false \
     -jar "swaggerific-${project.version}.jar"

exit 0
