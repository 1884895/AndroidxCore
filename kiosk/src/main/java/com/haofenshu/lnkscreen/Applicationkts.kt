package com.haofenshu.lnkscreen

import android.app.Activity
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.DEVICE_POLICY_SERVICE
import android.util.Log


public fun Application.initKioskMode() {
    try {
        Log.d("App", "开始初始化Kiosk模式")

        // 启用降级的入口系统（页面内悬浮按钮，无自动跳转）
        KioskUtils.enableLiteEntrySystem(this)

        // 检查网络状态，但不自动跳转
        val hasNetwork: Boolean = KioskUtils.isNetworkAvailable(this)
        if (!hasNetwork) {
            Log.w("App", "检测到无网络连接")
        }

        // 设置增强的Kiosk模式（包含白名单、状态栏保留、屏蔽恢复出厂设置等）
        val setupSuccess: Boolean = KioskUtils.setupEnhancedKioskMode(this)

        if (setupSuccess) {
            // 确保当前应用可以被更新
            KioskUtils.enableAppUpdate(this, null)
            Log.d("App", "增强Kiosk模式设置成功")
        } else {
            Log.w("App", "增强Kiosk模式设置失败")
        }
    } catch (e: Exception) {
        Log.e("App", "Kiosk模式初始化失败", e)
    }
}

/**
 * 启动锁定任务模式（在Activity中调用）
 * 这个方法供Activity调用来实际启动LockTask
 */
fun Application.startLockTaskIfNeeded(context: Context, applicationId: String) {
    try {
        if (KioskUtils.isDeviceOwner(context)) {
            if (context is Activity) {
                context.startLockTask()
                Log.d("App", "LockTask模式启动成功")
            }
        } else {
            // 如果不是设备所有者，尝试基础模式
            enableBasicLockTaskMode(context, applicationId)
        }
    } catch (e: Exception) {
        Log.e("App", "启动LockTask失败", e)
        enableBasicLockTaskMode(context, applicationId)
    }
}

/**
 * 基础锁定任务模式（备用方案）
 */
fun Application.enableBasicLockTaskMode(context: Context, applicationId: String) {
    try {
        val dpm = context.getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin: ComponentName = ComponentName(context, MyDeviceAdminReceiver::class.java)

        // 设置允许在LockTask模式中运行的应用白名单（仅当前应用）
        dpm.setLockTaskPackages(admin, arrayOf<String?>(applicationId))

        // 检查是否允许进入LockTask模式，如果允许则启动
        if (dpm.isLockTaskPermitted(applicationId)) {
            if (context is Activity) {
                context.startLockTask()
                Log.d("App", "基础LockTask模式启动成功")
            }
        }
    } catch (e: Exception) {
        Log.e("App", "基础LockTask模式启动失败", e)
    }
}