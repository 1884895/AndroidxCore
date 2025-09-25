package com.haofenshu.lnkscreen

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 页面内悬浮按钮 - 不需要系统悬浮窗权限
 * 可以直接添加到任何 Activity 的布局中
 */
class InPageFloatingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "InPageFloatingButton"
        private const val BUTTON_SIZE_DP = 56f
        private const val ICON_SIZE_DP = 24f
        private const val ANIMATION_DURATION = 200L
        private const val MOVE_THRESHOLD_DP = 8f
        private const val AUTO_HIDE_DELAY = 5000L
    }

    private val buttonSize = dpToPx(BUTTON_SIZE_DP)
    private val iconSize = dpToPx(ICON_SIZE_DP)
    private val moveThreshold = dpToPx(MOVE_THRESHOLD_DP)

    // 绘制相关
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val redDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 触摸相关
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downX = 0f
    private var downY = 0f
    private var startTime = 0L

    // 拖动相关，使用原始坐标避免抖动
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dX = 0f
    private var dY = 0f

    // 动画相关
    private var scaleAnimator: ValueAnimator? = null
    private var alphaAnimator: ValueAnimator? = null
    private var currentScale = 1.0f
    private var currentAlpha = 0.8f

    // 状态相关
    private var showRedDot = false
    private var isVisible = true
    private var autoHideHandler: Handler? = null
    private var autoHideRunnable: Runnable? = null

    // 位置相关
    private var parentWidth = 0
    private var parentHeight = 0
    private var isAttachedToEdge = false

    // 保存的位置信息，用于布局变化时恢复
    private var savedLeft = -1
    private var savedTop = -1
    private var hasInitialPosition = false

    init {
        setupPaints()
        setupSize()
        setupAutoHide()
    }

    private fun setupPaints() {
        // 背景画笔 - 黄色背景
        backgroundPaint.apply {
            color = 0xFFFFC107.toInt() // Material Design Amber
            style = Paint.Style.FILL
        }

        // 阴影画笔
        shadowPaint.apply {
            color = 0x60000000 // 更深的阴影
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(dpToPx(6f), BlurMaskFilter.Blur.NORMAL)
        }

        // 图标画笔 - 白色扳手
        iconPaint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = dpToPx(1.5f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // 红点画笔 - 红色提醒点
        redDotPaint.apply {
            color = 0xFFFF4444.toInt() // 红色警告
            style = Paint.Style.FILL
        }
    }

    private fun setupSize() {
        val size = buttonSize.toInt()
        minimumWidth = size
        minimumHeight = size
    }

    private fun setupAutoHide() {
        autoHideHandler = Handler(Looper.getMainLooper())
        autoHideRunnable = Runnable {
            if (!isDragging) {
                animateToEdge()
            }
        }
        startAutoHideTimer()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (buttonSize / 2f - dpToPx(4f)) * currentScale

        // 应用透明度
        val alpha = (255 * currentAlpha).toInt()
        backgroundPaint.alpha = alpha
        shadowPaint.alpha = (alpha * 0.5f).toInt()
        iconPaint.alpha = alpha

        // 绘制阴影
        canvas.drawCircle(centerX, centerY + dpToPx(3f), radius, shadowPaint)

        // 绘制背景圆形
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // 绘制扳手图标
        drawWrenchIcon(canvas, centerX, centerY, iconSize * currentScale)

        // 不再绘制红点提醒
    }

    private fun drawWrenchIcon(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(-45f) // 倾斜45度使扳手呈对角线方向

        val scale = size / 20f // 缩放比例
        canvas.scale(scale, scale)

        // 简化的扳手设计
        val path = Path()

        // 扳手主体 - 手柄部分
        val handleWidth = 2.5f
        val handleLength = 8f

        // 绘制手柄
        val rect = RectF(-handleWidth/2, -2f, handleWidth/2, handleLength)
        path.addRoundRect(rect, 0.5f, 0.5f, Path.Direction.CW)

        // 绘制扳手头部 - 开口端（上方）
        val headWidth = 6f
        val headHeight = 3f
        val headTop = -2f - headHeight

        // 左边臂
        path.addRect(-headWidth/2, headTop, -headWidth/2 + 1.5f, -2f, Path.Direction.CW)
        // 右边臂
        path.addRect(headWidth/2 - 1.5f, headTop, headWidth/2, -2f, Path.Direction.CW)

        // 绘制扳手尾部 - 固定端（下方）
        val tailSize = 3.5f
        val tailTop = handleLength - 0.5f

        // 六边形螺母形状
        val hexPath = Path()
        for (i in 0..5) {
            val angle = Math.toRadians((60 * i - 30).toDouble()) // 旋转30度使平边朝上
            val x = (tailSize * kotlin.math.cos(angle)).toFloat()
            val y = tailTop + (tailSize * kotlin.math.sin(angle)).toFloat()
            if (i == 0) {
                hexPath.moveTo(x, y)
            } else {
                hexPath.lineTo(x, y)
            }
        }
        hexPath.close()
        path.addPath(hexPath)

        // 绘制整个扳手
        iconPaint.style = Paint.Style.FILL
        canvas.drawPath(path, iconPaint)

        // 在六边形中心绘制一个小圆孔
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFC107.toInt() // 使用黄色背景色
            style = Paint.Style.FILL
        }
        canvas.drawCircle(0f, tailTop, tailSize * 0.4f, holePaint)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                startTime = System.currentTimeMillis()
                isDragging = false

                // 记录触摸点与View左上角的偏移
                lastRawX = event.rawX
                lastRawY = event.rawY
                dX = lastRawX - left
                dY = lastRawY - top

                // 停止自动隐藏
                stopAutoHideTimer()

                // 显示按钮
                showButton()

                // 按下动画
                animateScale(1.1f)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - downX
                val deltaY = event.y - downY
                val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                if (distance > moveThreshold && !isDragging) {
                    isDragging = true
                    Log.d(TAG, "开始拖动")
                }

                if (isDragging) {
                    // 使用layout方法移动按钮，避免触发重绘
                    val newLeft = (event.rawX - dX).toInt()
                    val newTop = (event.rawY - dY).toInt()

                    // 边界检查 - 允许按钮四分之一超出边缘
                    val parent = parent as? ViewGroup
                    if (parent != null) {
                        val edgeMargin = -width / 4  // 允许四分之一超出边缘
                        val topMargin = dpToPx(24f).toInt() // 顶部保留状态栏空间
                        val bottomMargin = dpToPx(16f).toInt()

                        val maxLeft = parent.width - (width * 3 / 4) // 右侧也允许四分之一超出
                        val maxTop = parent.height - height - bottomMargin

                        val finalLeft = newLeft.coerceIn(edgeMargin, maxLeft)
                        val finalTop = newTop.coerceIn(topMargin, maxTop)

                        // 使用layout方法更新位置，不触发重绘
                        layout(finalLeft, finalTop, finalLeft + width, finalTop + height)

                        // 保存当前位置
                        savedLeft = finalLeft
                        savedTop = finalTop
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 松开动画
                animateScale(1.0f)

                if (!isDragging) {
                    val clickDuration = System.currentTimeMillis() - startTime
                    if (clickDuration < 300) {
                        // 点击事件
                        performClick()
                    }
                } else {
                    // 拖动结束，吸附到边缘
                    snapToEdge()
                }

                isDragging = false

                // 重新开始自动隐藏计时
                startAutoHideTimer()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        Log.d(TAG, "页面内悬浮按钮被点击")

        // 点击动画
        animateScale(0.9f) {
            animateScale(1.0f) {
                openSystemSettings()
            }
        }
        return true
    }

    private fun openSystemSettings() {
        try {
            val intent = Intent(context, SystemSettingsActivity::class.java)
            // 清除任务栈，确保不会跳转到之前的系统设置页面
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
            Log.d(TAG, "系统设置页面已打开")
        } catch (e: Exception) {
            Log.e(TAG, "打开系统设置页面失败", e)
        }
    }

    private fun animateScale(targetScale: Float, onEnd: (() -> Unit)? = null) {
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(currentScale, targetScale).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                invalidate()
            }
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onEnd.invoke()
                    }
                })
            }
            start()
        }
    }

    private fun animateAlpha(targetAlpha: Float, onEnd: (() -> Unit)? = null) {
        alphaAnimator?.cancel()
        alphaAnimator = ValueAnimator.ofFloat(currentAlpha, targetAlpha).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentAlpha = animation.animatedValue as Float
                invalidate()
            }
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onEnd.invoke()
                    }
                })
            }
            start()
        }
    }

    private fun snapToEdge() {
        val parent = parent as? ViewGroup ?: return
        val centerX = parent.width / 2f

        // 判断吸附到左边还是右边，隐藏四分之一
        val targetLeft = if (left < centerX) {
            // 吸附到左边缘
            -width / 4  // 让按钮四分之一隐藏在边缘外
        } else {
            // 吸附到右边缘
            parent.width - (width * 3 / 4)  // 让按钮四分之一隐藏在边缘外
        }

        animateToPosition(targetLeft, top)
        isAttachedToEdge = true
    }

    private fun animateToEdge() {
        if (!isAttachedToEdge) {
            snapToEdge()
        }
        // 半透明显示
        animateAlpha(0.5f)
    }

    private fun animateToPosition(targetLeft: Int, targetTop: Int) {
        val startLeft = left
        val startTop = top

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val newLeft = (startLeft + (targetLeft - startLeft) * progress).toInt()
                val newTop = (startTop + (targetTop - startTop) * progress).toInt()

                // 使用layout方法更新位置，避免触发不必要的重绘
                layout(newLeft, newTop, newLeft + width, newTop + height)

                // 保存当前位置
                savedLeft = newLeft
                savedTop = newTop
            }
            start()
        }
    }

    private fun showButton() {
        isVisible = true
        animateAlpha(0.8f)
        isAttachedToEdge = false
    }

    private fun startAutoHideTimer() {
        stopAutoHideTimer()
        autoHideHandler?.postDelayed(autoHideRunnable!!, AUTO_HIDE_DELAY)
    }

    private fun stopAutoHideTimer() {
        autoHideHandler?.removeCallbacks(autoHideRunnable!!)
    }

    fun showRedDot() {
        showRedDot = true
        invalidate()
    }

    fun hideRedDot() {
        showRedDot = false
        invalidate()
    }

    fun isShowingRedDot(): Boolean = showRedDot

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // 如果有保存的位置，始终恢复到保存的位置
        if (hasInitialPosition && savedLeft >= 0 && savedTop >= 0) {
            // 检查当前位置是否与保存的位置不同
            if (left != savedLeft || top != savedTop) {
                // 恢复到保存的位置
                post {
                    layout(savedLeft, savedTop, savedLeft + width, savedTop + height)
                    Log.d(TAG, "保持悬浮按钮位置不变: ($savedLeft, $savedTop)")
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scaleAnimator?.cancel()
        alphaAnimator?.cancel()
        stopAutoHideTimer()
    }

    /**
     * 添加到指定的ViewGroup中
     */
    fun addToParent(parent: ViewGroup, useDefaultPosition: Boolean = true) {
        if (this.parent != null) {
            (this.parent as ViewGroup).removeView(this)
        }

        val layoutParams = ViewGroup.LayoutParams(
            buttonSize.toInt(),
            buttonSize.toInt()
        )

        // 设置初始位置（右下角）
        parent.addView(this, layoutParams)

        if (useDefaultPosition) {
            // 等待布局完成后设置默认位置
            post {
                // 只有在没有保存位置时才设置默认位置
                if (!hasInitialPosition || savedLeft < 0 || savedTop < 0) {
                    val margin = dpToPx(16f).toInt()
                    val initialLeft = parent.width - width - margin
                    val initialTop = parent.height - height - margin - dpToPx(80f).toInt() // 避开底部导航

                    // 使用layout方法设置初始位置
                    layout(initialLeft, initialTop, initialLeft + width, initialTop + height)

                    // 保存初始位置
                    savedLeft = initialLeft
                    savedTop = initialTop
                    hasInitialPosition = true
                }
                showButton()
            }
        } else {
            // 不设置默认位置，等待外部恢复位置
            post {
                showButton()
            }
        }
    }

    /**
     * 从父容器中移除
     */
    fun removeFromParent() {
        (parent as? ViewGroup)?.removeView(this)
    }

    /**
     * 恢复到指定位置
     */
    fun restorePosition(left: Int, top: Int) {
        // 使用layout方法设置位置
        layout(left, top, left + width, top + height)

        // 更新内部保存的位置
        savedLeft = left
        savedTop = top
        hasInitialPosition = true

        Log.d(TAG, "恢复位置: ($left, $top)")
    }

    /**
     * 获取当前位置
     */
    fun getCurrentPosition(): Pair<Int, Int> {
        return Pair(left, top)
    }
}