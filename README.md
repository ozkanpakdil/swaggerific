# swaggerific

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ozkanpakdil_swaggerific&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ozkanpakdil_swaggerific) ![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/ozkanpakdil/swaggerific?labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit%20Reviews)
[![blazingly fast](https://blazingly.fast/api/badge.svg?repo=ozkanpakdil%2Fswaggerific)](https://blazingly.fast)

Overall Downloads on
github: ![All downloads of github releases](https://img.shields.io/github/downloads/ozkanpakdil/swaggerific/total)

You can download the latest version
from [Github releases ![Download count of latest releases](https://img.shields.io/github/downloads/ozkanpakdil/swaggerific/latest/total.svg)](https://github.com/ozkanpakdil/swaggerific/releases)
Follow [here](https://bsky.app/profile/swaggerific.bsky.social) for new releases.

## Install via Homebrew (macOS arm64)

Swaggerific is available as a Homebrew Cask for Apple Silicon (arm64) macOS.

1) Tap this repository explicitly by URL:

```bash
brew tap ozkanpakdil/swaggerific https://github.com/ozkanpakdil/swaggerific
```

2) Install into your user Applications folder:

```bash
brew install --cask swaggerific --appdir=~/Applications
```

- Update to the latest build later:

```bash
brew upgrade --cask swaggerific
```

- Uninstall:

```bash
brew uninstall --cask swaggerific
```

Notes:
- This cask targets Apple Silicon (arm64) builds only.
- It pulls the latest prebuilt app bundle from the `latest_macos` GitHub release.

Tip (Gatekeeper on unsigned apps): If you see a message like “swaggerific.app is damaged and can’t be opened”, that’s macOS Gatekeeper blocking a quarantined, unsigned app from a personal tap. You can either install without quarantine or remove the quarantine attribute after install:

```bash
# Option A: install without quarantine
brew uninstall --cask swaggerific
brew install --cask --no-quarantine swaggerific --appdir=~/Applications

# Option B: keep install, remove quarantine from the app bundle
xattr -dr com.apple.quarantine ~/Applications/swaggerific.app
```

We plan to add signing/notarization later; for now this personal tap uses unsigned builds.

## Packaging artifacts

The macOS packaging script `build-macos-arm64.sh` places generated artifacts under the `staging/` directory (tar.gz, .pkg, and installer .pkg). The `staging/` folder is ignored by Git.

## Requirements

- Java Development Kit (JDK) 17 or higher
- JavaFX (included in the distribution)

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

## Running the Application

### From Distribution Package

1. Download the latest release from [GitHub Releases](https://github.com/ozkanpakdil/swaggerific/releases)
2. Extract the ZIP file
3. Run the application using the provided script:
    - Linux/macOS: `./run.sh`
    - Windows: `run.bat`

### From Source

1. Clone the repository
2. Build with Maven: `mvn package`
3. Unzip the generated `swaggerific-X.Y.Z.zip` file under target/dist
4. Run the application using the provided script:
    - Linux/macOS: `./run.sh`
    - Windows: `run.bat`

### Run tests in macOS

```shell
mvn -Djava.awt.headless=false -Dtestfx.robot=glass -Dsurefire.parallel=none test
```

test

### Run agent

```shell
mvn gluonfx:runAgent
```
