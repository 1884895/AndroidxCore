package com.haofenshu.lnkscreen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SystemSettingsActivity : AppCompatActivity() {

    private lateinit var wifiStatusText: TextView
    private lateinit var brightnessButton: Button
    private var systemSettingsButton: Button? = null
    private lateinit var soundSettingsButton: Button
    private lateinit var dateTimeButton: Button
    private lateinit var statusText: TextView
    private lateinit var backButton: Button
    private lateinit var versionText: TextView

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
        private const val REQUEST_WRITE_PERMISSION = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#FDEAC5")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        setContentView(R.layout.activity_system_settings)

        initViews()
        initServices()
        setupClickListeners()
        checkDebugMode()
        updateNetworkStatus()
    }

    private fun initViews() {
        wifiStatusText = findViewById(R.id.wifiStatusText)
        brightnessButton = findViewById(R.id.brightnessButton)
        soundSettingsButton = findViewById(R.id.soundSettingsButton)
        dateTimeButton = findViewById(R.id.dateTimeButton)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)
        versionText = findViewById(R.id.versionText)

        // 设置版本号
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "版本号: ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "版本号: 未知"
        }
    }

    private fun initServices() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun setupClickListeners() {
        // 返回按钮
        backButton.setOnClickListener {
            finish()
        }

        // 基本功能按钮
        brightnessButton.setOnClickListener { openBrightnessSettings() }
        soundSettingsButton.setOnClickListener { openSoundSettings() }
        dateTimeButton.setOnClickListener { openDateTimeSettings() }

        // 网络检测按钮
        findViewById<Button>(R.id.networkCheckButton).setOnClickListener {
            performNetworkCheck()
        }

        // 电源管理按钮
        findViewById<Button>(R.id.shutdownButton).setOnClickListener {
            showShutdownDialog()
        }
        findViewById<Button>(R.id.rebootButton).setOnClickListener {
            showRebootDialog()
        }
    }

    private fun checkDebugMode() {
        val applicationInfo = applicationInfo
        val isDebug =
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebug) {
            // 显示Debug按钮组
            findViewById<Button>(R.id.networkSettingsButton).apply {
                visibility = View.VISIBLE
                setOnClickListener { openNetworkSettings() }
            }

            findViewById<LinearLayout>(R.id.debugButtonsLayout).apply {
                visibility = View.VISIBLE

                // 设置Debug按钮点击事件
                findViewById<Button>(R.id.systemSettingsButton).setOnClickListener {
                    openSystemSettings()
                }
                findViewById<Button>(R.id.developerButton).setOnClickListener {
                    openDeveloperOptions()
                }
                findViewById<Button>(R.id.applicationButton).setOnClickListener {
                    openApplicationSettings()
                }
            }

            findViewById<LinearLayout>(R.id.honorAppsLayout).apply {
                visibility = View.VISIBLE

                // 设置Honor应用按钮点击事件
                findViewById<Button>(R.id.floatingWindowDebugButton).setOnClickListener {
                    debugFloatingWindow()
                }
                findViewById<Button>(R.id.cameraButton).setOnClickListener {
                    openHonorCameraWithPermission()
                }
                findViewById<Button>(R.id.galleryButton).setOnClickListener {
                    openHonorGalleryWithPermission()
                }
                findViewById<Button>(R.id.fileManagerButton).setOnClickListener {
                    openHonorFileManagerWithPermission()
                }
                findViewById<Button>(R.id.dockBarButton).setOnClickListener {
                    handleDockBarBlocking()
                }
                findViewById<Button>(R.id.resetSettingsButton).setOnClickListener {
                    handleResetSettingsBlocking()
                }
                findViewById<Button>(R.id.exitKioskButton).setOnClickListener {
                    handleExitKioskMode()
                }
            }

            systemSettingsButton = findViewById(R.id.systemSettingsButton)
        }
    }

    private fun updateNetworkStatus() {
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo?.isConnected == true
        val isWifi = networkInfo?.type == ConnectivityManager.TYPE_WIFI

        when {
            isConnected && isWifi -> {
                wifiStatusText.text = "✓ WiFi已连接"
                wifiStatusText.setTextColor(Color.parseColor("#4CAF50"))
            }

            isConnected -> {
                wifiStatusText.text = "✓ 已连接到网络"
                wifiStatusText.setTextColor(Color.parseColor("#4CAF50"))
            }

            wifiManager.isWifiEnabled -> {
                wifiStatusText.text = "WiFi已开启，但未连接网络"
                wifiStatusText.setTextColor(Color.parseColor("#FF9800"))
            }

            else -> {
                wifiStatusText.text = "✗ 无网络连接"
                wifiStatusText.setTextColor(Color.parseColor("#F44336"))
            }
        }
    }

    private fun performNetworkCheck() {
        val networkInfo = KioskUtils.getNetworkStatusInfo(this)
        showStatus("网络状态检查完成")
        android.app.AlertDialog.Builder(this)
            .setTitle("网络检测")
            .setMessage("当前无网络连接\n\n$networkInfo")
            .setPositiveButton("打开WiFi设置") { _, _ ->
                openWifiSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun debugFloatingWindow() {
        val debugInfo = KioskUtils.debugFloatingWindow(this)
        android.app.AlertDialog.Builder(this)
            .setTitle("悬浮窗调试信息")
            .setMessage(debugInfo)
            .setPositiveButton("测试悬浮窗") { _, _ ->
                val success = KioskUtils.testFloatingWindow(this)
                showStatus(if (success) "悬浮窗测试成功" else "悬浮窗测试失败")
            }
            .setNegativeButton("申请权限") { _, _ ->
                KioskUtils.requestOverlayPermission(this)
            }
            .setNeutralButton("关闭", null)
            .show()
    }

    private fun handleDockBarBlocking() {
        val isBlocked = KioskUtils.isHonorDockBarBlocked(this)
        val statusText = if (isBlocked) "已屏蔽" else "未屏蔽"

        android.app.AlertDialog.Builder(this)
            .setTitle("Honor侧滑悬浮入口状态")
            .setMessage("当前状态: $statusText\n\n需要重新应用Kiosk模式设置才能屏蔽Honor侧滑悬浮入口")
            .setPositiveButton("重新应用设置") { _, _ ->
                val success = KioskUtils.setupEnhancedKioskMode(this)
                if (success) {
                    showStatus("Honor侧滑悬浮入口屏蔽设置已应用")
                    val newStatus = KioskUtils.isHonorDockBarBlocked(this)
                    val newStatusText = if (newStatus) "已屏蔽" else "仍未屏蔽"
                    showStatus("屏蔽状态: $newStatusText")
                } else {
                    showStatus("屏蔽设置失败，请检查设备管理员权限")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun handleResetSettingsBlocking() {
        val isBlocked = KioskUtils.isHonorResetSettingsBlocked(this)
        val statusText = if (isBlocked) "已屏蔽" else "未屏蔽"

        android.app.AlertDialog.Builder(this)
            .setTitle("Honor重置菜单项屏蔽状态")
            .setMessage("当前状态: $statusText\n\n目标: 系统设置应用内的重置菜单项\n(SubSettings页面)\n\n需要重新应用Kiosk模式设置才能屏蔽重置菜单项")
            .setPositiveButton("重新应用设置") { _, _ ->
                val success = KioskUtils.setupEnhancedKioskMode(this)
                if (success) {
                    showStatus("Honor重置菜单项屏蔽设置已应用")
                    val newStatus = KioskUtils.isHonorResetSettingsBlocked(this)
                    val newStatusText = if (newStatus) "已屏蔽" else "仍未屏蔽"
                    showStatus("重置菜单项屏蔽状态: $newStatusText")
                } else {
                    showStatus("屏蔽设置失败，请检查设备管理员权限")
                }
            }
            .setNeutralButton("打开系统设置测试") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    showStatus("已打开系统设置，请手动检查重置选项是否被隐藏")
                } catch (e: Exception) {
                    showStatus("无法打开系统设置: ${e.message}")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun handleExitKioskMode() {
        android.app.AlertDialog.Builder(this)
            .setTitle("退出单应用模式")
            .setMessage("确定要退出单应用模式吗？这将清除所有Kiosk设置。")
            .setPositiveButton("确定") { _, _ ->
                val success = KioskUtils.exitKioskModeInDebug(this)
                if (success) {
                    showStatus("已退出单应用模式")
                    postDelayed({
                        finish()
                    }, 1000)
                } else {
                    showStatus("退出失败，请检查权限")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 原有的功能方法保持不变
    private fun openNetworkSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_WIRELESS_SETTINGS)
    }

    private fun openWifiSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_WIFI_SETTINGS)
    }

    private fun openBrightnessSettings() {
        // 自动清除亮度限制，兼容旧版本
        try {
            if (KioskUtils.isBrightnessRestricted(this)) {
                // 静默清除限制，不显示提示
                KioskUtils.clearBrightnessRestriction(this)
            }
        } catch (e: Exception) {
            // 忽略错误，继续打开设置
        }

        // 打开亮度设置页面
        KioskUtils.openSystemSettings(this, Settings.ACTION_DISPLAY_SETTINGS)
    }

    private fun openSoundSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_SOUND_SETTINGS)
    }

    private fun openDateTimeSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_DATE_SETTINGS)
    }

    private fun openSystemSettings() {
        KioskUtils.openSystemSettings(this)
    }

    private fun openDeveloperOptions() {
        KioskUtils.openDeveloperOptions(this)
    }

    private fun openApplicationSettings() {
        KioskUtils.openApplicationSettings(this)
    }

    private fun showStatus(message: String) {
        statusText.text = message
        statusText.postDelayed({
            statusText.text = ""
        }, 3000)
    }

    private fun openHonorCameraWithPermission() {
        if (checkCameraPermission()) {
            openHonorCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    private fun openHonorCamera() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.hihonor.camera")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                showStatus("正在打开Honor相机")
            } else {
                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (cameraIntent.resolveActivity(packageManager) != null) {
                    startActivity(cameraIntent)
                    showStatus("正在打开系统相机")
                } else {
                    showStatus("未找到可用的相机应用")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SystemSettingsActivity", "打开Honor相机失败", e)
            showStatus("打开相机失败: ${e.message}")
        }
    }

    private fun openHonorGalleryWithPermission() {
        try {
            openHonorGallery()
        } catch (e: Exception) {
            if (checkStoragePermission()) {
                openHonorGallery()
            } else {
                showPermissionExplanationForGallery()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                this,
                "android.permission.READ_MEDIA_IMAGES"
            ) == PackageManager.PERMISSION_GRANTED
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showPermissionExplanationForGallery() {
        android.app.AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("打开相册需要读取存储权限，是否授权？")
            .setPositiveButton("授权") { _, _ ->
                requestStoragePermission()
            }
            .setNegativeButton("取消") { _, _ ->
                showStatus("用户取消授权，无法打开相册")
            }
            .setNeutralButton("直接尝试") { _, _ ->
                openHonorGallery()
            }
            .show()
    }

    private fun requestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            "android.permission.READ_MEDIA_IMAGES"
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        try {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                REQUEST_STORAGE_PERMISSION
            )
        } catch (e: Exception) {
            android.util.Log.e("SystemSettingsActivity", "请求权限失败", e)
            showStatus("权限请求失败，尝试直接打开相册")
            openHonorGallery()
        }
    }

    private fun openHonorGallery() {
        var opened = false

        try {
            val honorIntent = packageManager.getLaunchIntentForPackage("com.hihonor.photos")
            if (honorIntent != null) {
                honorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(honorIntent)
                showStatus("正在打开Honor相册")
                opened = true
            }
        } catch (e: Exception) {
            android.util.Log.w("SystemSettingsActivity", "Honor相册启动失败", e)
        }

        if (!opened) {
            try {
                val galleryIntent = Intent(Intent.ACTION_PICK).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (galleryIntent.resolveActivity(packageManager) != null) {
                    startActivity(galleryIntent)
                    showStatus("正在打开图库选择器")
                    opened = true
                }
            } catch (e: Exception) {
                android.util.Log.w("SystemSettingsActivity", "图库选择器启动失败", e)
            }
        }

        if (!opened) {
            showStatus("未找到可用的图库应用，请安装相册应用")
        }
    }

    private fun openHonorFileManagerWithPermission() {
        if (checkWritePermission()) {
            openHonorFileManager()
        } else {
            requestWritePermission()
        }
    }

    private fun checkWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED ||
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    }

    private fun requestWritePermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_PERMISSION
            )
        } else {
            openHonorFileManager()
        }
    }

    private fun openHonorFileManager() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.hihonor.filemanager")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                showStatus("正在打开Honor文件管理")
            } else {
                val fileIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fileIntent.resolveActivity(packageManager) != null) {
                    startActivity(fileIntent)
                    showStatus("正在打开系统文件管理器")
                } else {
                    showStatus("未找到可用的文件管理器")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SystemSettingsActivity", "打开Honor文件管理失败", e)
            showStatus("打开文件管理器失败: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openHonorCamera()
                } else {
                    showStatus("相机权限被拒绝，无法打开相机")
                }
            }

            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openHonorGallery()
                } else {
                    showStatus("存储权限被拒绝，无法打开相册")
                }
            }

            REQUEST_WRITE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openHonorFileManager()
                } else {
                    showStatus("写入权限被拒绝，但仍可尝试打开文件管理器")
                    openHonorFileManager()
                }
            }
        }
    }

    private fun showShutdownDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("确认关机")
            .setMessage("您确定要关闭设备吗？")
            .setPositiveButton("确定关机") { _, _ ->
                performShutdown()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRebootDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("确认重启")
            .setMessage("您确定要重启设备吗？")
            .setPositiveButton("确定重启") { _, _ ->
                performReboot()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performShutdown() {
        if (KioskUtils.canShutdownOrReboot(this)) {
            showStatus("正在执行关机...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val success = KioskUtils.shutdownDevice(this)
                if (!success) {
                    showStatus("关机失败，可能需要系统权限")
                    android.app.AlertDialog.Builder(this)
                        .setTitle("关机失败")
                        .setMessage("无法直接关机。您可以：\n1. 长按电源键手动关机\n2. 联系管理员获取权限")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }, 1500)
        } else {
            showStatus("没有关机权限")
            android.app.AlertDialog.Builder(this)
                .setTitle("权限不足")
                .setMessage("当前应用没有关机权限。\n\n可能的原因：\n- 未设置为设备所有者\n- Android版本限制\n\n请长按电源键手动关机。")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun performReboot() {
        if (KioskUtils.canShutdownOrReboot(this)) {
            showStatus("正在执行重启...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val success = KioskUtils.rebootDevice(this)
                if (!success) {
                    showStatus("重启失败，可能需要系统权限")
                    android.app.AlertDialog.Builder(this)
                        .setTitle("重启失败")
                        .setMessage("无法直接重启。您可以：\n1. 长按电源键手动重启\n2. 联系管理员获取权限")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }, 1500)
        } else {
            showStatus("没有重启权限")
            android.app.AlertDialog.Builder(this)
                .setTitle("权限不足")
                .setMessage("当前应用没有重启权限。\n\n可能的原因：\n- 未设置为设备所有者\n- Android版本限制\n\n请长按电源键手动重启。")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun postDelayed(action: () -> Unit, delayMillis: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(action, delayMillis)
    }

    override fun onResume() {
        super.onResume()
        updateNetworkStatus()
    }
}