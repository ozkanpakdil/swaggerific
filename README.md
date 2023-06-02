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