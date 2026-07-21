# Roamer

Roamer 是一款无需 root 的 Android 开发者工具，用于修改 SIM 卡汇报的国家代码（ISO）与运营商名称，并支持将伪装的归属地同步到特定应用的语言/区域设置（Per-app locale），方便开发者在单台设备上调试多国及多运营商逻辑。

[English](README.md) · **简体中文**

技术栈：Kotlin · Jetpack Compose · Material 3 · Shizuku · minSdk 31 (Android 12+)

## 项目简介

Roamer 通过 [Shizuku](https://shizuku.rikka.app/) 调用系统 Telephony API 暴露的 CarrierConfig 覆盖层，使应用读取到指定的 SIM 卡归属地和运营商名称，无需更换实体 SIM 卡。

同时，Roamer 支持按应用设置区域覆盖（Android 13+）：选中的应用将跟随主 SIM 卡当前伪装的国家代码（例如伪装为日本时应用自动使用 `ja-JP` 区域设置）。当 SIM 还原或功能关闭时，应用会自动恢复至初始的区域设置。

## 主要功能

- 无需 Root：通过 Shizuku 覆盖 SIM 卡国家 ISO 及运营商名称。
- 多卡支持：支持多 SIM 卡独立配置或一键批量应用/还原。
- 内置预设：提供常用国家与地区（美国、日本、韩国、中国大陆、中国香港等）及主流运营商配置。
- 无快照还原：在运行时根据 MCC 实时重导真实值并回写，无需保存历史快照。
- 应用级区域同步：可选择特定应用跟随 SIM 伪装区域。
- 调试友好：原值与当前值对比展示，技术参数（MCC/MNC/ISO/subId）采用等宽字体显示，支持浅色/深色主题。

## 覆盖能力与边界

Roamer 仅修改 CarrierConfig 的覆盖层，不修改底层 SIM 卡或 RIL 硬件信息。

| 目标属性 | 是否可覆写 | Telephony API |
| --- | --- | --- |
| 国家代码 (Country ISO) | 是 | `getSimCountryIso` |
| 运营商名称 (Carrier Name) | 是 | `getSimOperatorName` |
| MCC / MNC 数字 | 否 | `getSimOperator` (SELinux 保护) |
| 在网注册信息 | 否 | `getNetworkOperator` / `getNetworkCountryIso` |

由于 MNC 和网络注册信息无法覆盖，Roamer 不会修改真实手机号码或基站连接信息。应用能否被正确伪装取决于其内部读取的是哪个 Telephony API 或位置服务。

## 环境要求

- Android 12+ (minSdk 31)，已在 Android 16 (Samsung) 上测试。
- 已安装并启动 [Shizuku](https://shizuku.rikka.app/)（支持无线调试或 ADB 启动）。
- 首次启动需授予 `READ_PHONE_STATE` 权限以读取卡槽信息。
- 应用级区域同步功能需要 Android 13+ 系统支持。

## 技术实现

### 提权机制

在部分 OEM 设备（如三星）上，直接通过 Shizuku 调用 `overrideConfig` 会因 `getCallingUid() == SHELL` 校验而被拒绝。

Roamer 的解决路径：
1. 通过 Shizuku 调用 `IActivityManager.startInstrumentation`（附加 `INSTR_FLAG_NO_RESTART` 标志，避免应用进程重启）。
2. Instrumentation 在应用进程内运行并执行 `startDelegateShellPermissionIdentity(myUid, null)`。
3. 将 Shell 权限委派给应用自身的 UID，随后带 `MODIFY_PHONE_STATE` 权限以应用身份执行 `overrideConfig`。

### 无快照还原

Roamer 不依赖本地保存的初始状态快照。在执行还原时：
- 真实国家 ISO 通过不可变 MCC (`getSimOperator`) 重新派生。还原过程先回写真实 ISO 以更新订阅数据库，随后执行 `overrideConfig(null)` 清理覆盖层。
- 运营商名称在覆盖层被清理后，系统再次读取时会自动恢复为原厂配置。

### 反射与隐藏 API

隐藏 API 的调用全部基于 [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) 实现，编译期无需依赖隐藏 Stub。

## 构建说明

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug        # 构建 Debug APK
./gradlew :app:testDebugUnitTest    # 执行 JVM 单元测试
```

## 项目结构

```
com.eigenlux.roamer/
├── core/   # Telephony API、Shizuku 绑定与 Instrumentation 提权逻辑
│   ├── ShizukuManager
│   ├── CarrierConfigController
│   ├── InstrumentationTrigger
│   ├── PrivilegedOverrideInstrumentation
│   ├── PrivilegedSubscriptionReader
│   ├── RegionLogic
│   └── LocaleOverrideController
├── data/   # 预设数据与本地状态存储
├── ui/     # Compose 界面、Material 3 主题与 MainActivity
└── AppPickerScreen.kt
```

## 国际化

界面文案维护在 `res/values`（默认英文）与 `res/values-zh`（中文）。JVM 单元测试 (`StringsParityTest`) 会自动检查两套文案 Key 的一致性。

## 已知限制

- 提权路径主要针对三星设备上 Shell UID 的限制逻辑进行适配，其他厂商设备为 Best-effort 支持。
- 若在操作过程中 SIM 卡断开连接（如飞行模式），底层 Telephony 状态更新可能会有延迟。

## 开源协议

基于 [MIT License](LICENSE) 开源。

## 致谢

参考了以下开源项目在无 Root 下通过 Shizuku 覆盖 CarrierConfig 的探索：
- [Ackites/Nrfr](https://github.com/Ackites/Nrfr)
- [lmh-codes/Nrfr](https://github.com/lmh-codes/Nrfr)
