# Roamer

无 root 的 Android 工具,用于覆写 SIM 卡的**归属地(国家/地区)与运营商名**,并可选地把该归属地**镜像到选定应用的 per-app locale**,让开发者在自己的设备上模拟不同国家 / 运营商环境做调试。

[English](README.md) · **简体中文**

`Kotlin` · `Jetpack Compose` · `Material 3` · `Shizuku` · minSdk 31(Android 12+)

## 它能做什么

Roamer 修改系统通过 telephony API 暴露的 CarrierConfig **覆盖层**,让应用把你的 SIM 读成属于另一个国家或运营商。它经 [Shizuku](https://shizuku.rikka.app/) 借用 shell 特权,**无需 root**。

典型用途:验证自己的应用面向美国 / 日本 / 中国香港 / 韩国用户、或特定运营商时的表现,而不必更换实体 SIM。

它还提供可选的**按应用区域覆盖**:勾选一组应用,它们的 per-app locale 会跟随主卡槽的伪装归属地(例如把 SIM 伪装成日本,选中的应用就读成/显示成 `ja-JP`)。默认关闭,SIM 还原时各应用自动回退到原本的 locale。

## 功能

- 无 root 覆写 SIM 的国家 ISO 与运营商名(经 Shizuku)。
- 双卡:一次操作批量覆写或还原所有卡槽。
- 一键伪装为 US / JP / KR / CN / HK,使用各地区市场份额领先的运营商;生效的按钮保持高亮。
- 一键还原,可单卡或全部;真值在运行时重导,不保存任何快照。
- 可选的按应用区域覆盖:选中的应用经 per-app locale 跟随主卡槽的伪装归属地,SIM 还原时自动回退(默认关闭)。
- 原始值与当前值并排展示;技术码值(MCC/MNC/ISO/subId)用等宽样式。
- 已覆盖的卡片带倾斜、半透明的面具印章标识。
- 亮 / 暗主题,可独立于系统切换;Material 3,对比度达 WCAG AA。
- 中英文,跟随系统语言,Android 13+ 可按应用单独切换。
- 品牌化启动屏;色盲友好的日志(图标 + 文字)。

## 能改与不能改

Roamer 只编辑 CarrierConfig 覆盖层,**不**触碰 SIM 或 RIL 的真实值。

| 值 | 可覆写 | API |
| --- | --- | --- |
| 国家 ISO | ✅ 能 | `getSimCountryIso` |
| 运营商名 | ✅ 能 | `getSimOperatorName` |
| MCC / MNC 数字 | ❌ 不能 | `getSimOperator`(SELinux 保护) |
| 在网真值 | ❌ 不能 | `getNetworkOperator` / `getNetworkCountryIso` |

因此 Roamer 把 MNC 标为只读,并**绝不承诺**改变真实号码、收发短信、或骗过所有应用。能否骗过某个应用,取决于它读的是哪个 API。

按应用区域覆盖是**另一套机制**(系统的 per-app locale),不属于上面这层 CarrierConfig。它改变的是目标应用经 `Locale`/`Configuration` 读到的东西(国家、格式化、语言)。很多应用的地区来自 IP / GPS / 账号——和 SIM 覆盖一样,是否生效取决于该应用读哪个源。

## 环境要求

- 一台 Android 设备(已在一台运行 Android 16 的三星设备上验证),Android 12+(minSdk 31)。
- 已安装并运行 [Shizuku](https://shizuku.rikka.app/)(通过无线调试或 ADB 启动)。
- 首次启动授予 `READ_PHONE_STATE`(仅用于枚举活跃 SIM;应用未声明 `CALL_PHONE`,无法拨打电话)。
- 按应用区域覆盖需要 Android 13+(系统 per-app locale API)。它声明 `QUERY_ALL_PACKAGES` 仅用于在选择器里列出已装应用;locale 本身经 Shizuku 写入。
- 系统内「按应用单独调整语言」需要 Android 13+。

## 工作原理

**提权路径。** 直接经 Shizuku 调 `overrideConfig` 在部分厂商上会失败:三星专门拒绝 `getCallingUid() == SHELL`,而每次 Shizuku 调用都跑在 shell 身份下。Roamer 改为经 Shizuku 调 `IActivityManager.startInstrumentation` 并带上 `INSTR_FLAG_NO_RESTART`(不 force-stop 目标包、UI 不闪退)。instrumentation 在应用进程内运行,调用 `startDelegateShellPermissionIdentity(myUid, null)` 把 shell 的权限委派给应用自身 uid。随后特权 `overrideConfig` 以应用 uid 身份运行(过「拒 shell」护栏),同时携带 `MODIFY_PHONE_STATE`(过权限校验)。

**无 baseline 还原。** Roamer 不保存任何覆盖前快照,还原时全部现场重导真值:

- 真实国家 ISO 由不可变的 MCC(`getSimOperator`,任何覆盖都污染不了)派生。还原是两步写:先回写真实 ISO(非空值强制订阅库更新),再用 `overrideConfig(null)` 清掉覆盖层。在本机型上,单纯盲清并不会把 ISO 还回去。
- 运营商名无需回写:清层会触发 pull-on-read,自动回退到生产值。
- 「是否已覆盖」现场判定:MCC 派生的真实 ISO 与当前生效的 `getSimCountryIso` 不一致即为已覆盖。

**按应用区域覆盖。** 勾选的应用镜像主卡槽的**覆盖状态**:SIM 处于伪装态时,每个选中应用的 per-app locale 被设为该国默认值(如 `ja-JP`);SIM 还原(或功能关闭)时,每个应用被精确还原到它此前的 locale。Roamer 经 Shizuku 的 `ILocaleManager` 以 shell 身份写入(无需 instrumentation)。它不跑后台服务,所以同步发生在 Roamer 执行 SIM 操作或刷新时——不是常驻后台。

所有反射经 [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass);项目不依赖任何隐藏 API 编译桩即可构建。

## 构建

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug        # 构建 debug APK
./gradlew :app:testDebugUnitTest    # 运行 JVM 单测
```

## 项目结构

```
com.eigenlux.roamer/
├── core/   # 唯一碰隐藏 API / 特权的地方
│   ├── ShizukuManager · SimInfo · MccUtil(MCC→ISO 真源)
│   ├── CarrierConfigController            # 枚举 + 触发 + 轮询确认;N/M 批量结果
│   ├── InstrumentationTrigger            # Shizuku → startInstrumentation + NO_RESTART
│   ├── PrivilegedOverrideInstrumentation # shell 委派 + 无 baseline 两步还原
│   ├── PrivilegedSubscriptionReader      # binder 直读 ISub(只读 ICCID 展示)
│   ├── RegionLogic                       # 按应用区域覆盖的纯决策逻辑(可 JVM 单测)
│   └── LocaleOverrideController          # 经 Shizuku ILocaleManager 读/写/清 per-app locale
├── data/   # 纯静态预设 + 本地状态(CountryPresets 27 地区 / CarrierPresets 按 ISO 联动 / AppLocaleStore)
├── AppPickerScreen.kt                    # 区域覆盖的全屏应用选择器
└── ui/     # Compose(Material 3 主题,单屏 MainActivity)
```

## 国际化

UI 文案位于 `res/values`(英文,默认)与 `res/values-zh`(中文)。任何非中文系统语言都回落英文。`res/xml/locales_config.xml` 在 Android 13+ 上启用「按应用调整语言」选择器。JVM 测试(`StringsParityTest`)强制两套字符串表保持同步。

## 安全

Roamer 是面向自己设备的调试工具,用完能让设备回到干净状态:由于还原会在运行时重导每一个值,测试后总能把 SIM 还回它真实的国家与运营商。

## 已知限制

- 仅在一台运行 Android 16 的三星设备上验证。其他厂商与 Android 版本为 best-effort:提权路径针对三星的 reject-shell 行为。
- 若操作中途 SIM 失去服务(拔卡或飞行模式),还原可能报告成功,但设备实际仍残留覆盖。

## 许可证

以 [MIT License](LICENSE) 发布。

## 致谢

Roamer 受两个项目启发,它们率先探索了无 root、基于 Shizuku 的运营商 / 地区伪装方案:

- [**Ackites/Nrfr**](https://github.com/Ackites/Nrfr)
- [**lmh-codes/Nrfr**](https://github.com/lmh-codes/Nrfr)

Roamer 的重心在界面:为一件以往多以简陋工具形态出现的事,做一套精致而诚实的 UI/UX。技术层面它也贡献了两步 ISO 回写还原(早期 fork 的盲 `overrideConfig(null)` 在本机型上还不回 ISO)以及完全无 baseline、运行时重导的还原模型。感谢这两个项目奠定的基础。
