package com.haofenshu.AndroidxCore

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_last_mian)
        startLockTaskIfNeeded()
    }

    /**
     * 启动LockTask模式
     * Kiosk模式的配置已在Application中完成，这里只负责启动LockTask
     */
    private fun startLockTaskIfNeeded() {
        try {
            // 从Application获取实例并启动LockTask
            if (getApplication() is AndroidXCore) {
                (getApplication() as AndroidXCore).startLockTaskIfNeeded(this)
            }
        } catch (e: Exception) {
            Log.e("ParentSplashActivity", "启动LockTask失败", e)
        }
    }
}