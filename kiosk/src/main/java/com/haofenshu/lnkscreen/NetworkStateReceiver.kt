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
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo?.isConnected == true

            Log.d(TAG, "网络状态变化: ${if (isConnected) "已连接" else "已断开"}")

            // 仅记录网络状态变化，不进行自动跳转
            if (lastNetworkState && !isConnected) {
                Log.d(TAG, "检测到网络断开")
                handleNetworkDisconnected(context)
            } else if (!lastNetworkState && isConnected) {
                Log.d(TAG, "网络已恢复连接")
                handleNetworkConnected(context)
            }

            lastNetworkState = isConnected
        }
    }

    private fun handleNetworkDisconnected(context: Context) {
        // 仅记录网络断开，不执行自动跳转
        Log.d(TAG, "网络已断开")
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

    // Controller 对象保留，但移除自动跳转相关功能
    object Controller {
        // 仅提供网络状态检查功能，不再提供自动跳转
        fun isNetworkAvailable(context: Context): Boolean {
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