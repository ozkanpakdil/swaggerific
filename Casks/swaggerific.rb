cask "swaggerific" do
  version "0.0.4"
  sha256 "3a6e1f37df338452102922546b89c854ee2b72334d44481c3aa702ca81d27519"

  url "https://github.com/ozkanpakdil/swaggerific/releases/download/latest_macos/Swaggerific_x86_64.dmg",
      verified: "github.com/ozkanpakdil/swaggerific"
  name "Swaggerific"
  desc "Simple GUI app for working with Swagger/OpenAPI"
  homepage "https://github.com/ozkanpakdil/swaggerific"

  app "swaggerific.app"

  caveats <<~EOS
    This app is signed and notarized with a Developer ID certificate.
    It should open without any Gatekeeper warnings.
  EOS
end
