$ErrorActionPreference = "Stop"

function global:deactivate {
    if (Test-Path Variable:\_OLD_LUNA_SDK_PATH) {
        $env:PATH = $_OLD_LUNA_SDK_PATH
        Remove-Item Variable:\_OLD_LUNA_SDK_PATH -ErrorAction SilentlyContinue
    }
    if (Test-Path Variable:\_OLD_LUNA_SDK_PROMPT) {
        Set-Item -Path filter:\prompt -Value $_OLD_LUNA_SDK_PROMPT
        Remove-Item Variable:\_OLD_LUNA_SDK_PROMPT -ErrorAction SilentlyContinue
    }

    Remove-Item env:LUNA_SDK_DIR -ErrorAction SilentlyContinue
    Remove-Item env:LUNA_SDK_MAVEN_VER -ErrorAction SilentlyContinue
    Remove-Item env:LUNA_SDK_VERSION -ErrorAction SilentlyContinue

    Remove-Item function:\deactivate -ErrorAction SilentlyContinue
}

if (Test-Path Variable:\_OLD_LUNA_SDK_PATH) {
    deactivate
}

$env:LUNA_SDK_VERSION = "${version}"
$env:LUNA_SDK_MAVEN_VER = "${apache.maven.version}"

$global:_OLD_LUNA_SDK_PATH = $env:PATH

$BIN_DIR = $PSScriptRoot
$env:LUNA_SDK_DIR = (Get-Item "$BIN_DIR\..").FullName

$env:PATH = "$BIN_DIR;$env:LUNA_SDK_DIR\apache-maven-$env:LUNA_SDK_MAVEN_VER\bin;$env:PATH"

if (Test-Path filter:\prompt) {
    $global:_OLD_LUNA_SDK_PROMPT = Get-Content filter:\prompt
}
function global:prompt {
    write-host "(luna-sdk $($env:LUNA_SDK_VERSION)) " -NoNewline -ForegroundColor Cyan
    return "PS $($ExecutionContext.SessionState.Path.CurrentLocation)> "
}