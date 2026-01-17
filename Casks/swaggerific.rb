cask "swaggerific" do
  version :latest
  sha256 :no_check

  url "https://github.com/ozkanpakdil/swaggerific/releases/download/latest_macos/swaggerific_x86_64-darwin.tar.gz",
      verified: "github.com/ozkanpakdil/swaggerific"
  name "Swaggerific"
  desc "Simple GUI app for working with Swagger/OpenAPI"
  homepage "https://github.com/ozkanpakdil/swaggerific"

  app "swaggerific.app"

  caveats <<~EOS
    This app is signed with a Developer ID certificate.
    If you encounter any Gatekeeper issues on first launch, right-click the app
    and select "Open" to bypass the initial security prompt.
  EOS
end
