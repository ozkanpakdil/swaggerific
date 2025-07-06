@echo off
echo Starting Swaggerific...

REM Set JavaFX version
set JAVAFX_VERSION=${javafx.version}
echo Using JavaFX version: %JAVAFX_VERSION%

REM Set OS-specific suffix
set JAVAFX_OS=win

REM Build module path for JavaFX using local lib directory
set MODULE_PATH=
set MODULES_TO_ADD=javafx.controls,javafx.web,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.swing

REM Check if we have the modules for this OS
if not exist "lib\%JAVAFX_OS%" (
    echo Error: JavaFX modules for %JAVAFX_OS% not found in lib\%JAVAFX_OS%
    echo Please run the application on Windows or download the appropriate package.
    pause
    exit /b 1
)

REM Add JavaFX modules to module path
call :AddModule "javafx-base"
call :AddModule "javafx-controls"
call :AddModule "javafx-fxml"
call :AddModule "javafx-graphics"
call :AddModule "javafx-media"
call :AddModule "javafx-web"
call :AddModule "javafx-swing"

REM Add GraalVM modules to module path
setlocal EnableDelayedExpansion
set GRAALVM_VERSION=${graalvm.version}
set GRAALVM_MODULES=polyglot-%GRAALVM_VERSION%.jar js-scriptengine-%GRAALVM_VERSION%.jar truffle-api-%GRAALVM_VERSION%.jar truffle-compiler-%GRAALVM_VERSION%.jar truffle-runtime-%GRAALVM_VERSION%.jar jniutils-%GRAALVM_VERSION%.jar

for %%m in (%GRAALVM_MODULES%) do (
    set "MODULE_JAR=lib\%%m"
    if exist "!MODULE_JAR!" (
        if "!MODULE_PATH!"=="" (
            set "MODULE_PATH=!MODULE_JAR!"
        ) else (
            set "MODULE_PATH=!MODULE_PATH!;!MODULE_JAR!"
        )
    ) else (
        echo Warning: GraalVM module not found: !MODULE_JAR!
    )
)
endlocal & set "MODULE_PATH=%MODULE_PATH%"

REM Check if any modules were found
if "%MODULE_PATH%"=="" (
    echo Error: No JavaFX modules found in lib\%JAVAFX_OS%
    echo Please ensure the package includes the required JavaFX modules.
    pause
    exit /b 1
)

REM Run the application
echo Starting Swaggerific...
java --module-path "%MODULE_PATH%" ^
     --add-modules=%MODULES_TO_ADD% ^
     --add-exports=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED ^
     --add-exports=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED ^
     --add-exports=javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED ^
     --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED ^
     --add-exports=javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
     --add-exports=javafx.base/com.sun.javafx.beans=ALL-UNNAMED ^
     --add-exports=org.graalvm.truffle.runtime/com.oracle.truffle.runtime=ALL-UNNAMED ^
     --enable-native-access=ALL-UNNAMED ^
     --add-opens=java.base/java.lang=ALL-UNNAMED ^
     --add-opens=java.base/java.util=ALL-UNNAMED ^
     -Dpolyglot.engine.WarnInterpreterOnly=false ^
     -jar "swaggerific-${project.version}.jar"
pause
exit /b

:AddModule
set MODULE=%~1
set MODULE_JAR=lib\%JAVAFX_OS%\%MODULE%-%JAVAFX_VERSION%-%JAVAFX_OS%.jar
if exist "%MODULE_JAR%" (
    if "%MODULE_PATH%"=="" (
        set MODULE_PATH=%MODULE_JAR%
    ) else (
        set MODULE_PATH=%MODULE_PATH%;%MODULE_JAR%
    )
) else (
    echo Warning: JavaFX module not found: %MODULE_JAR%
)
exit /b
