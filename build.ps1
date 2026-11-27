param([string[]]$Tasks=@("testDebugUnitTest","assembleDebug"))
$ErrorActionPreference="Stop"
if(-not $env:JAVA_HOME){
    $jdk=Get-ChildItem "C:\Users\a2643\JilemeTools\jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if($jdk){$env:JAVA_HOME=$jdk.FullName}
}
if(-not $env:ANDROID_HOME -and (Test-Path "C:\Users\a2643\JilemeTools\android-sdk")){$env:ANDROID_HOME="C:\Users\a2643\JilemeTools\android-sdk"}
$env:SDK_TEST_BASE_URL="https://mirrors.cloud.tencent.com/AndroidSDK/"
& ".\gradlew.bat" @Tasks --console=plain
exit $LASTEXITCODE
