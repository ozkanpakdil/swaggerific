# swaggerific

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ozkanpakdil_swaggerific&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ozkanpakdil_swaggerific)

Overall Downloads on github: ![All downloads of github releases](https://img.shields.io/github/downloads/ozkanpakdil/swaggerific/total)

You can download the latest version from [Github releases ![Download count of latest releases](https://img.shields.io/github/downloads/ozkanpakdil/swaggerific/latest/total.svg)](https://github.com/ozkanpakdil/swaggerific/releases) 

A user interface (UI) designed to interact with APIs using Swagger or OpenAPI jsons.

Features:
- The UI is built using JavaFX.
- Error handling and display of API responses implemented.
- Retrieve and load Swagger JSON to fetch API documentation.
- Display a list of functions (endpoints) on the left-hand side of the UI for easy navigation.
- Allow users to select a specific function (endpoint) from the list.
- Provide HTTP method selection (POST, GET, etc.) for the chosen function.
- Allow users to input data (payload) to be sent along with the API request.
- Offer a testing functionality, enabling users to execute the selected function with the chosen method and data.

Additional Considerations / not implemented features:
- Any necessary authentication or security features will be included.

# look
![using-get](https://github.com/ozkanpakdil/swaggerific/assets/604405/748eb2a8-3578-45e3-ac95-e8246ef27785)

# native build
```shell
mvn gluonfx:build gluonfx:nativerun
```
# update reflection classes
there are classes needs to be in the list of reflection list of the compiler(which was in the pom.xml before check 'git history'), that list can be generated from command below, that will update the jsons under META-INF/native-image  
```shell
mvn gluonfx:runagent
```

https://petstore.swagger.io/#/pet

 [Open source Support](https://jb.gg/OpenSourceSupport) by [JetBrains](https://www.jetbrains.com)
