#!/usr/bin/env bash

set -euo pipefail

# Package builder for macOS Apple Silicon (arm64 / aarch64)
# This script packages the already-built Gluon native image into:
# - a tar.gz archive
# - a .pkg and a product .pkg installer
#
# Requirements:
# - The native image should already be built at target/gluonfx/aarch64-darwin/swaggerific.app
# - pkgbuild and productbuild must be available (macOS only)
#
# Optional: After packaging, this script uploads the tar.gz to the GitHub
# release tagged "latest_macos" using GitHub CLI (`gh`). Keep it simple.
# Requirements for upload: `gh` installed and authenticated (`gh auth login`).

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET_DIR="$ROOT_DIR/target/gluonfx/aarch64-darwin"
APP_NAME="swaggerific.app"
APP_PATH="$TARGET_DIR/$APP_NAME"
STAGING_DIR="$ROOT_DIR/staging"
IDENTIFIER="io.github.ozkanpakdil.swaggerific"
APP_ICON_PNG="$ROOT_DIR/src/main/resources/applogo.png"

# --- Simple, fixed upload target ---
GITHUB_REPO="ozkanpakdil/swaggerific"
RELEASE_TAG="latest_macos"

# Obtain project version from Maven (falls back to 0.0.0 if unavailable)
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null || echo 0.0.0)"
if [[ -z "$VERSION" || "$VERSION" == *"["* ]]; then
  VERSION="0.0.0"
fi

echo "Using version: $VERSION"

if [[ ! -d "$TARGET_DIR" ]]; then
  echo "Error: Expected target directory not found: $TARGET_DIR" >&2
  echo "Please build the native image first, e.g.: ./mvnw -ntp -Pdesktop gluonfx:build gluonfx:package" >&2
  exit 1
fi

if [[ ! -d "$APP_PATH" ]]; then
  echo "Error: Expected app bundle not found: $APP_PATH" >&2
  echo "Please build the native image first, e.g.: ./mvnw -ntp -Pdesktop gluonfx:build gluonfx:package" >&2
  exit 1
fi

mkdir -p "$STAGING_DIR"

# Ensure the .app is executable (main binary inside .app will already be executable, but keep parity with CI)
chmod -R +x "$APP_PATH" || true

echo "Copying $APP_NAME to staging..."
rsync -a "$APP_PATH" "$STAGING_DIR/"

# Prepare icon: convert applogo.png -> .icns and patch Info.plist in the staged app
STAGED_APP="$STAGING_DIR/$APP_NAME"
ICNS_NAME="AppIcon.icns"
TMP_ICONSET_DIR="$(mktemp -d)"/AppIcon.iconset

if [[ -f "$APP_ICON_PNG" ]]; then
  echo "Generating macOS .icns from $APP_ICON_PNG ..."
  mkdir -p "$TMP_ICONSET_DIR"
  # Generate required icon sizes
  for size in 16 32 64 128 256 512; do
    sips -z $size $size     "$APP_ICON_PNG" --out "$TMP_ICONSET_DIR/icon_${size}x${size}.png" >/dev/null
    dbl=$((size*2))
    sips -z $dbl  $dbl      "$APP_ICON_PNG" --out "$TMP_ICONSET_DIR/icon_${size}x${size}@2x.png" >/dev/null
  done
  # 1024x1024 (optional but good for retina)
  sips -z 1024 1024 "$APP_ICON_PNG" --out "$TMP_ICONSET_DIR/icon_512x512@2x.png" >/dev/null || true
  iconutil -c icns "$TMP_ICONSET_DIR" -o "$STAGED_APP/Contents/Resources/$ICNS_NAME"
  rm -rf "$(dirname "$TMP_ICONSET_DIR")" || true

  PLIST="$STAGED_APP/Contents/Info.plist"
  if [[ -f "$PLIST" ]]; then
    echo "Patching CFBundleIconFile in Info.plist ..."
    /usr/libexec/PlistBuddy -c "Set :CFBundleIconFile AppIcon" "$PLIST" 2>/dev/null || \
    /usr/libexec/PlistBuddy -c "Add :CFBundleIconFile string AppIcon" "$PLIST" 2>/dev/null || true
  fi
else
  echo "Warning: App icon PNG not found at $APP_ICON_PNG. Using existing icon if any." >&2
fi

ARCHIVE_NAME="swaggerific_aarch64-darwin.tar.gz"
echo "Creating archive $ARCHIVE_NAME ..."
tar -czvf "$STAGING_DIR/$ARCHIVE_NAME" -C "$TARGET_DIR" "swaggerific.app"

PKG_NAME="Swaggerific.pkg"
INSTALLER_NAME="SwaggerificInstaller.pkg"

echo "Building pkg ($PKG_NAME) ..."
pkgbuild \
  --root "$TARGET_DIR/" \
  --identifier "$IDENTIFIER" \
  --version "$VERSION" \
  --install-location "/Applications" \
  "$PKG_NAME"

echo "Building product installer ($INSTALLER_NAME) ..."
productbuild --package "$PKG_NAME" "$INSTALLER_NAME"

echo "Moving installer(s) to staging ..."
mv -f "$PKG_NAME" "$STAGING_DIR/"
mv -f "$INSTALLER_NAME" "$STAGING_DIR/"

echo "Done. Artifacts:"
echo " - $STAGING_DIR/$ARCHIVE_NAME"
echo " - $STAGING_DIR/$PKG_NAME"
echo " - $STAGING_DIR/$INSTALLER_NAME"

# ------------------------
# Simple upload to GitHub Release (latest_macos)
# ------------------------
ARCHIVE_PATH="$STAGING_DIR/$ARCHIVE_NAME"
if command -v gh >/dev/null 2>&1; then
  echo "Uploading $ARCHIVE_PATH to https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG ..."
  # Ensure release exists (no frills); if it doesn't, print a hint and skip
  if gh release view "$RELEASE_TAG" --repo "$GITHUB_REPO" >/dev/null 2>&1; then
    gh release upload "$RELEASE_TAG" "$ARCHIVE_PATH" --repo "$GITHUB_REPO" --clobber
    echo "Upload complete."
  else
    echo "Release tag '$RELEASE_TAG' does not exist on $GITHUB_REPO. Create it first, then re-run."
    echo "Hint: gh release create $RELEASE_TAG -t \"latest macOS\" -n \"Automated macOS build\" --repo $GITHUB_REPO"
  fi
else
  echo "GitHub CLI (gh) not found. To upload manually run:"
  echo "  gh auth login"
  echo "  gh release upload $RELEASE_TAG $ARCHIVE_PATH --repo $GITHUB_REPO --clobber"
fi
