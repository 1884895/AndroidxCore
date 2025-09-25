package com.haofenshu.lnkscreen

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.sqrt

class FloatingButtonView(context: Context, private val onClickListener: () -> Unit) : View(context) {

    companion object {
        private const val TAG = "FloatingButtonView"
        private const val BUTTON_SIZE_DP = 56f
        private const val ICON_SIZE_DP = 24f
        private const val ANIMATION_DURATION = 200L
        private const val MOVE_THRESHOLD_DP = 8f
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

    // 动画相关
    private var scaleAnimator: ValueAnimator? = null
    private var currentScale = 1.0f

    // 红点提醒
    private var showRedDot = true

    init {
        setupPaints()
        setupSize()
    }

    private fun setupPaints() {
        // 背景渐变画笔
        val gradient = LinearGradient(
            0f, 0f, 0f, buttonSize,
            intArrayOf(0xFF4FC3F7.toInt(), 0xFF1976D2.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        backgroundPaint.apply {
            shader = gradient
            style = Paint.Style.FILL
        }

        // 阴影画笔
        shadowPaint.apply {
            color = 0x40000000
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(dpToPx(4f), BlurMaskFilter.Blur.NORMAL)
        }

        // 图标画笔
        iconPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // 红点画笔
        redDotPaint.apply {
            color = 0xFFFF4444.toInt()
            style = Paint.Style.FILL
        }
    }

    private fun setupSize() {
        val size = buttonSize.toInt()
        layoutParams = layoutParams?.apply {
            width = size
            height = size
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (buttonSize / 2f - dpToPx(4f)) * currentScale

        // 绘制阴影
        canvas.drawCircle(centerX, centerY + dpToPx(2f), radius, shadowPaint)

        // 绘制背景圆形
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // 绘制设置图标（齿轮）
        drawSettingsIcon(canvas, centerX, centerY, iconSize * currentScale)

        // 绘制红点提醒
        if (showRedDot) {
            val dotRadius = dpToPx(4f)
            val dotX = centerX + radius * 0.6f
            val dotY = centerY - radius * 0.6f
            canvas.drawCircle(dotX, dotY, dotRadius, redDotPaint)
        }
    }

    private fun drawSettingsIcon(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        val iconRadius = size / 2f
        val innerRadius = iconRadius * 0.4f
        val toothHeight = iconRadius * 0.2f

        canvas.save()
        canvas.translate(centerX, centerY)

        // 绘制齿轮外圈
        val path = Path()
        val toothCount = 8
        val angleStep = 360f / toothCount

        for (i in 0 until toothCount) {
            val angle = Math.toRadians((i * angleStep).toDouble())
            val nextAngle = Math.toRadians(((i + 1) * angleStep).toDouble())

            val x1 = (innerRadius * kotlin.math.cos(angle)).toFloat()
            val y1 = (innerRadius * kotlin.math.sin(angle)).toFloat()
            val x2 = ((innerRadius + toothHeight) * kotlin.math.cos(angle + angleStep / 2 * Math.PI / 180)).toFloat()
            val y2 = ((innerRadius + toothHeight) * kotlin.math.sin(angle + angleStep / 2 * Math.PI / 180)).toFloat()
            val x3 = (innerRadius * kotlin.math.cos(nextAngle)).toFloat()
            val y3 = (innerRadius * kotlin.math.sin(nextAngle)).toFloat()

            if (i == 0) {
                path.moveTo(x1, y1)
            }
            path.lineTo(x2, y2)
            path.lineTo(x3, y3)
        }
        path.close()

        canvas.drawPath(path, iconPaint)

        // 绘制中心圆
        canvas.drawCircle(0f, 0f, innerRadius * 0.6f, iconPaint)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                startTime = System.currentTimeMillis()
                isDragging = false

                // 按下动画
                animateScale(1.1f)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - downX
                val deltaY = event.rawY - downY
                val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

                if (distance > moveThreshold && !isDragging) {
                    isDragging = true
                    Log.d(TAG, "开始拖动")
                }

                if (isDragging) {
                    // 移动悬浮窗
                    val deltaMove = FloatingWindowManager.moveWindow(
                        event.rawX - lastTouchX,
                        event.rawY - lastTouchY
                    )
                    if (deltaMove) {
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 松开动画
                animateScale(1.0f)

                if (!isDragging) {
                    val clickDuration = System.currentTimeMillis() - startTime
                    if (clickDuration < ViewConfiguration.getTapTimeout()) {
                        // 点击事件
                        performClick()
                        hideRedDot()
                    }
                } else {
                    // 拖动结束，吸附到边缘
                    FloatingWindowManager.snapToEdge()
                }

                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        Log.d(TAG, "悬浮按钮被点击")

        // 点击动画
        animateScale(0.9f) {
            animateScale(1.0f) {
                onClickListener.invoke()
            }
        }
        return true
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scaleAnimator?.cancel()
    }
}