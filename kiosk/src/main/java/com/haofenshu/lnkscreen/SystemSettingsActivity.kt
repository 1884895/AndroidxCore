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
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SystemSettingsActivity : AppCompatActivity() {

    private lateinit var wifiStatusText: TextView
    private lateinit var brightnessButton: Button
    private var systemSettingsButton: Button? = null // 可能为null，因为release模式下不显示
    private lateinit var soundSettingsButton: Button
    private lateinit var dateTimeButton: Button
    private lateinit var statusText: TextView

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

        setContentView(createContentView())

        initViews()
        initServices()
        updateNetworkStatus()
    }

    private fun createContentView(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#FDEAC5"))
        }

        // 标题
        val titleText = TextView(this).apply {
            text = "📱 系统设置管理"
            textSize = 28f
            setTextColor(Color.parseColor("#8B4513"))
            setPadding(16, 8, 16, 30)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(titleText)

        // 网络设置区域
        val networkSection = createNetworkSection()
        rootLayout.addView(networkSection)

        // 分割线
        val divider1 = View(this).apply {
            setBackgroundColor(Color.parseColor("#D2B48C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4
            ).apply { setMargins(32, 16, 32, 16) }
        }
        rootLayout.addView(divider1)

        // 显示设置区域
        val displaySection = createDisplaySection()
        rootLayout.addView(displaySection)

        // 分割线
        val divider2 = View(this).apply {
            setBackgroundColor(Color.parseColor("#D2B48C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4
            ).apply { setMargins(32, 16, 32, 16) }
        }
        rootLayout.addView(divider2)

        // 音频和时间设置区域
        val audioTimeSection = createAudioTimeSection()
        rootLayout.addView(audioTimeSection)

        // 分割线
        val divider3 = View(this).apply {
            setBackgroundColor(Color.parseColor("#D2B48C"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                4
            ).apply { setMargins(32, 16, 32, 16) }
        }
        rootLayout.addView(divider3)

        // 系统设置区域
        val systemSection = createSystemSection()
        rootLayout.addView(systemSection)

        // 状态显示
        statusText = TextView(this).apply {
            text = ""
            textSize = 16f
            setTextColor(Color.parseColor("#8B4513"))
            setPadding(16, 20, 16, 0)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(statusText)

        // 网络状态详情
        val networkInfoText = TextView(this).apply {
            text = KioskUtils.getNetworkStatusInfo(this@SystemSettingsActivity)
            textSize = 14f
            setTextColor(Color.parseColor("#A0522D"))
            setPadding(16, 16, 16, 0)
            gravity = android.view.Gravity.CENTER
        }
        rootLayout.addView(networkInfoText)

        val scrollView = ScrollView(this)
        scrollView.addView(rootLayout)
        return scrollView
    }

    private fun createNetworkSection(): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sectionTitle = TextView(this).apply {
            text = "网络设置"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 10)
        }
        section.addView(sectionTitle)

        wifiStatusText = TextView(this).apply {
            text = "检查网络状态中..."
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 10, 0, 10)
        }
        section.addView(wifiStatusText)

        // 检查是否为debug模式
        val applicationInfo = applicationInfo
        val isDebug =
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // 只在debug模式下显示网络设置按钮
        if (isDebug) {
            val networkSettingsButton = Button(this).apply {
                text = "网络设置页面(Debug)"
                setOnClickListener { openNetworkSettings() }
            }
            section.addView(networkSettingsButton)
        }

        return section
    }

    private fun createDisplaySection(): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sectionTitle = TextView(this).apply {
            text = "显示设置"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 10)
        }
        section.addView(sectionTitle)

        brightnessButton = Button(this).apply {
            text = "屏幕亮度调节"
            setOnClickListener { openBrightnessSettings() }
        }
        section.addView(brightnessButton)

        return section
    }

    private fun createAudioTimeSection(): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sectionTitle = TextView(this).apply {
            text = "音频和时间"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 10)
        }
        section.addView(sectionTitle)

        soundSettingsButton = Button(this).apply {
            text = "声音设置"
            setOnClickListener { openSoundSettings() }
        }
        section.addView(soundSettingsButton)

        dateTimeButton = Button(this).apply {
            text = "日期和时间设置"
            setOnClickListener { openDateTimeSettings() }
        }
        section.addView(dateTimeButton)

        return section
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSystemSection(): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sectionTitle = TextView(this).apply {
            text = "系统设置"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, 10)
        }
        section.addView(sectionTitle)

        // 检查是否为debug模式
        val applicationInfo = applicationInfo
        val isDebug =
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        /* Release模式下只显示基础功能：
         * - 网络检测
         * - 关机/重启
         * Debug模式下额外显示：
         * - 系统设置页面、开发者选项、应用管理
         * - Honor应用快捷启动
         * - 悬浮窗调试、Kiosk模式调试
         * - 退出单应用模式
         */

        // 只在debug模式下显示高级功能按钮
        if (isDebug) {
            systemSettingsButton = Button(this).apply {
                text = "系统设置页面(Debug)"
                setOnClickListener { openSystemSettings() }
            }
            section.addView(systemSettingsButton)

            val developerButton = Button(this).apply {
                text = "开发者选项(Debug)"
                setOnClickListener { openDeveloperOptions() }
            }
            section.addView(developerButton)

            val applicationButton = Button(this).apply {
                text = "应用管理(Debug)"
                setOnClickListener { openApplicationSettings() }
            }
            section.addView(applicationButton)
        }

        val networkCheckButton = Button(this).apply {
            text = "网络检测和设置"
            setOnClickListener {
                val networkInfo = KioskUtils.getNetworkStatusInfo(this@SystemSettingsActivity)
                showStatus("网络状态检查完成")

                android.app.AlertDialog.Builder(this@SystemSettingsActivity)
                    .setTitle("网络检测和设置")
                    .setMessage("当前无网络连接\n\n$networkInfo")
                    .setPositiveButton("打开WiFi设置") { _, _ ->
                        openWifiSettings()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
        section.addView(networkCheckButton)

        // 添加电源管理功能（关机/重启）- Release模式下也显示
        val powerManagementLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 10, 0, 10)
            }
        }

        // 关机按钮
        val shutdownButton = Button(this).apply {
            text = "关机"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, 5, 0)
            }
            setBackgroundColor(0xFFFF5252.toInt()) // 红色背景
            setTextColor(0xFFFFFFFF.toInt()) // 白色文字
            setOnClickListener {
                showShutdownDialog()
            }
        }
        powerManagementLayout.addView(shutdownButton)

        // 重启按钮
        val rebootButton = Button(this).apply {
            text = "重启"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(5, 0, 0, 0)
            }
            setBackgroundColor(0xFFFF9800.toInt()) // 橙色背景
            setTextColor(0xFFFFFFFF.toInt()) // 白色文字
            setOnClickListener {
                showRebootDialog()
            }
        }
        powerManagementLayout.addView(rebootButton)

        section.addView(powerManagementLayout)

        // 只在debug模式下显示悬浮窗调试按钮
        if (isDebug) {
            val floatingWindowDebugButton = Button(this).apply {
                text = "悬浮窗调试(Debug)"
                setOnClickListener {
                    val debugInfo = KioskUtils.debugFloatingWindow(this@SystemSettingsActivity)
                    android.app.AlertDialog.Builder(this@SystemSettingsActivity)
                        .setTitle("悬浮窗调试信息")
                        .setMessage(debugInfo)
                        .setPositiveButton("测试悬浮窗") { _, _ ->
                            val success = KioskUtils.testFloatingWindow(this@SystemSettingsActivity)
                            showStatus(if (success) "悬浮窗测试成功" else "悬浮窗测试失败")
                        }
                        .setNegativeButton("申请权限") { _, _ ->
                            KioskUtils.requestOverlayPermission(this@SystemSettingsActivity)
                        }
                        .setNeutralButton("关闭", null)
                        .show()
                }
            }
            section.addView(floatingWindowDebugButton)
        }

        // 只在debug模式下显示Honor应用按钮
        if (isDebug) {
            // 添加相机设置按钮
            val cameraButton = Button(this).apply {
                text = "打开Honor相机(Debug)"
                setOnClickListener { openHonorCameraWithPermission() }
            }
            section.addView(cameraButton)

            // 添加图库设置按钮
            val galleryButton = Button(this).apply {
                text = "打开Honor相册(Debug)"
                setOnClickListener { openHonorGalleryWithPermission() }
            }
            section.addView(galleryButton)

            // 添加文件管理器按钮
            val fileManagerButton = Button(this).apply {
                text = "打开Honor文件管理(Debug)"
                setOnClickListener { openHonorFileManagerWithPermission() }
            }
            section.addView(fileManagerButton)
        }

        // Debug模式下添加Honor侧滑悬浮入口屏蔽按钮
        if (isDebug) {
            val dockBarButton = Button(this).apply {
                text = "Honor侧滑悬浮入口屏蔽(Debug)"
                setTextColor(0xFF0066CC.toInt()) // 蓝色文字
                setOnClickListener {
                    val isBlocked = KioskUtils.isHonorDockBarBlocked(this@SystemSettingsActivity)
                    val statusText = if (isBlocked) "已屏蔽" else "未屏蔽"

                    android.app.AlertDialog.Builder(this@SystemSettingsActivity)
                        .setTitle("Honor侧滑悬浮入口状态")
                        .setMessage("当前状态: $statusText\n\n需要重新应用Kiosk模式设置才能屏蔽Honor侧滑悬浮入口")
                        .setPositiveButton("重新应用设置") { _, _ ->
                            val success =
                                KioskUtils.setupEnhancedKioskMode(this@SystemSettingsActivity)
                            if (success) {
                                showStatus("Honor侧滑悬浮入口屏蔽设置已应用")

                                // 再次检查状态
                                val newStatus =
                                    KioskUtils.isHonorDockBarBlocked(this@SystemSettingsActivity)
                                val newStatusText = if (newStatus) "已屏蔽" else "仍未屏蔽"
                                showStatus("屏蔽状态: $newStatusText")
                            } else {
                                showStatus("屏蔽设置失败，请检查设备管理员权限")
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
            section.addView(dockBarButton)

            // Debug模式下添加Honor重置菜单项屏蔽按钮
            val resetSettingsButton = Button(this).apply {
                text = "Honor重置菜单项屏蔽(Debug)"
                setTextColor(0xFF9C27B0.toInt()) // 紫色文字
                setOnClickListener {
                    val isBlocked =
                        KioskUtils.isHonorResetSettingsBlocked(this@SystemSettingsActivity)
                    val statusText = if (isBlocked) "已屏蔽" else "未屏蔽"

                    android.app.AlertDialog.Builder(this@SystemSettingsActivity)
                        .setTitle("Honor重置菜单项屏蔽状态")
                        .setMessage("当前状态: $statusText\n\n目标: 系统设置应用内的重置菜单项\n(SubSettings页面)\n\n需要重新应用Kiosk模式设置才能屏蔽重置菜单项")
                        .setPositiveButton("重新应用设置") { _, _ ->
                            val success =
                                KioskUtils.setupEnhancedKioskMode(this@SystemSettingsActivity)
                            if (success) {
                                showStatus("Honor重置菜单项屏蔽设置已应用")

                                // 再次检查状态
                                val newStatus =
                                    KioskUtils.isHonorResetSettingsBlocked(this@SystemSettingsActivity)
                                val newStatusText = if (newStatus) "已屏蔽" else "仍未屏蔽"
                                showStatus("重置菜单项屏蔽状态: $newStatusText")
                            } else {
                                showStatus("屏蔽设置失败，请检查设备管理员权限")
                            }
                        }
                        .setNeutralButton("打开系统设置测试") { _, _ ->
                            // 打开系统设置来手动检查重置菜单项是否存在
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
            }
            section.addView(resetSettingsButton)
        }

        // Debug模式下添加退出Kiosk按钮（使用前面已定义的isDebug变量）
        if (isDebug) {
            val exitKioskButton = Button(this).apply {
                text = "退出单应用模式(Debug)"
                setTextColor(0xFFFF0000.toInt()) // 红色文字
                setOnClickListener {
                    android.app.AlertDialog.Builder(this@SystemSettingsActivity)
                        .setTitle("退出单应用模式")
                        .setMessage("确定要退出单应用模式吗？这将清除所有Kiosk设置。")
                        .setPositiveButton("确定") { _, _ ->
                            val success =
                                KioskUtils.exitKioskModeInDebug(this@SystemSettingsActivity)
                            if (success) {
                                showStatus("已退出单应用模式")
                                // 延迟一下再关闭页面
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
            }
            section.addView(exitKioskButton)
        }

        return section
    }

    private fun initViews() {
        // Views are created in createContentView
    }

    private fun initServices() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun updateNetworkStatus() {
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo?.isConnected == true
        val isWifi = networkInfo?.type == ConnectivityManager.TYPE_WIFI

        when {
            isConnected && isWifi -> {
                wifiStatusText.text = "✓ WiFi已连接"
                wifiStatusText.setTextColor(0xFF4CAF50.toInt())
            }

            isConnected -> {
                wifiStatusText.text = "✓ 已连接到网络"
                wifiStatusText.setTextColor(0xFF4CAF50.toInt())
            }

            wifiManager.isWifiEnabled -> {
                wifiStatusText.text = "WiFi已开启，但未连接网络"
                wifiStatusText.setTextColor(0xFFFF9800.toInt())
            }

            else -> {
                wifiStatusText.text = "✗ 无网络连接"
                wifiStatusText.setTextColor(0xFFF44336.toInt())
            }
        }
    }


    private fun openNetworkSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_WIRELESS_SETTINGS)
    }

    private fun openWifiSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_WIFI_SETTINGS)
    }

    private fun openBrightnessSettings() {
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

    private fun openSystemCamera() {
        try {
            // 打开相机应用
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 检查是否有相机应用可以处理这个Intent
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivity(cameraIntent)
                showStatus("正在打开系统相机")
            } else {
                // 如果默认相机Intent不可用，尝试打开特定品牌的相机应用
                val cameraPackages = listOf(
                    "com.android.camera",
                    "com.android.camera2",
                    "com.hihonor.camera",  // Honor相机
                    "com.huawei.camera",
                    "com.oppo.camera",
                    "com.vivo.camera",
                    "com.xiaomi.camera"
                )

                var opened = false
                for (packageName in cameraPackages) {
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                            opened = true
                            break
                        }
                    } catch (e: Exception) {
                        // 继续尝试下一个
                    }
                }

                if (!opened) {
                    showStatus("无法打开相机，请检查相机应用是否安装")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SystemSettingsActivity", "打开相机失败", e)
            showStatus("打开相机失败: ${e.message}")
        }
    }

    private fun openSystemGallery() {
        try {
            // 尝试多种方式打开图库
            val galleryIntents = listOf(
                // 标准图库Intent
                Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                // 选择图片Intent
                Intent(Intent.ACTION_PICK).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                // Google Photos
                packageManager.getLaunchIntentForPackage("com.google.android.apps.photos"),
                // 系统图库
                packageManager.getLaunchIntentForPackage("com.android.gallery3d"),
                // 小米图库
                packageManager.getLaunchIntentForPackage("com.miui.gallery"),
                // 华为图库
                packageManager.getLaunchIntentForPackage("com.huawei.photos"),
                // Honor图库
                packageManager.getLaunchIntentForPackage("com.hihonor.photo"),
                // OPPO图库
                packageManager.getLaunchIntentForPackage("com.coloros.gallery3d"),
                // VIVO图库
                packageManager.getLaunchIntentForPackage("com.vivo.gallery")
            )

            // 尝试每个Intent直到成功
            var opened = false
            for (intent in galleryIntents) {
                if (intent != null) {
                    try {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        showStatus("正在打开系统图库")
                        opened = true
                        break
                    } catch (e: Exception) {
                        // 继续尝试下一个
                    }
                }
            }

            if (!opened) {
                // 如果都失败了，打开文件管理器
                val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
                fileIntent.type = "image/*"
                fileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fileIntent)
                showStatus("正在打开文件选择器")
            }
        } catch (e: Exception) {
            android.util.Log.e("SystemSettingsActivity", "打开图库失败", e)
            showStatus("打开图库失败: ${e.message}")
        }
    }

    private fun postDelayed(action: () -> Unit, delayMillis: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(action, delayMillis)
    }

    // Honor相机 - 需要相机权限
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
                // 如果Honor相机不可用，尝试其他相机
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

    // Honor相册 - 优先直接打开，失败时检查权限
    private fun openHonorGalleryWithPermission() {
        // 先尝试直接打开，大多数情况下不需要特殊权限
        try {
            openHonorGallery()
        } catch (e: Exception) {
            // 如果直接打开失败，再检查权限
            if (checkStoragePermission()) {
                openHonorGallery()
            } else {
                showPermissionExplanationForGallery()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) { // API 33 = TIRAMISU
            // Android 13+ 使用细粒度媒体权限
            ContextCompat.checkSelfPermission(
                this,
                "android.permission.READ_MEDIA_IMAGES"
            ) == PackageManager.PERMISSION_GRANTED
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            // Android 6+ 使用传统存储权限
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6以下不需要运行时权限
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
                // 即使没有权限也尝试打开，有些情况下仍然可以工作
                openHonorGallery()
            }
            .show()
    }

    private fun requestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) { // API 33 = TIRAMISU
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
            // 方案1: 直接启动Honor相册应用
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
                // 方案2: 尝试打开系统图库选择器
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
            try {
                // 方案3: 通用图库Intent
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (viewIntent.resolveActivity(packageManager) != null) {
                    startActivity(viewIntent)
                    showStatus("正在打开系统图库")
                    opened = true
                }
            } catch (e: Exception) {
                android.util.Log.w("SystemSettingsActivity", "系统图库启动失败", e)
            }
        }

        if (!opened) {
            try {
                // 方案4: 文件管理器查看图片
                val fileIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fileIntent.resolveActivity(packageManager) != null) {
                    startActivity(fileIntent)
                    showStatus("正在打开文件选择器")
                    opened = true
                }
            } catch (e: Exception) {
                android.util.Log.w("SystemSettingsActivity", "文件选择器启动失败", e)
            }
        }

        if (!opened) {
            showStatus("未找到可用的图库应用，请安装相册应用")
            // 提供手动解决方案
            android.app.AlertDialog.Builder(this)
                .setTitle("无法打开相册")
                .setMessage("系统中没有找到可用的相册应用。您可以：\n1. 从应用市场安装相册应用\n2. 检查Honor相册是否被禁用\n3. 使用文件管理器查看图片")
                .setPositiveButton("打开应用市场") { _, _ ->
                    try {
                        val marketIntent = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("market://search?q=相册")
                        )
                        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(marketIntent)
                    } catch (e: Exception) {
                        showStatus("无法打开应用市场")
                    }
                }
                .setNegativeButton("确定", null)
                .show()
        }
    }

    // Honor文件管理 - 需要写入权限
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
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q // Android 10+ 不需要写入权限
    }

    private fun requestWritePermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_PERMISSION
            )
        } else {
            // Android 10+ 直接打开
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
                // 如果Honor文件管理不可用，尝试系统文件管理器
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

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
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
                    openHonorFileManager() // 即使权限被拒绝也尝试打开
                }
            }
        }
    }

    /**
     * 显示关机确认对话框
     */
    private fun showShutdownDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("确认关机")
            .setMessage("您确定要关闭设备吗？\n\n单应用模式下关机需要设备管理员权限。")
            .setPositiveButton("确定关机") { _, _ ->
                performShutdown()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示重启确认对话框
     */
    private fun showRebootDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("确认重启")
            .setMessage("您确定要重启设备吗？\n\n单应用模式下重启需要设备管理员权限。")
            .setPositiveButton("确定重启") { _, _ ->
                performReboot()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 执行关机操作
     */
    private fun performShutdown() {
        // 先检查权限
        if (KioskUtils.canShutdownOrReboot(this)) {
            showStatus("正在执行关机...")

            // 延迟执行关机，让用户看到提示
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val success = KioskUtils.shutdownDevice(this)
                if (!success) {
                    showStatus("关机失败，可能需要系统权限")

                    // 提供备选方案
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

    /**
     * 执行重启操作
     */
    private fun performReboot() {
        // 先检查权限
        if (KioskUtils.canShutdownOrReboot(this)) {
            showStatus("正在执行重启...")

            // 延迟执行重启，让用户看到提示
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val success = KioskUtils.rebootDevice(this)
                if (!success) {
                    showStatus("重启失败，可能需要系统权限")

                    // 提供备选方案
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

    override fun onResume() {
        super.onResume()
        updateNetworkStatus()
    }
}