[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ozkanpakdil_swaggerific&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ozkanpakdil_swaggerific)
# swaggerific
A user interface (UI) designed to interact with APIs using Swagger documentation.

Features:
- Retrieve and load Swagger JSON to fetch API documentation.
- Display a list of functions (endpoints) on the left-hand side of the UI for easy navigation.
- Allow users to select a specific function (endpoint) from the list.
- Provide HTTP method selection (POST, GET, etc.) for the chosen function.
- Allow users to input data (payload) to be sent along with the API request.
- Offer a testing functionality, enabling users to execute the selected function with the chosen method and data.

Additional Considerations / not implemented features:
- The UI will be built using JavaFX.
- Error handling and display of API responses will be implemented.
- Any necessary authentication or security features will be included.

# look
![image](https://github.com/ozkanpakdil/swaggerific/assets/604405/bd6f76ed-64cc-4bcb-85d0-480c84ea06db)

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
