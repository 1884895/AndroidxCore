package com.xcore.floatball

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 勿删
 *任务：此控件为悬浮球
 * 1.点击展开为6个功能菜单
 * 2.支持可移动，自动吸边
 * 3.吸附到左边时点击展开右半球，吸附到右边点击展开左半球，展开时功能菜单沿半球弧度均匀排列
 * 4.安全距离：左、右半球可完全的打开
 * 5.添加安全距离，松开手指时如果距离上或者下边距离不可安全展开菜单，悬浮球自动移动到左或者右两边的安全距离点处
 * 6.菜单栏展开时不支持移动，点击中心或者点击菜单收起菜单栏
 * 7.中心使用图片资源设置不要用代码进行绘制，包含其他的的菜单选项尽量使用xml布局作为子view填充进来，原则尽可能减少绘制事件
 *问题：
 * 任务3：悬浮球可随手指移动，松开后自动吸附到屏幕的左边或者右边（根据距离左边或者右边的边距自动判断），注意是吸附到左边或者右边且无边距！！！！
 * 任务4: menuItemViews更改为TextView（分别对应首页底部tab的文字） 图片（top）图片大小48*48dp,padding:2.5dp   +文字（底部居中）文字颜色：#333333 大小：10sp。
 * 任务5： 背景色位：#FFFFFF，不透明度为0.1 ，悬浮球周边距离menuItemViews距离为12dp
 */
class CircleFanMenuViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 中心悬浮按钮
    private lateinit var centerButton: FloatingActionButton

    // 圆形背景View（作为菜单项的容器）
    private lateinit var backgroundView: FrameLayout


    // 菜单项子View
    private val menuItemViews = mutableListOf<View>()

    // 布局参数（dp转px）
    private var backgroundRadius: Float
    private var innerPadding: Float
    private var menuItemCount: Int
    private var animationDelay: Long
    private var menuItemLayoutId: Int
    private var menuIcons: IntArray
    private var menuTexts: Array<String>
    private var centerScr: Int

    // 计算得出的菜单分布半径
    private val menuRadius: Float
        get() = backgroundRadius - innerPadding
    private val buttonSize = (48 * context.resources.displayMetrics.density).toInt() // 改为48dp

    // 悬浮球位置（可拖动）
    private var buttonCenterX = 0f
    private var buttonCenterY = 0f

    // 拖动相关
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var dragStartX = 0f
    private var dragStartY = 0f

    // 菜单状态 - 使用自定义setter确保背景和菜单项同步
    private var _isMenuOpen = false
    private var isMenuOpen: Boolean
        get() = _isMenuOpen
        set(value) {
            if (_isMenuOpen != value) {
                _isMenuOpen = value
                syncMenuState()
            }
        }
    private var animatorSet: AnimatorSet? = null
    private var edgeAnimator: ValueAnimator? = null

    // 协程作用域，用于管理动画生命周期
    private val animationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 动画协程任务
    private var animationJob: Job? = null

    private var onMenuItemClickListener: ((Int) -> Unit)? = null

    init {
        // 读取自定义属性
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleFanMenuViewGroup)
        try {
            // 内边距，默认40dp
            innerPadding = typedArray.getDimension(
                R.styleable.CircleFanMenuViewGroup_innerPadding,
                40f * context.resources.displayMetrics.density
            )
            // 背景半径，默认182dp
            backgroundRadius = typedArray.getDimension(
                R.styleable.CircleFanMenuViewGroup_backgroundRadius,
                182f * context.resources.displayMetrics.density
            )
            // 菜单项数量，默认6个
            menuItemCount = typedArray.getInteger(
                R.styleable.CircleFanMenuViewGroup_menuItemCount,
                6
            )
            // 动画延迟时间，默认5ms
            animationDelay = typedArray.getInteger(
                R.styleable.CircleFanMenuViewGroup_animationDelay,
                5
            ).toLong()
            // 菜单项布局ID，默认为0（使用代码创建TextView）
            menuItemLayoutId = typedArray.getResourceId(
                R.styleable.CircleFanMenuViewGroup_menuItemLayoutId,
                R.layout.menu_item_default
            )
            centerScr = typedArray.getResourceId(
                R.styleable.CircleFanMenuViewGroup_centerScr,
                R.mipmap.icon_fan_menu
            )
            // 菜单项图标数组
            val iconsArrayId = typedArray.getResourceId(
                R.styleable.CircleFanMenuViewGroup_menuIcons,
                R.array.menu_icons
            )
            val iconsTypedArray = context.resources.obtainTypedArray(iconsArrayId)
            menuIcons = IntArray(iconsTypedArray.length()) { i ->
                iconsTypedArray.getResourceId(i, R.mipmap.main_tab_z_y)
            }
            iconsTypedArray.recycle()

            // 菜单项文字数组
            val textsArrayId = typedArray.getResourceId(
                R.styleable.CircleFanMenuViewGroup_menuTexts,
                R.array.menu_texts
            )
            menuTexts = context.resources.getStringArray(textsArrayId)
        } finally {
            typedArray.recycle()
        }

        setupViews()
    }

    private fun setupViews() {
        // 创建中心悬浮按钮
        centerButton = FloatingActionButton(context).apply {
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            layoutParams = LayoutParams(buttonSize, buttonSize) // 明确设置48dp尺寸
            setImageResource(centerScr) // 使用指定的图标
            scaleType = ImageView.ScaleType.CENTER_CROP // 图片充满且不变形
            elevation = 0f
            customSize = buttonSize // 强制设置FAB尺寸为48dp

            // 移除FAB的默认padding，让图片真正充满
            setPadding(0, 0, 0, 0)
            setMaxImageSize(customSize)

            setOnClickListener {
                if (!isDragging) {
                    toggleMenu()
                }
            }
        }

        // 创建圆形背景View作为菜单项容器
        backgroundView = FrameLayout(context).apply {
            val bgSize = (backgroundRadius * 2).toInt()
            layoutParams = LayoutParams(bgSize, bgSize)
            background = ContextCompat.getDrawable(context, R.drawable.circle_background)
            alpha = 0f // 初始隐藏
            scaleX = 0f
            scaleY = 0f
        }
        addView(backgroundView)
        addView(centerButton)

        // 直接创建菜单项，数据由自定义布局控制

        // 根据设置的数量创建菜单项，添加到背景View中
        for (i in 0 until menuItemCount) {
            val menuItemView = createMenuItemView(i)
            menuItemViews.add(menuItemView)
            backgroundView.addView(menuItemView)
        }
    }

    /**
     * 创建菜单项View，使用自定义布局
     */
    private fun createMenuItemView(index: Int): View {
        if (menuItemLayoutId == 0) {
            throw IllegalArgumentException("menuItemLayoutId must be specified")
        }

        val inflater = LayoutInflater.from(context)
        val menuItemView = inflater.inflate(menuItemLayoutId, this, false)

        // 根据index从数组中获取对应的图标和文字
        val iconRes = if (index < menuIcons.size) menuIcons[index] else menuIcons[0]
        val textRes = if (index < menuTexts.size) menuTexts[index] else menuTexts[0]

        // 设置基本属性
        menuItemView.apply {
            // 保持布局文件中定义的尺寸，不强制覆盖
            elevation = 6f
            alpha = 0f // 初始透明
            visibility = GONE

            setOnClickListener {
                onMenuItemClickListener?.invoke(index)
//                closeMenu()
            }
        }

        // 设置菜单项数据
        setMenuItemData(menuItemView, iconRes, textRes)

        return menuItemView
    }

    /**
     * 为菜单项设置数组数据
     */
    private fun setMenuItemData(view: View, iconRes: Int, textRes: String) {
        // 如果是TextView，直接设置
        if (view is TextView) {
            view.text = textRes
            view.setTextColor(0xFF333333.toInt())
            view.textSize = 10f

            val drawable = ContextCompat.getDrawable(context, iconRes)
            drawable?.let {
                it.setBounds(
                    0,
                    0,
                    (43 * context.resources.displayMetrics.density).toInt(),
                    (43 * context.resources.displayMetrics.density).toInt()
                )
                view.setCompoundDrawables(null, it, null, null)
            }
            return
        }

        // 如果是复合布局，查找子View设置
        view.findViewById<ImageView>(R.id.menu_icon)?.let { imageView ->
            imageView.setImageResource(iconRes)
        }

        view.findViewById<TextView>(R.id.menu_text)?.let { textView ->
            textView.text = textRes
            textView.setTextColor(0xFF333333.toInt())
            textView.textSize = 10f
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 初始位置在左侧中间
        if (buttonCenterX == 0f && buttonCenterY == 0f) {
            buttonCenterX = buttonSize / 2f // 左边缘，无边距
            buttonCenterY = height / 2f   // 垂直中间位置
            updateCenterButtonPosition()
        }
    }


    fun setOnMenuItemClickListener(listener: (Int) -> Unit) {
        this.onMenuItemClickListener = listener
    }

    /**
     * 更新中心按钮位置
     */
    private fun updateCenterButtonPosition() {
        centerButton.x = buttonCenterX - buttonSize / 2f
        centerButton.y = buttonCenterY - buttonSize / 2f

        // 同时更新背景View位置，保持与中心重合
        backgroundView.x = buttonCenterX - backgroundRadius
        backgroundView.y = buttonCenterY - backgroundRadius
    }

    /**
     * 同步菜单状态，确保背景和菜单项状态一致
     */
    private fun syncMenuState() {
        if (_isMenuOpen) {
            // 展开状态：只确保背景可见，菜单项由动画控制
            backgroundView.visibility = VISIBLE
        } else {
            // 收起状态：确保背景和菜单项都隐藏
            post {
                // 使用post确保动画完成后执行
                menuItemViews.forEach {
                    it.visibility = GONE
                    it.alpha = 0f
                    it.scaleX = 0.3f
                    it.scaleY = 0.3f
                }
                // 背景缩放和透明度重置
                backgroundView.scaleX = 0f
                backgroundView.scaleY = 0f
                backgroundView.alpha = 0f
            }
        }
    }

    /**
     * 计算菜单项位置（在backgroundView内的相对位置）
     * 任务3：左边展开右半球，右边展开左半球，沿半球弧度均匀排列
     */
    private fun calculateMenuItemPositions(): List<PointF> {
        val positions = mutableListOf<PointF>()
        val screenCenterX = width / 2f
        val isOnLeftSide = buttonCenterX < screenCenterX

        // 精确半球展开：180度真正的半球
        val startAngle = if (isOnLeftSide) {
            // 左边吸附 → 展开右半球（从上方开始顺时针到下方）
            270.0 // 从正上方开始
        } else {
            // 右边吸附 → 展开左半球（从上方开始逆时针到下方）
            270.0 // 从正上方开始
        }

        // 菜单项沿180度半球均匀分布
        val angleStep = if (menuItemCount > 1) {
            180.0 / (menuItemCount - 1) // (n-1)个间隔，共180度
        } else {
            0.0 // 只有一个菜单项时放在中间
        }

        for (i in 0 until menuItemCount) {
            val angle = if (isOnLeftSide) {
                // 右半球：270° → 360° → 90°（顺时针）
                (startAngle + i * angleStep) % 360.0
            } else {
                // 左半球：270° → 180° → 90°（逆时针）
                (startAngle - i * angleStep + 360.0) % 360.0
            }

            val rad = Math.toRadians(angle)
            // 计算在backgroundView中的相对位置（backgroundView中心为原点）
            val x = (backgroundRadius + cos(rad) * menuRadius).toFloat()
            val y = (backgroundRadius + sin(rad) * menuRadius).toFloat()
            positions.add(PointF(x, y))
        }

        return positions
    }

    /**
     * 使用属性动画展开菜单
     */
    private fun openMenu() {
        if (_isMenuOpen) return

        // 取消之前的动画协程
        animationJob?.cancel()

        // 设置状态
        _isMenuOpen = true

        // 启动展开动画协程
        animationJob = animationScope.launch {
            openMenuWithCoroutine()
        }
    }

    /**
     * 协程版本的展开动画
     */
    private suspend fun openMenuWithCoroutine() {
        // 手动控制背景和菜单项的显示
        backgroundView.visibility = VISIBLE

        // 保持中心按钮图标不变（总是显示icon_fan_menu）
        // centerButton图标保持不变

        val positions = calculateMenuItemPositions()
        val animators = mutableListOf<Animator>()
        val delayStep = animationDelay // 使用自定义延迟时间

        for (i in menuItemViews.indices) {
            val view = menuItemViews[i]
            val targetPos = positions[i]
            val delay = i * delayStep // 开始时间错开

            view.visibility = VISIBLE
            // 菜单项在backgroundView中的初始位置（backgroundView的中心）
            val currentItemWidth = view.layoutParams.width
            val currentItemHeight = view.layoutParams.height
            view.x = backgroundRadius - currentItemWidth / 2f // backgroundView中心
            view.y = backgroundRadius - currentItemHeight / 2f

            // 位置动画：targetPos是中心点，需要转换为左上角坐标
            val targetX = targetPos.x - currentItemWidth / 2f
            val targetY = targetPos.y - currentItemHeight / 2f

            val translateXAnimator = ObjectAnimator.ofFloat(view, "x", view.x, targetX).apply {
                startDelay = delay
            }
            val translateYAnimator = ObjectAnimator.ofFloat(view, "y", view.y, targetY).apply {
                startDelay = delay
            }

            // 透明度动画
            val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                startDelay = delay
            }

            // 缩放动画
            val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.3f, 1f).apply {
                startDelay = delay
            }
            val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.3f, 1f).apply {
                startDelay = delay
            }

            animators.addAll(
                listOf(
                    translateXAnimator,
                    translateYAnimator,
                    alphaAnimator,
                    scaleXAnimator,
                    scaleYAnimator
                )
            )
        }

        // 添加背景View展开动画
        val bgScaleXAnimator = ObjectAnimator.ofFloat(backgroundView, "scaleX", 0f, 1f)
        val bgScaleYAnimator = ObjectAnimator.ofFloat(backgroundView, "scaleY", 0f, 1f)
        val bgAlphaAnimator = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 1f)
        animators.addAll(listOf(bgScaleXAnimator, bgScaleYAnimator, bgAlphaAnimator))

        // 使用suspendCancellableCoroutine包装动画
        return suspendCancellableCoroutine { continuation ->
            animatorSet?.cancel()
            animatorSet = AnimatorSet().apply {
                playTogether(animators)
                duration = 300
                interpolator = OvershootInterpolator(0.3f)
                doOnEnd {
                    // 展开动画结束
                    continuation.resumeWith(Result.success(Unit))
                }
                start()
            }

            // 协程取消时取消动画
            continuation.invokeOnCancellation {
                animatorSet?.cancel()
            }
        }
    }

    /**
     * 使用属性动画关闭菜单
     */
    private fun closeMenu() {
        if (!_isMenuOpen) return

        // 取消之前的动画协程
        animationJob?.cancel()

        // 设置状态
        _isMenuOpen = false

        // 启动收起动画协程
        animationJob = animationScope.launch {
            closeMenuWithCoroutine()
        }
    }

    /**
     * 协程版本的收起动画
     */
    private suspend fun closeMenuWithCoroutine() {

        // 中心按钮图标始终保持不变（总是icon_fan_menu）
        // centerButton图标保持不变

        val animators = mutableListOf<Animator>()
        val delayStep = animationDelay // 使用自定义延迟时间

        for (i in menuItemViews.indices) {
            val view = menuItemViews[i]
            val delay = i * delayStep // 开始时间错开
            // 菜单项在backgroundView中的中心位置
            val currentItemWidth = view.layoutParams.width
            val currentItemHeight = view.layoutParams.height
            val centerX = backgroundRadius - currentItemWidth / 2f // backgroundView中心
            val centerY = backgroundRadius - currentItemHeight / 2f

            // 位置动画回到中心
            val translateXAnimator = ObjectAnimator.ofFloat(view, "x", view.x, centerX).apply {
                startDelay = delay
            }
            val translateYAnimator = ObjectAnimator.ofFloat(view, "y", view.y, centerY).apply {
                startDelay = delay
            }

            // 透明度动画
            val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f).apply {
                startDelay = delay
            }

            // 缩放动画
            val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, 0.3f).apply {
                startDelay = delay
            }
            val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, 0.3f).apply {
                startDelay = delay
            }

            animators.addAll(
                listOf(
                    translateXAnimator,
                    translateYAnimator,
                    alphaAnimator,
                    scaleXAnimator,
                    scaleYAnimator
                )
            )
        }

        // 添加背景View收起动画
        val bgScaleXAnimator = ObjectAnimator.ofFloat(backgroundView, "scaleX", 1f, 0f)
        val bgScaleYAnimator = ObjectAnimator.ofFloat(backgroundView, "scaleY", 1f, 0f)
        val bgAlphaAnimator = ObjectAnimator.ofFloat(backgroundView, "alpha", 1f, 0f)
        animators.addAll(listOf(bgScaleXAnimator, bgScaleYAnimator, bgAlphaAnimator))

        // 使用suspendCancellableCoroutine包装动画
        return suspendCancellableCoroutine { continuation ->
            animatorSet?.cancel()
            animatorSet = AnimatorSet().apply {
                playTogether(animators)
                duration = 200
                interpolator = AccelerateDecelerateInterpolator()
                doOnEnd {
                    // 动画结束后隐藏菜单项和背景
                    menuItemViews.forEach { it.visibility = GONE }
                    backgroundView.scaleX = 0f
                    backgroundView.scaleY = 0f
                    backgroundView.alpha = 0f

                    continuation.resumeWith(Result.success(Unit))
                }
                start()
            }

            // 协程取消时取消动画
            continuation.invokeOnCancellation {
                animatorSet?.cancel()
            }
        }
    }

    private fun toggleMenu() {
        if (isMenuOpen) {
            closeMenu()
        } else {
            // 展开前检查是否在安全位置
            if (isInSafePosition()) {
                openMenu()
            } else {
                // 不在安全位置，先移动到安全位置再展开
                moveToSafePositionAndOpen()
            }
        }
    }

    /**
     * 检查当前位置是否为安全位置
     */
    private fun isInSafePosition(): Boolean {
        val safePosition = getNearestSafePosition()
        val deltaX = abs(buttonCenterX - safePosition.x)
        val deltaY = abs(buttonCenterY - safePosition.y)
        // 允许5dp的误差范围
        val tolerance = 5f * context.resources.displayMetrics.density
        return deltaX <= tolerance && deltaY <= tolerance
    }

    /**
     * 平滑移动到安全位置后自动展开菜单
     */
    private fun moveToSafePositionAndOpen() {
        val safePosition = getNearestSafePosition()

        edgeAnimator?.cancel()
        edgeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = OvershootInterpolator(0.3f)

            val startX = buttonCenterX
            val startY = buttonCenterY
            val deltaX = safePosition.x - startX
            val deltaY = safePosition.y - startY

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                buttonCenterX = startX + deltaX * progress
                buttonCenterY = startY + deltaY * progress
                updateCenterButtonPosition()
            }
            doOnEnd {
                buttonCenterX = safePosition.x
                buttonCenterY = safePosition.y
                updateCenterButtonPosition()
                // 移动完成后自动展开菜单
                openMenu()
            }
            start()
        }
    }

    /**
     * 智能安全位置调整
     */
    private fun snapToSafePosition() {
        val safePosition = getNearestSafePosition()

        edgeAnimator?.cancel()
        edgeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = OvershootInterpolator(0.3f)

            val startX = buttonCenterX
            val startY = buttonCenterY
            val deltaX = safePosition.x - startX
            val deltaY = safePosition.y - startY

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                buttonCenterX = startX + deltaX * progress
                buttonCenterY = startY + deltaY * progress
                updateCenterButtonPosition()
            }
            doOnEnd {
                buttonCenterX = safePosition.x
                buttonCenterY = safePosition.y
                updateCenterButtonPosition()
            }
            start()
        }
    }

    /**
     * 获取最近的安全位置
     * 任务5：确保半球可完全展开的安全距离计算
     */
    /**
     * 拦截触摸事件，确保拖动功能正常工作
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val distance = sqrt((ev.x - buttonCenterX).pow(2) + (ev.y - buttonCenterY).pow(2))

                // 如果触摸的是悬浮球区域，拦截事件进行拖动处理
                if (distance <= buttonSize / 2f) {
                    return true
                }

                // 如果菜单展开且触摸的是菜单项区域，也拦截
                if (isMenuOpen) {
                    closeMenu()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // 如果已经在拖动，继续拦截
                return isDragging
            }
        }
        return false
    }

    private fun getNearestSafePosition(): PointF {
        // 简化计算：只需确保背景圆形完全在屏幕内
        val safeMinY = backgroundRadius + 20f // 上边界：背景半径 + 安全边距
        val safeMaxY = height - backgroundRadius - 20f // 下边界：屏幕高度 - 背景半径 - 安全边距

        val screenCenterX = width / 2f

        // 吸附到边缘无边距
        val targetX = if (buttonCenterX < screenCenterX) {
            buttonSize / 2f // 完全贴左边，无边距
        } else {
            width - buttonSize / 2f // 完全贴右边，无边距
        }

        // 选择安全的Y位置
        val targetY = when {
            safeMinY >= safeMaxY -> height / 2f // 屏幕太小时使用中心
            buttonCenterY < safeMinY -> safeMinY
            buttonCenterY > safeMaxY -> safeMaxY
            else -> buttonCenterY // 当前位置安全
        }

        return PointF(targetX, targetY)
    }

    /**
     * 优化触摸事件处理
     * 任务6：菜单展开时不支持移动，点击中心或菜单收起
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val distance =
                    sqrt((event.x - buttonCenterX).pow(2) + (event.y - buttonCenterY).pow(2))

                if (distance <= buttonSize / 2f) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    dragStartX = event.x
                    dragStartY = event.y
                    isDragging = false
                    return true
                } else if (isMenuOpen) {
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                // 任务6：菜单展开时完全禁用拖动
                if (isMenuOpen) {
                    return false
                }

                val distance =
                    sqrt((event.x - buttonCenterX).pow(2) + (event.y - buttonCenterY).pow(2))

                if (distance <= buttonSize / 2f || isDragging) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    val totalDelta = sqrt(deltaX.pow(2) + deltaY.pow(2))

                    // 只有在菜单关闭时才允许拖动
                    if (!isDragging && !isMenuOpen && totalDelta > touchSlop) {
                        isDragging = true
                    }

                    if (isDragging && !isMenuOpen) {
                        val minX = buttonSize / 2f
                        val maxX = (width - buttonSize / 2f).coerceAtLeast(minX)
                        val minY = buttonSize / 2f
                        val maxY = (height - buttonSize / 2f).coerceAtLeast(minY)

                        buttonCenterX = (buttonCenterX + deltaX).coerceIn(minX, maxX)
                        buttonCenterY = (buttonCenterY + deltaY).coerceIn(minY, maxY)

                        updateCenterButtonPosition()

                        lastTouchX = event.x
                        lastTouchY = event.y
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    // 任务5：智能安全位置调整
                    snapToSafePosition()
                    isDragging = false
                } else {
                    // 精确点击检测
                    val clickDistance =
                        sqrt((event.x - dragStartX).pow(2) + (event.y - dragStartY).pow(2))
                    if (clickDistance < touchSlop) {
                        val distance =
                            sqrt((event.x - buttonCenterX).pow(2) + (event.y - buttonCenterY).pow(2))
                        if (distance <= buttonSize / 2f) {
                            toggleMenu()
                        }
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 取消所有动画
        animatorSet?.cancel()
        edgeAnimator?.cancel()
        // 取消所有协程，保证生命周期安全
        animationJob?.cancel()
        animationScope.cancel()
    }

    /**
     * 强制关闭菜单
     */
    fun forceCloseMenu() {
        if (isMenuOpen) {
            closeMenu()
        }
    }

    /**
     * 设置悬浮球位置
     */
    fun setButtonPosition(x: Float, y: Float) {
        val minX = buttonSize / 2f
        val maxX = (width - buttonSize / 2f).coerceAtLeast(minX)
        val minY = buttonSize / 2f
        val maxY = (height - buttonSize / 2f).coerceAtLeast(minY)

        buttonCenterX = x.coerceIn(minX, maxX)
        buttonCenterY = y.coerceIn(minY, maxY)
        updateCenterButtonPosition()
    }

    /**
     * 获取当前悬浮球位置
     */
    fun getButtonPosition(): PointF {
        return PointF(buttonCenterX, buttonCenterY)
    }
}