cask "swaggerific" do
  version "0.0.4"
  sha256 "af452dc573213d4fa5ecaaf6d6c718b52bcad78bea8a39dfe00be7840d60fd94"

  url "https://github.com/ozkanpakdil/swaggerific/releases/download/latest_macos/swaggerific_x86_64-darwin.tar.gz",
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
