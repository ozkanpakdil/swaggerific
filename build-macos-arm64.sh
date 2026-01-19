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

# Load SDKMAN! if it exists
export SDKMAN_DIR="$HOME/.sdkman"
if [[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]]; then
    set +u
    source "$SDKMAN_DIR/bin/sdkman-init.sh"
    set -u
fi

set +u
CURRENT_JAVA=`sdk current java | awk '{ print $NF }' `

sdk default java 22.1.0.1.r17-gln
set -u

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
# Use -DforceStdout to get the version only
VERSION="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null | grep -v '^\[' || echo 0.0.0)"
if [[ -z "$VERSION" || "$VERSION" == *"["* ]]; then
  VERSION="0.0.0"
fi
# Remove any remaining '[' characters that grep might have missed
VERSION=$(echo "$VERSION" | sed 's/\[//g')

# App Store Connect credentials for notarization
# It is recommended to set these in your environment or a shell profile
APPLE_ID="${APPLE_ID:-}"
APPLE_PASSWORD="${APPLE_PASSWORD:-}" # App-specific password
TEAM_ID="TA5X5C9T57"

# OR use API Key (recommended for automation)
# It is recommended to set these in your environment (e.g., ~/.zshrc or ~/.bash_profile)
APPLE_API_KEY_PATH="${APPLE_API_KEY_PATH:-$ROOT_DIR/certificates/AuthKey_6K9DHGLS84.p8}"
APPLE_API_KEY_ID="${APPLE_API_KEY_ID:-}"
APPLE_API_ISSUER_ID="${APPLE_API_ISSUER_ID:-}"

if [[ -z "$APPLE_API_KEY_ID" || -z "$APPLE_API_ISSUER_ID" ]]; then
  echo "Hint: You can set APPLE_API_KEY_ID and APPLE_API_ISSUER_ID env vars to avoid editing this script."
fi

# Code signing identity (must be in Keychain)
SIGNING_IDENTITY="Developer ID Application: Ozkan Pakdil ($TEAM_ID)"
INSTALLER_IDENTITY="Developer ID Installer: Ozkan Pakdil ($TEAM_ID)"
ENTITLEMENTS="$ROOT_DIR/entitlements.plist"

echo "Using version: $VERSION"

if [[ ! -d "$TARGET_DIR" ]]; then
  echo "Error: Expected target directory not found: $TARGET_DIR" >&2
  ./mvnw -ntp -Pdesktop gluonfx:build gluonfx:package
fi

if [[ ! -d "$APP_PATH" ]]; then
  echo "Error: Expected app bundle not found: $APP_PATH" >&2
  ./mvnw -ntp -Pdesktop gluonfx:build gluonfx:package
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

# --- Local Signing ---
echo "=== Signing all binaries in the .app bundle ==="
# Ensure entitlements file exists
if [[ ! -f "$ENTITLEMENTS" ]]; then
  echo "Generating temporary entitlements.plist ..."
  cat > "$ENTITLEMENTS" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key>
    <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <true/>
    <key>com.apple.security.cs.disable-library-validation</key>
    <true/>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key>
    <true/>
    <key>com.apple.security.network.client</key>
    <true/>
    <key>com.apple.security.network.server</key>
    <true/>
</dict>
</plist>
EOF
fi

# 0. Sign any .dylib files first
echo "Signing shared libraries (.dylib)..."
find "$STAGED_APP" -name "*.dylib" -print0 | while read -r -d '' file; do
  echo "Signing dylib: $file"
  chmod +w "$file"
  codesign --force --verify --verbose \
    --sign "$SIGNING_IDENTITY" \
    --options runtime \
    --timestamp \
    --entitlements "$ENTITLEMENTS" \
    "$file"
done

# 1. Find and sign all Mach-O binaries in reverse depth order
echo "Scanning for Mach-O binaries..."
find "$STAGED_APP" -type f -print0 | while read -r -d '' file; do
  if file "$file" | grep -qE "Mach-O|current ar archive"; then
    echo "$file"
  fi
done | sort -r | while read -r file; do
  # Skip if already signed in the dylib step to avoid double signing if possible, though codesign --force handles it
  echo "Signing binary: $file"
  chmod +w "$file"
  codesign --force --verify --verbose \
    --sign "$SIGNING_IDENTITY" \
    --options runtime \
    --timestamp \
    --entitlements "$ENTITLEMENTS" \
    "$file" || echo "Warning: Failed to sign $file, might not be necessary if it is not a loadable binary."
done

# Explicitly sign the main executable again
echo "=== Signing main executable ==="
MAIN_EXE="$STAGED_APP/Contents/MacOS/swaggerific"
if [[ -f "$MAIN_EXE" ]]; then
   chmod +w "$MAIN_EXE"
   codesign --force --verify --verbose \
     --sign "$SIGNING_IDENTITY" \
     --options runtime \
     --timestamp \
     --entitlements "$ENTITLEMENTS" \
     "$MAIN_EXE"
fi

echo "=== Signing the .app bundle ==="
codesign --force --verify --verbose \
  --sign "$SIGNING_IDENTITY" \
  --options runtime \
  --timestamp \
  --entitlements "$ENTITLEMENTS" \
  "$STAGED_APP"

echo "=== Verifying signature ==="
codesign --verify --verbose=4 --deep "$STAGED_APP"
spctl --assess --verbose=4 --type execute "$STAGED_APP" || true

# Optional: Prompt to test the app before notarization
echo ""
echo "=== Manual Verification Step ==="
echo "The app has been signed and is located at: $STAGED_APP"
echo "You can now try to open it manually to verify it works."
read -p "Press Enter to continue with packaging and notarization, or Ctrl+C to abort..."

# --- DMG Creation ---
DMG_NAME="Swaggerific_aarch64.dmg"
echo "Creating DMG $DMG_NAME ..."
mkdir -p "$STAGING_DIR/dmg_content"
cp -r "$STAGED_APP" "$STAGING_DIR/dmg_content/"
hdiutil create -volname "Swaggerific" -srcfolder "$STAGING_DIR/dmg_content" -ov -format UDZO "$STAGING_DIR/$DMG_NAME"
rm -rf "$STAGING_DIR/dmg_content"

echo "=== Signing DMG ==="
codesign --force --verify --verbose \
  --sign "$SIGNING_IDENTITY" \
  --timestamp \
  "$STAGING_DIR/$DMG_NAME"

PKG_NAME="Swaggerific.pkg"
INSTALLER_NAME="SwaggerificInstaller.pkg"

echo "Building pkg ($PKG_NAME) ..."
pkgbuild \
  --component "$STAGED_APP" \
  --identifier "$IDENTIFIER" \
  --version "$VERSION" \
  --install-location "/Applications" \
  --sign "$INSTALLER_IDENTITY" \
  "$PKG_NAME"

echo "Building product installer ($INSTALLER_NAME) ..."
productbuild --package "$PKG_NAME" \
  --sign "$INSTALLER_IDENTITY" \
  "$INSTALLER_NAME"

echo "Moving installer(s) to staging ..."
mv -f "$PKG_NAME" "$STAGING_DIR/"
mv -f "$INSTALLER_NAME" "$STAGING_DIR/"
STAGED_INSTALLER="$STAGING_DIR/$INSTALLER_NAME"

# --- Notarization ---
echo "=== Notarization ==="
if [[ -n "${APPLE_API_KEY_PATH:-}" && -n "${APPLE_API_KEY_ID:-}" && -n "${APPLE_API_ISSUER_ID:-}" ]]; then
  AUTH_ARGS=(--key "$APPLE_API_KEY_PATH" --key-id "$APPLE_API_KEY_ID" --issuer "$APPLE_API_ISSUER_ID")
elif [[ -n "${APPLE_ID:-}" && -n "${APPLE_PASSWORD:-}" ]]; then
  AUTH_ARGS=(--apple-id "$APPLE_ID" --password "$APPLE_PASSWORD" --team-id "$TEAM_ID")
else
  echo "Warning: No notarization credentials found (APPLE_ID/APPLE_PASSWORD or API Key). Skipping notarization."
  echo "To notarize, set APPLE_ID and APPLE_PASSWORD (app-specific) or API key env vars."
  AUTH_ARGS=()
fi

if [[ ${#AUTH_ARGS[@]} -gt 0 ]]; then
  # 1. Notarize the app (via zip)
  echo "Creating zip for app notarization..."
  APP_ZIP="$STAGING_DIR/swaggerific_app_notarization.zip"
  ditto -c -k --keepParent "$STAGED_APP" "$APP_ZIP"
  
  echo "Submitting app for notarization..."
  xcrun notarytool submit "$APP_ZIP" "${AUTH_ARGS[@]}" --wait > notarization_app_output.txt 2>&1 || true
  cat notarization_app_output.txt

  # Extract submission ID and check status
  SUBMISSION_ID=$(grep -o 'id: [a-f0-9-]*' notarization_app_output.txt | head -1 | cut -d' ' -f2)
  if ! grep -q "status: Accepted" notarization_app_output.txt; then
    echo "=== App notarization failed, fetching log ==="
    if [[ -n "${SUBMISSION_ID:-}" ]]; then
      xcrun notarytool log "$SUBMISSION_ID" "${AUTH_ARGS[@]}" notarization_app_log.json || true
      cat notarization_app_log.json || true
    fi
    exit 1
  fi
  
  echo "Stapling notarization ticket to app..."
  xcrun stapler staple "$STAGED_APP"
  rm "$APP_ZIP"

  # 1b. Notarize the DMG
  echo "Submitting DMG for notarization..."
  xcrun notarytool submit "$STAGING_DIR/$DMG_NAME" "${AUTH_ARGS[@]}" --wait > notarization_dmg_output.txt 2>&1 || true
  cat notarization_dmg_output.txt
  SUBMISSION_ID=$(grep -o 'id: [a-f0-9-]*' notarization_dmg_output.txt | head -1 | cut -d' ' -f2)
  if ! grep -q "status: Accepted" notarization_dmg_output.txt; then
    echo "=== DMG notarization failed, fetching log ==="
    if [[ -n "${SUBMISSION_ID:-}" ]]; then
      xcrun notarytool log "$SUBMISSION_ID" "${AUTH_ARGS[@]}" notarization_dmg_log.json || true
      cat notarization_dmg_log.json || true
    fi
    exit 1
  fi
  echo "Stapling notarization ticket to DMG..."
  xcrun stapler staple "$STAGING_DIR/$DMG_NAME"

  # 2. Notarize the installer package
  echo "Submitting installer for notarization..."
  xcrun notarytool submit "$STAGED_INSTALLER" "${AUTH_ARGS[@]}" --wait > notarization_pkg_output.txt 2>&1 || true
  cat notarization_pkg_output.txt

  # Extract submission ID and check status
  SUBMISSION_ID=$(grep -o 'id: [a-f0-9-]*' notarization_pkg_output.txt | head -1 | cut -d' ' -f2)
  if ! grep -q "status: Accepted" notarization_pkg_output.txt; then
    echo "=== Installer notarization failed, fetching log ==="
    if [[ -n "${SUBMISSION_ID:-}" ]]; then
      xcrun notarytool log "$SUBMISSION_ID" "${AUTH_ARGS[@]}" notarization_pkg_log.json || true
      cat notarization_pkg_log.json || true
    fi
    exit 1
  fi
  
  echo "Stapling notarization ticket to installer..."
  xcrun stapler staple "$STAGED_INSTALLER"

  # 3. Done
  echo "Notarization and stapling complete."
fi

echo "Done. Artifacts:"
echo " - $STAGING_DIR/$DMG_NAME"
echo " - $STAGING_DIR/$PKG_NAME"
echo " - $STAGING_DIR/$INSTALLER_NAME"

# ------------------------
# Simple upload to GitHub Release (latest_macos)
# ------------------------
if command -v gh >/dev/null 2>&1; then
  echo "Uploading artifacts to https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG ..."
  # Ensure release exists (no frills); if it doesn't, print a hint and skip
  if gh release view "$RELEASE_TAG" --repo "$GITHUB_REPO" >/dev/null 2>&1; then
    gh release upload "$RELEASE_TAG" "$STAGING_DIR/$DMG_NAME" --repo "$GITHUB_REPO" --clobber || true
    gh release upload "$RELEASE_TAG" "$STAGING_DIR/$INSTALLER_NAME" --repo "$GITHUB_REPO" --clobber || true
    echo "Upload complete."
  else
    echo "Release tag '$RELEASE_TAG' does not exist on $GITHUB_REPO. Create it first, then re-run."
    echo "Hint: gh release create $RELEASE_TAG -t \"latest macOS\" -n \"Automated macOS build\" --repo $GITHUB_REPO"
  fi
else
  echo "GitHub CLI (gh) not found. To upload manually run:"
  echo "  gh auth login"
  echo "  gh release upload $RELEASE_TAG $STAGING_DIR/$DMG_NAME --repo $GITHUB_REPO --clobber"
fi


set +u
sdk default java $CURRENT_JAVA
set -u