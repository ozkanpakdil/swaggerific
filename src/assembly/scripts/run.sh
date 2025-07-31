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

# Check if running with GraalVM
IS_GRAALVM=false
JAVA_VM_NAME=$(java -XshowSettings:properties -version 2>&1 | grep "java.vm.name" | awk -F '=' '{print $2}' | tr -d ' ')
if [[ "$JAVA_VM_NAME" == *"GraalVM"* ]]; then
    IS_GRAALVM=true
    echo "Detected GraalVM: $JAVA_VM_NAME"
else
    echo "Running with standard JDK: $JAVA_VM_NAME"
    echo "Note: Some JavaScript features may have limited functionality without GraalVM."
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

# Find all GraalVM JARs in the lib directory
for jar in "$SCRIPT_DIR"/lib/graal*.jar "$SCRIPT_DIR"/lib/js*.jar "$SCRIPT_DIR"/lib/truffle*.jar; do
    if [ -f "$jar" ]; then
        if [ -z "$MODULE_PATH" ]; then
            MODULE_PATH="$jar"
        else
            MODULE_PATH="$MODULE_PATH:$jar"
        fi
        echo "Added GraalVM module: $jar"
    fi
done

# Check if we found any GraalVM modules
FOUND_GRAALVM_MODULES=false
if echo "$MODULE_PATH" | grep -q "graal\|js\|truffle"; then
    FOUND_GRAALVM_MODULES=true
else
    if [ "$IS_GRAALVM" = true ]; then
        echo "Warning: Running with GraalVM JDK but no GraalVM modules found in $SCRIPT_DIR/lib"
        echo "The application will use the GraalVM capabilities from your JDK."
    else
        echo "Warning: No GraalVM modules found in $SCRIPT_DIR/lib"
        echo "JavaScript evaluation and scripting features will have limited functionality."
        echo "For full JavaScript support, either:"
        echo "  1. Use GraalVM as your JDK (https://www.graalvm.org/downloads/)"
        echo "  2. Ensure the distribution package includes the required GraalVM modules"
    fi
fi

# If no modules were found, exit with error
if [ -z "$MODULE_PATH" ]; then
    echo "Error: No JavaFX modules found in $SCRIPT_DIR/lib/$JAVAFX_OS"
    echo "Please ensure the package includes the required JavaFX modules."
    exit 1
fi

# Run the application
echo "Starting Swaggerific..."

# Prepare Java command with common options
JAVA_CMD="java --module-path \"$MODULE_PATH\" \
     --add-modules=javafx.controls,javafx.web,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.swing \
     --add-exports=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED \
     --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED \
     --add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED \
     --enable-native-access=ALL-UNNAMED \
     --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     -Dpolyglot.engine.WarnInterpreterOnly=false"

# Add GraalVM module options based on available modules and JDK type
if [ "$FOUND_GRAALVM_MODULES" = true ]; then
    # GraalVM modules found in lib directory - use them
    echo "Using GraalVM modules from distribution package"
    JAVA_CMD="$JAVA_CMD --add-modules=org.graalvm.sdk"
    
    # Add exports if truffle modules are available
    if echo "$MODULE_PATH" | grep -q "truffle"; then
        JAVA_CMD="$JAVA_CMD --add-exports=org.graalvm.truffle/com.oracle.truffle.api=ALL-UNNAMED"
    fi
elif [ "$IS_GRAALVM" = true ]; then
    # Running with GraalVM JDK but no modules in lib - use JDK's built-in modules
    echo "Using GraalVM modules from JDK"
    JAVA_CMD="$JAVA_CMD --add-modules=org.graalvm.sdk"
    JAVA_CMD="$JAVA_CMD --add-exports=org.graalvm.truffle/com.oracle.truffle.api=ALL-UNNAMED"
else
    # Standard JDK with no GraalVM modules - run with limited JavaScript functionality
    echo "Running with limited JavaScript functionality"
    # No GraalVM-specific options needed
fi

# Execute the Java command
eval "$JAVA_CMD -jar \"swaggerific-${project.version}.jar\""

exit 0
