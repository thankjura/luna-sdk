@echo off

if not "%_OLD_LUNA_SDK_PATH%"=="" (
    set "PATH=%_OLD_LUNA_SDK_PATH%"
    set "_OLD_LUNA_SDK_PATH="
)

if not "%_OLD_LUNA_SDK_PROMPT%"=="" (
    set "PROMPT=%_OLD_LUNA_SDK_PROMPT%"
    set "_OLD_LUNA_SDK_PROMPT="
)

set "LUNA_SDK_VERSION="
set "LUNA_SDK_MAVEN_VER="
set "LUNA_SDK_DIR="