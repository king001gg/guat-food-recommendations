@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup script for Windows
@REM ----------------------------------------------------------------------------

@echo off
setlocal enabledelayedexpansion

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if not defined JAVA_HOME (
    for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
) else (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if not defined JAVACMD (
    echo ERROR: JAVA_HOME is not set and no 'java' command could be found. >&2
    exit /b 1
)

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    curl -f -L -o "%WRAPPER_JAR%" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    if errorlevel 1 (
        echo ERROR: Failed to download Maven Wrapper. >&2
        exit /b 1
    )
)

if not exist "%MAVEN_HOME%" (
    echo Downloading Maven 3.9.9...
    set "MAVEN_ZIP=%MAVEN_HOME%\maven.zip"
    mkdir "%MAVEN_HOME%" 2>nul
    curl -f -L -o "%MAVEN_ZIP%" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
    if errorlevel 1 (
        echo ERROR: Failed to download Maven. >&2
        exit /b 1
    )
    echo Extracting Maven...
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force" 2>nul
    if errorlevel 1 (
        echo Unzip with powershell failed, trying tar...
        tar -xf "%MAVEN_ZIP%" -C "%MAVEN_HOME%" 2>nul
    )
)

"%JAVACMD%" ^
    -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" ^
    -jar "%WRAPPER_JAR%" ^
    %*
