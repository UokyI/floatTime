$sdkRoot = "D:\yuan\workspace\Android\sdk"
$base    = "https://mirrors.cloud.tencent.com/AndroidSDK"

New-Item -ItemType Directory -Force -Path $sdkRoot | Out-Null

function Test-Ready {
  param($dir)
  $ok = (Test-Path $dir) -and ((Get-ChildItem $dir -Force -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0)
  if ($ok) { Write-Output "  [skip] 已存在: $dir" }
  return $ok
}

# 1) platform-tools
Write-Output "[1/4] platform-tools ..."
if (-not (Test-Ready "$sdkRoot\platform-tools")) {
  curl.exe -L -o "$sdkRoot\pt.zip" "$base/platform-tools_r34.0.5-windows.zip"
  Expand-Archive "$sdkRoot\pt.zip" -DestinationPath $sdkRoot
  Remove-Item "$sdkRoot\pt.zip"
}

# 2) build-tools 34.0.0
Write-Output "[2/4] build-tools 34.0.0 ..."
if (-not (Test-Ready "$sdkRoot\build-tools\34.0.0")) {
  New-Item -ItemType Directory -Force -Path "$sdkRoot\build-tools\34.0.0" | Out-Null
  curl.exe -L -o "$sdkRoot\bt.zip" "$base/build-tools_r34-windows.zip"
  Expand-Archive "$sdkRoot\bt.zip" -DestinationPath "$sdkRoot\build-tools\34.0.0"
  Remove-Item "$sdkRoot\bt.zip"
}
if (Test-Path "$sdkRoot\build-tools\34.0.0\android-14") {
  Move-Item "$sdkRoot\build-tools\34.0.0\android-14\*" "$sdkRoot\build-tools\34.0.0\" -Force
  Remove-Item "$sdkRoot\build-tools\34.0.0\android-14" -Recurse -Force
}
"Pkg.UserSrc=false`r`nPkg.Revision=34.0.0" |
  Set-Content "$sdkRoot\build-tools\34.0.0\source.properties" -Encoding ascii

# 3) platforms/android-34
Write-Output "[3/4] platforms android-34 ..."
if (-not (Test-Ready "$sdkRoot\platforms\android-34")) {
  New-Item -ItemType Directory -Force -Path "$sdkRoot\platforms\android-34" | Out-Null
  curl.exe -L -o "$sdkRoot\p34.zip" "$base/platform-34-ext8_r01.zip"
  Expand-Archive "$sdkRoot\p34.zip" -DestinationPath "$sdkRoot\platforms\android-34"
  Remove-Item "$sdkRoot\p34.zip"
  if (Test-Path "$sdkRoot\platforms\android-34\android-14") {
    Move-Item "$sdkRoot\platforms\android-34\android-14\*" "$sdkRoot\platforms\android-34\" -Force
    Remove-Item "$sdkRoot\platforms\android-34\android-14" -Recurse -Force
  }
  if (Test-Path "$sdkRoot\platforms\android-34\android-34") {
    Move-Item "$sdkRoot\platforms\android-34\android-34\*" "$sdkRoot\platforms\android-34\" -Force
    Remove-Item "$sdkRoot\platforms\android-34\android-34" -Recurse -Force
  }
}
"Pkg.UserSrc=false`r`nPkg.Revision=1`r`nPkg.Desc=Android SDK Platform 14" |
  Set-Content "$sdkRoot\platforms\android-34\source.properties" -Encoding ascii

# 4) emulator (只建空目录占位，不下 300MB 整包)
Write-Output "[4/4] emulator 占位 ..."
if (-not (Test-Ready "$sdkRoot\emulator")) {
  New-Item -ItemType Directory -Force -Path "$sdkRoot\emulator" | Out-Null
}
"Pkg.UserSrc=false`r`nPkg.Revision=34.2.1" |
  Set-Content "$sdkRoot\emulator\source.properties" -Encoding ascii

# 5) licenses (避免 Android Studio 报“未接受许可”)
$licDir = "$sdkRoot\licenses"
if (-not (Test-Path $licDir)) {
  New-Item -ItemType Directory -Force -Path $licDir | Out-Null
}
"24333f8a63b6825ea9c5514f83c2829d" |
  Set-Content "$licDir\android-sdk-license" -Encoding ascii

# 6) 设置环境变量(当前用户永久生效)
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $sdkRoot, "User")

Write-Output ""
Write-Output "Done. SDK path: $sdkRoot"
Write-Output "Directory tree:"
Get-ChildItem $sdkRoot -Directory | ForEach-Object { Write-Output ("  " + $_.Name) }
