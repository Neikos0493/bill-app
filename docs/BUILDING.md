# 构建与发布速查

主需求与进度见 [APP_BLUEPRINT.md](APP_BLUEPRINT.md)。

## 环境

- JDK 17；
- Android SDK 35、Build Tools 35.0.0、Platform Tools；
- Gradle Wrapper 8.10.2；
- `local.properties` 中配置本机 `sdk.dir`，该文件不提交。

当前工作机通过 `C:\Users\a2643\JilemeTools` 和 `C:\Users\a2643\JilemeProject` 的英文路径映射规避 Android 工具对中文路径的兼容问题，原源码仍保存在原工作目录。

## 常用命令

```powershell
# 单元测试和 Debug APK
.\build.ps1

# 直接调用 Wrapper
.\gradlew.bat testDebugUnitTest assembleDebug

# 连接安卓设备后的仪器与 Compose 测试
.\gradlew.bat connectedDebugAndroidTest

# Release APK（需先配置签名）
.\gradlew.bat assembleRelease
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK：

```text
app/build/outputs/apk/release/app-release.apk
```

## 签名配置

在项目根目录创建不提交到 Git 的 `keystore.properties`：

```properties
storeFile=C:/path/to/jileme-release.jks
storePassword=替换为本地密码
keyAlias=jileme
keyPassword=替换为本地密码
```

签名文件和密码必须另行备份。丢失或更换签名会导致老版本无法覆盖升级。

## 发布

1. 运行全部单元测试、仪器测试和 Release 构建。
2. 递增 `versionCode`，设置 `versionName`。
3. 构建并验证 `app-release.apk`。
4. 在 GitHub Releases 新建对应标签。
5. 上传 `app-release.apk` 与 `version.json`。
6. 将发布结果和升级验证写回主蓝图。

## 自动化

`.github/workflows/android.yml` 在推送到 `main` 或收到 PR 时运行单元测试、Debug 构建并上传构建产物。
