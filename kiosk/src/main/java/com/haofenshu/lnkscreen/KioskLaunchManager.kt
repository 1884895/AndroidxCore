package com.haofenshu.lnkscreen

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Kiosk通用启动管理器
 * 使用通用方案启动应用，避免硬编码包名和Activity
 */
object KioskLaunchManager {
    private const val TAG = "KioskLaunchManager"

    /**
     * 启动应用
     * 使用PackageManager获取并启动当前应用的默认Activity
     */
    fun launchApp(context: Context): Boolean {
        Log.d(TAG, "开始启动应用")
        return launchCurrentApp(context)
    }

    /**
     * 启动当前应用的默认Activity
     */
    private fun launchCurrentApp(context: Context): Boolean {
        return try {
            val packageName = context.packageName
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(launchIntent)
                Log.d(TAG, "启动当前应用: $packageName")
                return true
            }

            Log.e(TAG, "无法获取当前应用的启动Intent")
            false
        } catch (e: Exception) {
            Log.e(TAG, "启动当前应用失败", e)
            false
        }
    }
}