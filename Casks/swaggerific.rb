cask "swaggerific" do
  version :latest
  sha256 :no_check

  url "https://github.com/ozkanpakdil/swaggerific/releases/download/latest_macos/swaggerific_aarch64-darwin.tar.gz",
      verified: "github.com/ozkanpakdil/swaggerific"
  name "Swaggerific"
  desc "Simple GUI app for working with Swagger/OpenAPI"
  homepage "https://github.com/ozkanpakdil/swaggerific"

  depends_on arch: :arm64

  app "swaggerific.app"

  caveats <<~EOS
    This cask targets Apple Silicon (arm64) builds.
    To install the app into your user Applications folder, run:
      brew install --cask swaggerific --appdir=~/Applications

    If you see “app is damaged and can’t be opened”, macOS Gatekeeper is
    blocking a quarantined, unsigned app from a personal tap. Either install
    without quarantine:
      brew uninstall --cask swaggerific
      brew install --cask --no-quarantine swaggerific --appdir=~/Applications
    or remove the quarantine attribute after install:
      xattr -dr com.apple.quarantine ~/Applications/swaggerific.app
  EOS
end
