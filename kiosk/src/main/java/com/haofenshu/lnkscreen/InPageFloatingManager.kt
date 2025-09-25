package com.haofenshu.lnkscreen

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * 页面内悬浮按钮管理器
 * 用于在指定的Activity中添加悬浮按钮
 */
object InPageFloatingManager {
    private const val TAG = "InPageFloatingManager"
    private const val PREF_NAME = "floating_button_positions"

    // 存储各个Activity的悬浮按钮实例
    private val floatingButtons = mutableMapOf<String, InPageFloatingButton>()

    // 存储各个Activity的悬浮按钮位置（内存缓存）
    private val floatingPositions = mutableMapOf<String, Pair<Int, Int>>()

    /**
     * 保存位置到SharedPreferences
     */
    private fun savePosition(activity: Activity, position: Pair<Int, Int>) {
        val activityName = activity.javaClass.simpleName
        val prefs = activity.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE)
        prefs.edit()
            .putInt("${activityName}_left", position.first)
            .putInt("${activityName}_top", position.second)
            .apply()
    }

    /**
     * 从SharedPreferences读取位置
     */
    private fun loadPosition(activity: Activity): Pair<Int, Int>? {
        val activityName = activity.javaClass.simpleName
        val prefs = activity.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE)
        val left = prefs.getInt("${activityName}_left", -1)
        val top = prefs.getInt("${activityName}_top", -1)
        return if (left >= 0 && top >= 0) {
            Pair(left, top)
        } else {
            null
        }
    }

    /**
     * 为Activity添加页面内悬浮按钮
     */
    fun addFloatingButton(activity: Activity): Boolean {
        return try {
            val activityName = activity.javaClass.simpleName

            // 检查是否已经添加过
            if (floatingButtons.containsKey(activityName)) {
                Log.w(TAG, "Activity $activityName 已经添加过悬浮按钮")
                return true
            }

            // 获取Activity的根布局
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            if (rootView == null) {
                Log.e(TAG, "无法获取Activity的根布局")
                return false
            }

            // 创建悬浮按钮
            val floatingButton = InPageFloatingButton(activity)

            // 不再显示网络状态红点

            // 先从SharedPreferences读取位置，再检查内存缓存
            var savedPosition = loadPosition(activity)
            if (savedPosition == null) {
                savedPosition = floatingPositions[activityName]
            }
            val hasSavedPosition = savedPosition != null

            // 添加到根布局（如果有保存的位置，不使用默认位置）
            floatingButton.addToParent(rootView, useDefaultPosition = !hasSavedPosition)

            // 如果有保存的位置，恢复位置
            if (savedPosition != null) {
                floatingButton.post {
                    // 恢复保存的位置
                    floatingButton.restorePosition(savedPosition.first, savedPosition.second)
                    Log.d(TAG, "恢复 $activityName 的悬浮按钮位置: (${savedPosition.first}, ${savedPosition.second})")
                }
                // 更新内存缓存
                floatingPositions[activityName] = savedPosition
            }

            // 保存引用
            floatingButtons[activityName] = floatingButton

            Log.d(TAG, "成功为 $activityName 添加页面内悬浮按钮")
            true
        } catch (e: Exception) {
            Log.e(TAG, "添加悬浮按钮失败", e)
            false
        }
    }

    /**
     * 从Activity移除页面内悬浮按钮
     */
    fun removeFloatingButton(activity: Activity): Boolean {
        return try {
            val activityName = activity.javaClass.simpleName
            val floatingButton = floatingButtons[activityName]

            if (floatingButton != null) {
                // 保存当前位置
                val position = floatingButton.getCurrentPosition()
                floatingPositions[activityName] = position

                // 保存到SharedPreferences
                savePosition(activity, position)
                Log.d(TAG, "保存 $activityName 的悬浮按钮位置: (${position.first}, ${position.second})")

                floatingButton.removeFromParent()
                floatingButtons.remove(activityName)
                Log.d(TAG, "成功从 $activityName 移除页面内悬浮按钮")
                true
            } else {
                Log.w(TAG, "Activity $activityName 没有悬浮按钮可移除")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "移除悬浮按钮失败", e)
            false
        }
    }

    /**
     * 显示红点提醒
     */
    fun showRedDot(activity: Activity) {
        val activityName = activity.javaClass.simpleName
        floatingButtons[activityName]?.showRedDot()
    }

    /**
     * 隐藏红点提醒
     */
    fun hideRedDot(activity: Activity) {
        val activityName = activity.javaClass.simpleName
        floatingButtons[activityName]?.hideRedDot()
    }

    /**
     * 检查是否正在显示红点
     */
    fun isShowingRedDot(activity: Activity): Boolean {
        val activityName = activity.javaClass.simpleName
        return floatingButtons[activityName]?.isShowingRedDot() ?: false
    }

    /**
     * 检查Activity是否已添加悬浮按钮
     */
    fun hasFloatingButton(activity: Activity): Boolean {
        val activityName = activity.javaClass.simpleName
        return floatingButtons.containsKey(activityName)
    }

    /**
     * 获取所有有悬浮按钮的Activity数量
     */
    fun getActiveButtonCount(): Int {
        return floatingButtons.size
    }

    /**
     * 清理所有悬浮按钮（应用退出时调用）
     */
    fun clearAll() {
        floatingButtons.values.forEach { button ->
            try {
                button.removeFromParent()
            } catch (e: Exception) {
                Log.e(TAG, "清理悬浮按钮时出错", e)
            }
        }
        floatingButtons.clear()
        floatingPositions.clear() // 清理保存的位置
        Log.d(TAG, "已清理所有页面内悬浮按钮和位置记录")
    }

    /**
     * 根据网络状态更新所有悬浮按钮的红点状态
     */
    fun updateNetworkStatus(hasNetwork: Boolean) {
        floatingButtons.values.forEach { button ->
            if (hasNetwork) {
                button.hideRedDot()
            } else {
                button.showRedDot()
            }
        }
        Log.d(TAG, "已更新所有悬浮按钮的网络状态提醒: ${if (hasNetwork) "隐藏" else "显示"}红点")
    }

    /**
     * 获取状态信息
     */
    fun getStatusInfo(): String {
        val sb = StringBuilder()
        sb.append("页面内悬浮按钮状态:\n")
        sb.append("- 活跃按钮数量: ${floatingButtons.size}\n")

        if (floatingButtons.isNotEmpty()) {
            sb.append("- 活跃页面: ${floatingButtons.keys.joinToString(", ")}\n")
            val redDotCount = floatingButtons.values.count { it.isShowingRedDot() }
            sb.append("- 红点提醒数量: $redDotCount\n")
        }

        return sb.toString()
    }
}