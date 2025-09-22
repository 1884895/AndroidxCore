# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 通用指导原则

**始终使用中文与用户交流**
**始终同步项目任何改动**
**代码不要重复生成，优化改进基于已生成的代码基础上改进，做到同步兼容**

## 项目概述

一个用于学生学习和考试管理的 Android 教育应用。应用通过二维码扫描进行设备绑定，提供作业、练习题、错题本等功能。

## 构建命令

```bash
# 构建调试版 APK
./gradlew assembleDebug

# 构建所有渠道的发布版 APK
./gradlew assembleRelease

# 构建特定渠道版本（如华为）
./gradlew assembleHuaweiDebug
./gradlew assembleHuaweiRelease

# 清理构建
./gradlew clean

# 在连接的设备上安装调试版
./gradlew installDebug

# 运行代码检查
./gradlew lint

# 运行单元测试
./gradlew test

# 运行仪器化测试
./gradlew connectedAndroidTest
```

## 开发注意事项

- 应用目标 minSdk 28，compileSdk 35
- 所有构建启用 Multi-dex
- 配置了小米和华为设备的刘海屏支持
- WebView JavaScript 接口名称："HFS"
- Cookie 持久化使用 SharedPreferences，键名为 "CookiePrefsFile_V1"