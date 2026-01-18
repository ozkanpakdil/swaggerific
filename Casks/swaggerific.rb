cask "swaggerific" do
  version "0.0.4"
  sha256 "261772c84e8326f36a238b62e482f832c8ae26513aa3c449bb05c0b73ba79671"

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
