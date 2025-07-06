#!/bin/bash

# Swaggerific Packaging Script
# This script packages the Swaggerific application for distribution
# All packaging logic is now handled by Maven

# Exit on error
set -e

# Script location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Version from pom.xml
VERSION=$(grep -m 1 "<version>" pom.xml | sed -e 's/<version>\(.*\)<\/version>/\1/' -e 's/[[:space:]]//g')
APP_NAME="swaggerific"

echo "Packaging $APP_NAME version $VERSION"

# Build and package the project using Maven
echo "Building and packaging the project using Maven..."
./mvnw clean package -DskipTests

# Copy the distribution package to the main dist directory for compatibility
echo "Copying distribution package..."
mkdir -p "$SCRIPT_DIR/dist"
if [ -f "$SCRIPT_DIR/target/dist/${APP_NAME}-${VERSION}.zip" ]; then
    cp "$SCRIPT_DIR/target/dist/${APP_NAME}-${VERSION}.zip" "$SCRIPT_DIR/dist/"
    echo "Package created: $SCRIPT_DIR/dist/${APP_NAME}-${VERSION}.zip"
    echo "Users can download, unzip, and run the application using the provided scripts."
else
    echo "Error: Distribution package not found at target/dist/${APP_NAME}-${VERSION}.zip"
    echo "Please check the Maven build output for errors."
    exit 1
fi
