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
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SystemSettingsActivity : AppCompatActivity() {

    private lateinit var wifiStatusText: TextView
    private lateinit var brightnessButton: View
    private lateinit var soundSettingsButton: View
    private lateinit var dateTimeButton: View
    private lateinit var networkCheckButton: View
    private lateinit var shutdownButton: View
    private lateinit var rebootButton: View
    private lateinit var backButton: View
    private lateinit var versionText: TextView
    private lateinit var debugButtonsLayout: LinearLayout

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private var input = 0

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
        private const val REQUEST_WRITE_PERMISSION = 1003
        const val KEY_INPUT = "key_input"
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

        setContentView(R.layout.activity_system_settings1)

        initViews()
        initServices()
        setupClickListeners()
        updateNetworkStatus()
        input = intent.getIntExtra(KEY_INPUT, 0)
    }

    private fun initViews() {
        wifiStatusText = findViewById(R.id.wifiStatusText)
        brightnessButton = findViewById(R.id.brightnessButton)
        soundSettingsButton = findViewById(R.id.soundSettingsButton)
        dateTimeButton = findViewById(R.id.dateTimeButton)
        networkCheckButton = findViewById(R.id.networkCheckButton)
        shutdownButton = findViewById(R.id.shutdownButton)
        rebootButton = findViewById(R.id.rebootButton)
        backButton = findViewById(R.id.backButton)
        versionText = findViewById(R.id.versionText)
        debugButtonsLayout = findViewById(R.id.debugButtonsLayout)

        // 设置版本号
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "版本号: ${packageInfo.versionName}"
        } catch (e: Exception) {
            versionText.text = "版本号: 未知"
        }

        // 检查是否为Debug模式
        checkDebugMode()
    }

    private fun initServices() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun setupClickListeners() {
        // 返回按钮
        backButton.setOnClickListener {
            if (input == 0) {
                setResult(111111, Intent())
                finish()
            }
        }

        // 基本功能按钮
        brightnessButton.setOnClickListener { openBrightnessSettings() }
        soundSettingsButton.setOnClickListener { openSoundSettings() }
        dateTimeButton.setOnClickListener { openDateTimeSettings() }

        // 网络检测按钮
        networkCheckButton.setOnClickListener {
            performNetworkCheck()
        }

        // 电源管理按钮
        shutdownButton.setOnClickListener {
            showShutdownDialog()
        }
        rebootButton.setOnClickListener {
            showRebootDialog()
        }
    }

    private fun checkDebugMode() {
        val applicationInfo = applicationInfo
        val isDebug =
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebug) {
            // 显示Debug按钮组
            debugButtonsLayout.visibility = View.VISIBLE

            // 设置Debug按钮点击事件
            debugButtonsLayout.findViewById<View>(R.id.developerButton).setOnClickListener {
                openDeveloperOptions()
            }
            debugButtonsLayout.findViewById<View>(R.id.applicationButton).setOnClickListener {
                openApplicationSettings()
            }
            debugButtonsLayout.findViewById<View>(R.id.cameraButton).setOnClickListener {
                openHonorCameraWithPermission()
            }
            debugButtonsLayout.findViewById<View>(R.id.galleryButton).setOnClickListener {
                openHonorGalleryWithPermission()
            }
            debugButtonsLayout.findViewById<View>(R.id.fileManagerButton).setOnClickListener {
                openHonorFileManagerWithPermission()
            }
            debugButtonsLayout.findViewById<View>(R.id.exitKioskButton).setOnClickListener {
                handleExitKioskMode()
            }
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
        android.app.AlertDialog.Builder(this)
            .setTitle("网络检测")
            .setMessage("网络状态信息：\n\n$networkInfo")
            .setPositiveButton("打开WiFi设置") { _, _ ->
                openWifiSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openWifiSettings() {
        KioskUtils.openSystemSettings(this, Settings.ACTION_WIFI_SETTINGS)
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

    private fun openDeveloperOptions() {
        KioskUtils.openDeveloperOptions(this)
    }

    private fun openApplicationSettings() {
        KioskUtils.openApplicationSettings(this)
    }

    private fun showStatus(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && input == 0) {
            setResult(111111, Intent())
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }
}