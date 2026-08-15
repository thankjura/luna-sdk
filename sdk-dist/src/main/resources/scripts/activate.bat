@echo off

if not "%_OLD_LUNA_SDK_PATH%"=="" (
    call deactivate
)

set "LUNA_SDK_VERSION=${version}"
set "LUNA_SDK_MAVEN_VER=${apache.maven.version}"

set "_OLD_LUNA_SDK_PATH=%PATH%"
set "_OLD_LUNA_SDK_PROMPT=%PROMPT%"

set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

cd /d "%SCRIPT_DIR%\.."
set "LUNA_SDK_DIR=%CD%"
cd /d "%SCRIPT_DIR%"

set "PATH=%LUNA_SDK_DIR%\bin;%LUNA_SDK_DIR%\apache-maven-%LUNA_SDK_MAVEN_VER%\bin;%PATH%"

if "%PROMPT%"== "" (
    set "PROMPT=(luna-sdk %LUNA_SDK_VERSION%) $P$G"
) else (
    set "PROMPT=(luna-sdk %LUNA_SDK_VERSION%) %PROMPT%"
)