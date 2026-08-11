# 钉钉定时 (DDTask)

Kotlin 原生 Android 应用，支持 **Android 6.0（API 23）** 及以上。可设置定时任务，到点自动打开 **钉钉**。

## 功能

- 添加定时任务，选择触发时间（24 小时制）
- 支持「每天重复」或「仅一次」
- 可选备注标签
- 开关启用/禁用单个任务
- 开机后自动恢复所有已启用任务
- 检测钉钉是否已安装

## 构建与安装

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK（compileSdk 34）

### 步骤

1. 用 Android Studio 打开本项目目录 `/home/fly/ddtask`
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器（API 23+）
4. 点击 Run 或执行：

```bash
./gradlew assembleDebug
```

生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

## 使用说明

1. 首次打开应用，确认顶部显示「钉钉：已安装」
2. 点击右下角 **+** 添加定时任务
3. 选择时间，可选填备注，选择是否每天重复
4. 保存后任务会在指定时间自动打开钉钉

### Android 12+ 权限

Android 12 及以上需要在系统设置中授予 **「闹钟和提醒」**（精确闹钟）权限，应用首次启动时会提示跳转设置。

### 省电与后台限制

部分厂商（小米、华为、OPPO 等）会限制后台闹钟。若任务未准时触发，请在系统设置中：

- 允许本应用 **自启动**
- 关闭本应用的 **电池优化**
- 允许 **后台运行**

## 技术说明

| 项目 | 说明 |
|------|------|
| minSdk | 23（Android 6.0） |
| targetSdk | 34 |
| 钉钉包名 | `com.alibaba.android.rimet` |
| 定时机制 | `AlarmManager.setExactAndAllowWhileIdle` |
| 数据存储 | SharedPreferences + Gson |

## 项目结构

```
app/src/main/java/com/ddtask/scheduler/
├── MainActivity.kt          # 主界面
├── model/ScheduledTask.kt   # 任务数据模型
├── service/AlarmScheduler.kt # 闹钟调度
├── receiver/
│   ├── AlarmReceiver.kt     # 到点触发，打开钉钉
│   └── BootReceiver.kt      # 开机恢复任务
└── util/
    ├── TaskStorage.kt       # 任务持久化
    └── DingTalkLauncher.kt  # 钉钉包名常量
```

## 许可证

MIT
