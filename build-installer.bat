@echo off
chcp 65001 >nul

set "JAVA_HOME=C:\Users\Denis\Downloads\jdk-21.0.11"
set "JAVAFX_SDK=C:\Users\Denis\Downloads\javafx-sdk-21.0.11"
set "APP_NAME=GateControl"
set "APP_VERSION=1.0.0"
set "MAIN_JAR=gatecontrol-1.0.0.jar"
set "MAIN_CLASS=org.springframework.boot.loader.launch.JarLauncher"

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "TARGET_DIR=%PROJECT_DIR%\target"
set "INPUT_DIR=%PROJECT_DIR%\jpackage-input"
set "IMAGE_DIR=%PROJECT_DIR%\jpackage-image"
set "RUNTIME_DIR=%PROJECT_DIR%\jre-runtime"
set "OUTPUT_DIR=%PROJECT_DIR%\installer-output"
set "RESOURCES_DIR=%PROJECT_DIR%\src\main\resources"

set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo [1/5] Building fat JAR with Maven...
echo.
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed.
    exit /b 1
)

echo.
echo [2/5] Preparing jpackage input directory...
echo.
if exist "%INPUT_DIR%" rd /s /q "%INPUT_DIR%"
md "%INPUT_DIR%"

if not exist "%TARGET_DIR%\%MAIN_JAR%" (
    echo ERROR: JAR not found at %TARGET_DIR%\%MAIN_JAR%
    exit /b 1
)
copy "%TARGET_DIR%\%MAIN_JAR%" "%INPUT_DIR%\%MAIN_JAR%" >nul
echo Copied JAR.

if exist "%RESOURCES_DIR%\db.properties" (
    copy "%RESOURCES_DIR%\db.properties" "%INPUT_DIR%\db.properties" >nul
    echo Copied db.properties.
) else (
    echo WARNING: db.properties not found - skipping.
)

if exist "%RESOURCES_DIR%\images\soldier_default.png" (
    md "%INPUT_DIR%\images" 2>nul
    copy "%RESOURCES_DIR%\images\soldier_default.png" "%INPUT_DIR%\images\soldier_default.png" >nul
    echo Copied soldier_default.png.
)

echo Copying JavaFX native DLLs...
if not exist "%JAVAFX_SDK%\bin" (
    echo ERROR: JavaFX SDK bin not found at %JAVAFX_SDK%\bin
    exit /b 1
)
copy "%JAVAFX_SDK%\bin\*.dll" "%INPUT_DIR%\" >nul
echo Done.

echo.
echo [3/5] Creating custom JRE with jlink...
echo.
if exist "%RUNTIME_DIR%" rd /s /q "%RUNTIME_DIR%"

"%JAVA_HOME%\bin\jlink" ^
  --module-path "%JAVA_HOME%\jmods" ^
  --add-modules ALL-MODULE-PATH ^
  --no-header-files ^
  --no-man-pages ^
  --strip-debug ^
  --output "%RUNTIME_DIR%"

if errorlevel 1 (
    echo ERROR: jlink failed.
    exit /b 1
)
echo Done.

echo.
echo [4/5] Running jpackage...
echo.
if exist "%IMAGE_DIR%" rd /s /q "%IMAGE_DIR%"
if exist "%PROJECT_DIR%\%APP_NAME%" rd /s /q "%PROJECT_DIR%\%APP_NAME%"

"%JAVA_HOME%\bin\jpackage" ^
  --type app-image ^
  --input "%INPUT_DIR%" ^
  --dest "%PROJECT_DIR%" ^
  --name "%APP_NAME%" ^
  --app-version %APP_VERSION% ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --icon "%RESOURCES_DIR%\app.ico" ^
  --runtime-image "%RUNTIME_DIR%" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --java-options "-Dstdout.encoding=UTF-8" ^
  --java-options "-Dspring.profiles.active=prod" ^
  --java-options "-Djava.library.path=$APPDIR" ^
  --java-options "-Dapp.db.properties=$APPDIR/db.properties" ^
  --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.io=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.net=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.nio=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/sun.net.www.protocol.file=ALL-UNNAMED" ^
  --java-options "--add-opens=java.management/sun.management=ALL-UNNAMED" ^
  --java-options "--add-opens=java.desktop/sun.awt=ALL-UNNAMED" ^
  --java-options "--add-opens=java.desktop/sun.java2d=ALL-UNNAMED"

if errorlevel 1 (
    echo ERROR: jpackage failed.
    exit /b 1
)

if exist "%PROJECT_DIR%\%APP_NAME%" (
    rename "%PROJECT_DIR%\%APP_NAME%" jpackage-image
) else (
    echo ERROR: jpackage did not produce expected output directory: %APP_NAME%
    exit /b 1
)
echo Done.

echo.
echo [5/5] Building installer with Inno Setup...
echo.
if not exist "%OUTPUT_DIR%" md "%OUTPUT_DIR%"

cmd /c ""C:\Program Files\Inno Setup 7\ISCC.exe" "%PROJECT_DIR%\GateControl.iss""
if errorlevel 1 (
    echo ERROR: Inno Setup compilation failed.
    exit /b 1
)

echo.
echo BUILD COMPLETE
echo Installer: %OUTPUT_DIR%\GateControl-Setup-%APP_VERSION%.exe
echo.
