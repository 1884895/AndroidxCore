package com.haofenshu.lnkscreen


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import com.haofenshu.lnkscreen.KioskUtils

class NetworkStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkStateReceiver"
        private var lastNetworkState = true // 默认认为有网络
        private var autoJumpEnabled = false  // 默认禁用自动跳转
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo?.isConnected == true

            Log.d(TAG, "网络状态变化: ${if (isConnected) "已连接" else "已断开"}")

            // 如果从有网络变为无网络，且启用了自动跳转
            if (lastNetworkState && !isConnected && autoJumpEnabled) {
                Log.d(TAG, "检测到网络断开，准备跳转到设置页面")
                handleNetworkDisconnected(context)
            } else if (!lastNetworkState && isConnected) {
                Log.d(TAG, "网络已恢复连接")
                handleNetworkConnected(context)
            }

            lastNetworkState = isConnected
        }
    }

    private fun handleNetworkDisconnected(context: Context) {
        try {
            // 延迟一点时间确保网络状态稳定
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isNetworkAvailable(context)) {
                    Log.d(TAG, "确认网络断开，跳转到系统设置页面")
                    KioskUtils.openSystemSettingsManager(context)
                }
            }, 2000) // 延迟2秒
        } catch (e: Exception) {
            Log.e(TAG, "处理网络断开失败", e)
        }
    }

    private fun handleNetworkConnected(context: Context) {
        // 网络恢复时的处理，可以显示通知或其他操作
        Log.d(TAG, "网络已恢复")
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        } catch (e: Exception) {
            Log.e(TAG, "检查网络状态失败", e)
            false
        }
    }

    // 静态方法用于控制自动跳转功能
    object Controller {
        fun enableAutoJump() {
            autoJumpEnabled = true
            Log.d(TAG, "自动跳转功能已启用")
        }

        fun disableAutoJump() {
            autoJumpEnabled = false
            Log.d(TAG, "自动跳转功能已禁用")
        }

        fun isAutoJumpEnabled(): Boolean {
            return autoJumpEnabled
        }

        fun checkNetworkAndJump(context: Context) {
            if (!isNetworkAvailable(context) && autoJumpEnabled) {
                Log.d(TAG, "手动检查：网络不可用，跳转到设置页面")
                KioskUtils.openSystemSettingsManager(context)
            }
        }

        private fun isNetworkAvailable(context: Context): Boolean {
            return try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            } catch (e: Exception) {
                Log.e(TAG, "检查网络状态失败", e)
                false
            }
        }
    }
}