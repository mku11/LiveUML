@echo ON
set CURRDIR=%CD%

set VERSION=1.0.0

:: LiveUML package script
set OUTPUT_ROOT=..\..\output
set OUTPUT_DIR=%OUTPUT_ROOT%
set LIVE_UML_JAR=.\build\libs\LiveUML-%VERSION%.jar
set DEPS_LIBS_DIR=.\libs
set LIVE_UML_SCRIPTS_DIR=.\scripts

set PACKAGES_DIR=app_package
set LIVE_UML=LiveUML
set LIVE_UML_PACKAGE_NAME=%LIVE_UML%.%VERSION%

powershell mkdir -ErrorAction SilentlyContinue %PACKAGES_DIR%
del /S /Q .\%PACKAGES_DIR%\*

powershell mkdir -ErrorAction SilentlyContinue %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%
del /S /Q .\%PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%\*

powershell mkdir -ErrorAction SilentlyContinue %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%\LiveUML
copy /Y %LIVE_UML_JAR% %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%\LiveUML\
robocopy /E %DEPS_LIBS_DIR%\ %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%\libs\

copy %LIVE_UML_SCRIPTS_DIR%\* %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%\
copy README.txt %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%\

cd %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%
powershell -command Compress-Archive -Force -DestinationPath ..\%LIVE_UML_PACKAGE_NAME%.zip *
cd ..\..\
powershell mkdir -ErrorAction SilentlyContinue %OUTPUT_DIR%
copy /Y %PACKAGES_DIR%\%LIVE_UML_PACKAGE_NAME%.zip %OUTPUT_DIR%

cd %CURRDIR%