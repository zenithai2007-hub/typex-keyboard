package com.zenithai.typex.ime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Canvas-drawn keyboard view. No XML key inflation, no per-key View objects —
 * this is what keeps typing at 60fps even on low-end devices.
 */
class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var keyListener: ((code: Int, label: String) -> Unit)? = null

    var rows: List<List<KeyDef>> = emptyList()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var theme: KeyboardTheme = KeyboardThemes.TYPEX
        set(value) {
            field = value
            iconCache.clear()
            invalidate()
        }

    /** Shown on the enter key for IME actions (Go/Search/Send/Done/Next); null = icon. */
    var enterLabel: String? = null
        set(value) {
            field = value
            invalidate()
        }

    var shiftState: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private var keyRects: List<List<RectF>> = emptyList()
    private var pressedKey: KeyDef? = null
    private var downKey: KeyDef? = null
    private var longPressFired = false

    private val handler = Handler(Looper.getMainLooper())
    private val iconCache = mutableMapOf<Int, Bitmap>()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var previewPopup: PopupWindow? = null
    private var previewText: TextView? = null

    private val longPressRunnable = Runnable {
        longPressFired = true
        when (downKey?.code) {
            KeyCodes.DELETE -> handler.post(deleteRepeatRunnable)
            KeyCodes.SHIFT -> keyListener?.invoke(KeyCodes.CAPS_LOCK, "")
        }
        dismissPreview()
    }

    private val deleteRepeatRunnable = object : Runnable {
        override fun run() {
            keyListener?.invoke(KeyCodes.DELETE, "")
            handler.postDelayed(this, 55)
        }
    }

    private fun keyHeight(): Float {
        val landscape =
            resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        return dp(if (landscape) 42f else 58f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desiredH = (keyHeight() * maxOf(rows.size, 1) + paddingTop + paddingBottom).toInt()
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> min(desiredH, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredH
        }
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys(w, h)
    }

    private fun layoutKeys(w: Int, h: Int) {
        if (rows.isEmpty() || w == 0) {
            keyRects = emptyList()
            return
        }
        val rowH = (h - paddingTop - paddingBottom).toFloat() / rows.size
        val gap = dp(3f)
        keyRects = rows.mapIndexed { r, row ->
            val totalWeight = row.sumOf { it.width.toDouble() }.toFloat().coerceAtLeast(1f)
            var x = paddingLeft.toFloat()
            row.map { key ->
                val kw = (w - paddingLeft - paddingRight).toFloat() * (key.width / totalWeight)
                val rect = RectF(
                    x + gap / 2, paddingTop + r * rowH + gap / 2,
                    x + kw - gap / 2, paddingTop + (r + 1) * rowH - gap / 2
                )
                x += kw
                rect
            }
        }
    }

    private fun findKey(x: Float, y: Float): Pair<KeyDef, RectF>? {
        keyRects.forEachIndexed { r, row ->
            row.forEachIndexed { c, rect ->
                if (rect.contains(x, y)) return rows[r][c] to rect
            }
        }
        return null
    }

    private fun iconFor(res: Int, color: Int): Bitmap {
        return iconCache.getOrPut(res * 31 + color) {
            val d: Drawable = ContextCompat.getDrawable(context, res)!!.mutate()
            d.setTint(color)
            val size = dp(24f).toInt()
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            d.setBounds(0, 0, size, size)
            d.draw(canvas)
            bmp
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgPaint.color = theme.background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        if (rows.isEmpty() || keyRects.isEmpty()) return
        val radius = dp(6f)
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, key ->
                if (c >= keyRects[r].size) return@forEachIndexed
                val rect = keyRects[r][c]
                val isEnter = key.code == KeyCodes.ENTER
                when {
                    isEnter -> keyPaint.color = theme.accent
                    key.special -> keyPaint.color = theme.specialKeyBackground
                    else -> keyPaint.color = theme.keyBackground
                }
                if (key == pressedKey) {
                    // pressed highlight: blend toward accent
                    keyPaint.color = blend(keyPaint.color, theme.accent, 0.35f)
                }
                canvas.drawRoundRect(rect, radius, radius, keyPaint)

                if (key.iconRes != 0 && !(isEnter && enterLabel != null)) {
                    val iconColor = if (isEnter) theme.previewText else theme.specialKeyText
                    val bmp = iconFor(key.iconRes, iconColor)
                    canvas.drawBitmap(
                        bmp,
                        rect.centerX() - bmp.width / 2f,
                        rect.centerY() - bmp.height / 2f,
                        null
                    )
                    if (key.code == KeyCodes.SHIFT && shiftState != 0) {
                        dotPaint.color = theme.accent
                        canvas.drawCircle(rect.centerX(), rect.bottom - dp(6f), dp(3f), dotPaint)
                    }
                } else {
                    val label = if (isEnter && enterLabel != null) enterLabel!! else key.label
                    textPaint.color = if (isEnter) theme.previewText
                    else if (key.special) theme.specialKeyText else theme.keyText
                    textPaint.textSize =
                        if (label.length <= 1) dp(22f) else dp(13f)
                    textPaint.isFakeBoldText = key.special
                    val y = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(label, rect.centerX(), y, textPaint)
                }
            }
        }
    }

    private fun blend(a: Int, b: Int, ratio: Float): Int {
        val ar = (a shr 16) and 0xFF; val ag = (a shr 8) and 0xFF; val ab = a and 0xFF
        val br = (b shr 16) and 0xFF; val bg = (b shr 8) and 0xFF; val bb = b and 0xFF
        return (0xFF shl 24) or
            ((ar + ((br - ar) * ratio)).toInt() shl 16) or
            ((ag + ((bg - ag) * ratio)).toInt() shl 8) or
            (ab + ((bb - ab) * ratio)).toInt()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = findKey(event.x, event.y) ?: return false
                val (key, rect) = hit
                downKey = key
                pressedKey = key
                longPressFired = false
                invalidate()
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (key.code == KeyCodes.DELETE) {
                    // fire immediately, then repeat on long-press
                    keyListener?.invoke(KeyCodes.DELETE, "")
                    handler.postDelayed(longPressRunnable, 400)
                } else {
                    if (key.code == KeyCodes.SHIFT) {
                        handler.postDelayed(longPressRunnable, 550)
                    }
                    if (isPreviewable(key)) showPreview(key, rect)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val hit = findKey(event.x, event.y)
                val key = hit?.first
                if (key != pressedKey) {
                    cancelLongPress()
                    pressedKey = key
                    if (key != null && isPreviewable(key) && hit != null) showPreview(key, hit.second)
                    else dismissPreview()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasPress = pressedKey
                val fire = event.actionMasked == MotionEvent.ACTION_UP &&
                    wasPress != null && wasPress == downKey && !longPressFired &&
                    wasPress.code != KeyCodes.DELETE && wasPress.code != 0
                cancelLongPress()
                dismissPreview()
                pressedKey = null
                downKey = null
                invalidate()
                if (fire) keyListener?.invoke(wasPress!!.code, wasPress.label)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isPreviewable(key: KeyDef): Boolean =
        key.code != 0 && key.iconRes == 0 && key.label.length == 1 &&
            key.code != KeyCodes.SPACE && key.code != KeyCodes.ENTER

    private fun showPreview(key: KeyDef, rect: RectF) {
        dismissPreview()
        val tv = TextView(context).apply {
            text = key.label
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(theme.previewText)
            setBackgroundColor(theme.previewBackground)
            val p = dp(10f).toInt()
            setPadding(p, dp(4f).toInt(), p, dp(4f).toInt())
        }
        previewText = tv
        val popup = PopupWindow(tv, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        popup.isClippingEnabled = false
        previewPopup = popup
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        tv.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val x = (loc[0] + rect.centerX() - tv.measuredWidth / 2f).toInt()
        val y = (loc[1] + rect.top - tv.measuredHeight - dp(8f)).toInt()
        popup.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
    }

    private fun dismissPreview() {
        previewPopup?.dismiss()
        previewPopup = null
        previewText = null
    }

    private fun cancelLongPress() {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(deleteRepeatRunnable)
        longPressFired = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelLongPress()
        dismissPreview()
    }
}
