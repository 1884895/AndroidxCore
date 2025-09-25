package com.haofenshu.lnkscreen

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator

object FloatingWindowManager {
    private const val TAG = "FloatingWindowManager"

    private var windowManager: WindowManager? = null
    private var floatingView: FloatingButtonView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var context: Context? = null

    // 屏幕尺寸
    private var screenWidth = 0
    private var screenHeight = 0
    private var statusBarHeight = 0

    // 边缘吸附动画
    private var snapAnimator: ValueAnimator? = null

    fun initialize(ctx: Context) {
        context = ctx
        windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 获取屏幕尺寸
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // 获取状态栏高度
        statusBarHeight = getStatusBarHeight(ctx)

        Log.d(TAG, "FloatingWindowManager初始化完成 屏幕尺寸: ${screenWidth}x${screenHeight}")
    }

    fun showFloatingButton(onClickListener: () -> Unit): Boolean {
        return try {
            if (floatingView != null) {
                Log.w(TAG, "悬浮按钮已经存在")
                return true
            }

            val ctx = context ?: throw IllegalStateException("Context未初始化")

            // 检查悬浮窗权限
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(ctx)
            } else {
                true
            }

            if (!hasPermission) {
                Log.e(TAG, "缺少悬浮窗权限 SYSTEM_ALERT_WINDOW")
                return false
            }

            Log.d(TAG, "开始创建悬浮按钮，屏幕尺寸: ${screenWidth}x${screenHeight}")

            // 创建悬浮按钮视图
            floatingView = FloatingButtonView(ctx, onClickListener)

            // 创建悬浮窗参数
            layoutParams = createLayoutParams().apply {
                // 默认位置：右侧中间
                x = screenWidth - dpToPx(ctx, 56f + 16f) // 按钮宽度 + 边距
                y = screenHeight / 2 - dpToPx(ctx, 28f) // 居中
                Log.d(TAG, "悬浮窗位置: (${x}, ${y})")
            }

            // 添加到窗口
            windowManager?.addView(floatingView, layoutParams)
            Log.d(TAG, "悬浮按钮显示成功，位置: (${layoutParams?.x}, ${layoutParams?.y})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "显示悬浮按钮失败", e)
            false
        }
    }

    fun hideFloatingButton(): Boolean {
        return try {
            floatingView?.let { view ->
                windowManager?.removeView(view)
                floatingView = null
                layoutParams = null
                Log.d(TAG, "悬浮按钮已隐藏")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "隐藏悬浮按钮失败", e)
            false
        }
    }

    fun moveWindow(deltaX: Float, deltaY: Float): Boolean {
        return try {
            val params = layoutParams ?: return false
            val ctx = context ?: return false

            // 计算新位置
            val newX = params.x + deltaX.toInt()
            val newY = params.y + deltaY.toInt()

            // 边界检查
            val buttonSize = dpToPx(ctx, 56f)
            val margin = dpToPx(ctx, 8f)

            params.x = newX.coerceIn(-buttonSize + margin, screenWidth - margin)
            params.y = newY.coerceIn(statusBarHeight, screenHeight - buttonSize - margin)

            // 更新位置
            windowManager?.updateViewLayout(floatingView, params)
            true
        } catch (e: Exception) {
            Log.e(TAG, "移动悬浮窗失败", e)
            false
        }
    }

    fun snapToEdge() {
        val params = layoutParams ?: return
        val ctx = context ?: return

        val buttonSize = dpToPx(ctx, 56f)
        val margin = dpToPx(ctx, 16f)
        val centerX = screenWidth / 2

        // 判断吸附到左边还是右边
        val targetX = if (params.x < centerX) {
            // 吸附到左边
            margin
        } else {
            // 吸附到右边
            screenWidth - buttonSize - margin
        }

        // 动画吸附
        animateToPosition(targetX, params.y)
    }

    private fun animateToPosition(targetX: Int, targetY: Int) {
        val params = layoutParams ?: return
        val startX = params.x
        val startY = params.y

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                params.x = (startX + (targetX - startX) * progress).toInt()
                params.y = (startY + (targetY - startY) * progress).toInt()

                try {
                    windowManager?.updateViewLayout(floatingView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "动画更新位置失败", e)
                    cancel()
                }
            }
            start()
        }
    }

    fun showRedDot() {
        floatingView?.showRedDot()
    }

    fun hideRedDot() {
        floatingView?.hideRedDot()
    }

    fun isShowingRedDot(): Boolean {
        return floatingView?.isShowingRedDot() ?: false
    }

    fun isShowing(): Boolean {
        return floatingView != null
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val ctx = context ?: throw IllegalStateException("Context未初始化")

        return WindowManager.LayoutParams().apply {
            // 窗口类型
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // 窗口标志
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            // 格式
            format = PixelFormat.TRANSLUCENT

            // 尺寸
            val buttonSize = dpToPx(ctx, 56f)
            width = buttonSize
            height = buttonSize

            // 位置
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun onDestroy() {
        snapAnimator?.cancel()
        hideFloatingButton()
        windowManager = null
        context = null
    }

    // 获取当前悬浮窗位置信息（用于调试）
    fun getPositionInfo(): String {
        val params = layoutParams ?: return "悬浮窗未显示"
        return "悬浮窗位置: (${params.x}, ${params.y}) 屏幕: ${screenWidth}x${screenHeight}"
    }

    // 重置到默认位置
    fun resetPosition() {
        val ctx = context ?: return
        val buttonSize = dpToPx(ctx, 56f)
        val margin = dpToPx(ctx, 16f)

        val targetX = screenWidth - buttonSize - margin
        val targetY = screenHeight / 2 - buttonSize / 2

        animateToPosition(targetX, targetY)
    }

    // 检查悬浮窗是否在屏幕边缘
    fun isAtEdge(): Boolean {
        val params = layoutParams ?: return false
        val ctx = context ?: return false

        val margin = dpToPx(ctx, 32f) // 边缘阈值
        val buttonSize = dpToPx(ctx, 56f)

        return params.x <= margin ||
               params.x >= screenWidth - buttonSize - margin
    }
}