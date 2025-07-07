@echo off
echo Setting up Visual Studio Build Environment...
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
echo Building native image with GluonFX...
mvn gluonfx:build
echo Build completed.
pause