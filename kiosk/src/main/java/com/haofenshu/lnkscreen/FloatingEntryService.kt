package com.haofenshu.lnkscreen

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log

class FloatingEntryService : Service() {

    companion object {
        private const val TAG = "FloatingEntryService"
        private const val NOTIFICATION_ID = 9527
        private const val CHANNEL_ID = "floating_entry_channel"
        private const val CHANNEL_NAME = "系统设置入口"

        fun start(context: Context) {
            val intent = Intent(context, FloatingEntryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingEntryService::class.java)
            context.stopService(intent)
        }
    }

    private var isFloatingWindowShowing = false
    private var hasOverlayPermission = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingEntryService 创建")

        // 初始化悬浮窗管理器
        FloatingWindowManager.initialize(this)

        // 检查悬浮窗权限
        checkOverlayPermission()

        // 创建通知渠道
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingEntryService 启动")

        // 启动前台服务
        startForegroundService()

        // 根据权限情况显示入口
        showAppropriateEntry()

        return START_STICKY // 服务被杀死后尝试重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingEntryService 销毁")

        // 隐藏悬浮窗
        hideFloatingWindow()

        // 清理悬浮窗管理器
        FloatingWindowManager.onDestroy()
    }

    private fun checkOverlayPermission() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // Android 6.0以下默认有权限
        }

        Log.d(TAG, "悬浮窗权限检查: $hasOverlayPermission")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提供系统设置的快速入口"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "前台服务启动完成")
    }

    private fun createNotification(): Notification {
        // 点击通知的意图
        val settingsIntent = Intent(this, SystemSettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            settingsIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // 构建通知
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("系统设置管理")
            .setContentText("点击打开系统设置页面")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 设置为持续通知，不能被滑动删除
            .setAutoCancel(false) // 点击后不自动取消
            .setPriority(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationManager.IMPORTANCE_LOW
            } else {
                @Suppress("DEPRECATION")
                Notification.PRIORITY_LOW
            })
            .build()
    }

    private fun showAppropriateEntry() {
        Log.d(TAG, "显示适当入口，悬浮窗权限: $hasOverlayPermission")

        if (hasOverlayPermission) {
            // 有悬浮窗权限，显示悬浮按钮
            showFloatingWindow()
            // 更新通知为辅助提示
            updateNotificationForFloatingMode()
        } else {
            // 没有悬浮窗权限，通知作为主要入口
            Log.w(TAG, "无悬浮窗权限，使用通知作为主要入口")
            updateNotificationForMainMode()
        }
    }

    private fun showFloatingWindow() {
        Log.d(TAG, "尝试显示悬浮窗，当前状态: isShowing=$isFloatingWindowShowing, hasPermission=$hasOverlayPermission")

        if (!isFloatingWindowShowing && hasOverlayPermission) {
            Log.d(TAG, "开始创建悬浮按钮")
            val success = FloatingWindowManager.showFloatingButton {
                // 悬浮按钮点击事件
                Log.d(TAG, "悬浮按钮被点击，打开系统设置")
                openSystemSettings()
            }

            if (success) {
                isFloatingWindowShowing = true
                Log.d(TAG, "悬浮窗显示成功")
            } else {
                Log.e(TAG, "悬浮窗显示失败，可能权限被撤销或其他原因")
                // 显示失败时重新检查权限状态
                checkOverlayPermission()
                if (!hasOverlayPermission) {
                    Log.w(TAG, "检测到权限已被撤销，切换到通知模式")
                    updateNotificationForMainMode()
                }
            }
        } else if (isFloatingWindowShowing) {
            Log.d(TAG, "悬浮窗已经在显示中")
        } else {
            Log.w(TAG, "无悬浮窗权限，跳过悬浮窗显示")
        }
    }

    private fun hideFloatingWindow() {
        if (isFloatingWindowShowing) {
            FloatingWindowManager.hideFloatingButton()
            isFloatingWindowShowing = false
            Log.d(TAG, "悬浮窗已隐藏")
        }
    }

    private fun updateNotificationForFloatingMode() {
        // 悬浮窗模式下，通知作为辅助入口
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationForMainMode() {
        // 通知作为主要入口时，可以添加更多操作按钮
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        // 主设置按钮
        val settingsIntent = Intent(this, SystemSettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            this,
            1,
            settingsIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // WiFi设置快捷按钮
        val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val wifiPendingIntent = PendingIntent.getActivity(
            this,
            2,
            wifiIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = builder
            .setContentTitle("系统设置管理")
            .setContentText("无悬浮窗权限，使用通知入口")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(settingsPendingIntent)
            .addAction(android.R.drawable.ic_menu_preferences, "系统设置", settingsPendingIntent)
            .addAction(android.R.drawable.stat_sys_data_bluetooth, "WiFi设置", wifiPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationManager.IMPORTANCE_DEFAULT
            } else {
                @Suppress("DEPRECATION")
                Notification.PRIORITY_DEFAULT
            })
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d(TAG, "通知已更新为主要入口模式")
    }

    private fun openSystemSettings() {
        try {
            val intent = Intent(this, SystemSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            Log.d(TAG, "系统设置页面已打开")
        } catch (e: Exception) {
            Log.e(TAG, "打开系统设置页面失败", e)
        }
    }

    fun refreshEntry() {
        // 重新检查权限并更新入口
        checkOverlayPermission()

        if (hasOverlayPermission && !isFloatingWindowShowing) {
            showFloatingWindow()
            updateNotificationForFloatingMode()
        } else if (!hasOverlayPermission && isFloatingWindowShowing) {
            hideFloatingWindow()
            updateNotificationForMainMode()
        }
    }

    fun showRedDotIndicator() {
        if (isFloatingWindowShowing) {
            FloatingWindowManager.showRedDot()
        }
        // 通知不需要红点，因为它本身就是提醒
    }

    fun hideRedDotIndicator() {
        if (isFloatingWindowShowing) {
            FloatingWindowManager.hideRedDot()
        }
    }

    fun isShowingRedDot(): Boolean {
        return if (isFloatingWindowShowing) {
            FloatingWindowManager.isShowingRedDot()
        } else {
            false
        }
    }

    fun getEntryStatus(): String {
        return buildString {
            append("双重入口服务状态:\n")
            append("- 服务运行: 是\n")
            append("- 悬浮窗权限: ${if (hasOverlayPermission) "已授权" else "未授权"}\n")
            append("- 悬浮窗显示: ${if (isFloatingWindowShowing) "是" else "否"}\n")
            append("- 通知状态: 运行中\n")
            append("- 主要入口: ${if (hasOverlayPermission) "悬浮窗" else "通知"}\n")
            if (isFloatingWindowShowing) {
                append("- 悬浮窗位置: ${FloatingWindowManager.getPositionInfo()}\n")
            }
        }
    }
}