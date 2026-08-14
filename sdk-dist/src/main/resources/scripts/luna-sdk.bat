@echo off
setlocal enabledelayedexpansion

set "LUNA_SDK_MAVEN_VER=${apache.maven.version}"

if "%LUNA_SDK_DIR%"=="" (
    set "SCRIPT_DIR=%~dp0"
    set "SCRIPT_DIR=!SCRIPT_DIR:~0,-1!"

    cd /d "!SCRIPT_DIR!\.."
    set "LUNA_SDK_DIR=!cd!"
)

if "%JAVA%"=="" (
    set "JAVA=java"
)

set "JAR_LIB_DIR=%LUNA_SDK_DIR%\libs"

"%JAVA%" -cp "%JAR_LIB_DIR%\*" -Dfile.encoding=UTF-8 -Dmvn.directory="%LUNA_SDK_DIR%\apache-maven-%LUNA_SDK_MAVEN_VER%" -Dluna.sdk.home="%LUNA_SDK_DIR%" -Dmnv.repository="%LUNA_SDK_DIR%\repository" "ru.slie.luna.sdk.LunaCliMain" %*

endlocal