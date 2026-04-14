@file:Suppress("unused")

package io.legado.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.os.Build
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EdgeEffect
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.record
import androidx.core.graphics.withTranslation
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import io.legado.app.help.GlideImageGetter
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.TintHelper
import io.legado.app.utils.canvasrecorder.CanvasRecorder
import io.legado.app.utils.canvasrecorder.record
import splitties.systemservices.inputMethodManager
import splitties.views.bottomPadding
import splitties.views.topPadding
import java.lang.reflect.Field
import androidx.core.graphics.createBitmap
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.text.parseAsHtml
import androidx.core.view.postDelayed
import io.legado.app.help.TextViewTagHandler
import io.legado.app.model.analyzeRule.AnalyzeUrl.Companion.paramPattern
import io.noties.markwon.Markwon
import io.noties.markwon.image.AsyncDrawableSpan

/**
 * 专门处理图片触摸事件的 MovementMethod
 * 支持长按和点击事件的配置
 */
private class ImageTouchMovementMethod(
    private val imageSpans: List<ImageSpanInfo>,
    private val onLongClick: (String) -> Unit,
    private val onClick: ((String) -> Unit)? = null,
    private val longPressTimeout: Long = 600L,
    private val clickDebounceMs: Long = 200L
) : android.text.method.LinkMovementMethod() {
    
    private var longClickRunnable: Runnable? = null
    private var isLongClick = false
    private var lastClickTime = 0L
    
    override fun onTouchEvent(
        widget: TextView,
        buffer: android.text.Spannable,
        event: MotionEvent
    ): Boolean {
        val touchedImage = findTouchedImage(widget, event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                cancelLongClick()
                if (touchedImage != null) {
                    isLongClickable = false
                    longClickRunnable = widget.postDelayed(longPressTimeout) {
                        isLongClick = true
                        onLongClick(touchedImage.source)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                isLongClickable = true
                if (!isLongClick && touchedImage != null) {
                    cancelLongClick()
                    onClick?.let { clickCallback ->
                        if (touchedImage.clickUrl != null) {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > clickDebounceMs) {
                                lastClickTime = now
                                clickCallback(touchedImage.clickUrl)
                            }
                        }
                    }
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchedImage != null) {
                    isLongClickable = false
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isLongClickable = true
                cancelLongClick()
                return true
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }
    
    private fun findTouchedImage(widget: TextView, event: MotionEvent): ImageSpanInfo? {
        var x = event.x.toInt()
        var y = event.y.toInt()
        x -= widget.totalPaddingLeft
        y -= widget.totalPaddingTop
        
        val line = widget.layout.getLineForVertical(y)
        val offset = widget.layout.getOffsetForHorizontal(line, x.toFloat())
        
        return imageSpans.find { (range, _, _) ->
            offset in range.first..range.second
        }
    }
    
    private fun cancelLongClick() {
        longClickRunnable?.let {
            longClickRunnable = null
        }
        isLongClick = false
    }
    
    /**
     * 图片 Span 信息
     */
    data class ImageSpanInfo(
        val range: IntRange,
        val source: String,
        val clickUrl: String? = null
    )
}

private tailrec fun getCompatActivity(context: Context?): AppCompatActivity? {
    return when (context) {
        is AppCompatActivity -> context
        is androidx.appcompat.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
        is android.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
        else -> null
    }
}

val View.activity: AppCompatActivity?
    get() = getCompatActivity(context)

fun View.hideSoftInput() = run {
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

fun EditText.showSoftInput() = run {
    requestFocus()
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.disableAutoFill() = run {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
}

fun View.applyTint(
    @ColorInt color: Int,
    isDark: Boolean = AppConfig.isNightTheme
) {
    TintHelper.setTintAuto(this, color, false, isDark)
}

fun View.applyBackgroundTint(
    @ColorInt color: Int,
    isDark: Boolean = AppConfig.isNightTheme
) {
    if (background == null) {
        setBackgroundColor(color)
    } else {
        TintHelper.setTintAuto(this, color, true, isDark)
    }
}

fun RecyclerView.setEdgeEffectColor(@ColorInt color: Int) {
    edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            val edgeEffect = super.createEdgeEffect(view, direction)
            edgeEffect.color = color
            return edgeEffect
        }
    }
}

fun ViewPager.setEdgeEffectColor(@ColorInt color: Int) {
    try {
        val clazz = ViewPager::class.java
        for (name in arrayOf("mLeftEdge", "mRightEdge")) {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            val edge = field.get(this)
            (edge as EdgeEffect).color = color
        }
    } catch (ignored: Exception) {
    }
}

fun EditText.disableEdit() {
    keyListener = null
}

fun View.gone() {
    if (visibility != GONE) {
        visibility = GONE
    }
}

fun View.gone(gone: Boolean) {
    if (gone) {
        gone()
    } else {
        visibility = VISIBLE
    }
}

fun View.invisible() {
    if (visibility != INVISIBLE) {
        visibility = INVISIBLE
    }
}

fun View.visible() {
    if (visibility != VISIBLE) {
        visibility = VISIBLE
    }
}

fun View.visible(visible: Boolean) {
    if (visible && visibility != VISIBLE) {
        visibility = VISIBLE
    } else if (!visible && isVisible) {
        visibility = INVISIBLE
    }
}

fun View.screenshot(bitmap: Bitmap? = null, canvas: Canvas? = null): Bitmap? {
    return if (width > 0 && height > 0) {
        val screenshot = if (bitmap != null && bitmap.width == width && bitmap.height == height) {
            bitmap.eraseColor(Color.TRANSPARENT)
            bitmap
        } else {
            bitmap?.recycle()
            createBitmap(width, height)
        }
        val c = canvas ?: Canvas()
        c.setBitmap(screenshot)
        c.withTranslation(-scrollX.toFloat(), -scrollY.toFloat()) {
            this@screenshot.draw(this)
        }
        c.setBitmap(null)
        screenshot.prepareToDraw()
        screenshot
    } else {
        null
    }
}

fun View.screenshot(picture: Picture) {
    if (width > 0 && height > 0) {
        picture.record(width, height) {
            withTranslation(-scrollX.toFloat(), -scrollY.toFloat()) {
                draw(this)
            }
        }
    }
}

fun View.screenshot(canvasRecorder: CanvasRecorder) {
    if (width > 0 && height > 0) {
        canvasRecorder.record(width, height) {
            draw(this)
        }
    }
}

fun View.setPaddingBottom(bottom: Int) {
    setPadding(paddingLeft, paddingTop, paddingRight, bottom)
}

fun SeekBar.progressAdd(int: Int) {
    progress += int
}

fun RadioGroup.getIndexById(id: Int): Int {
    for (i in 0 until this.childCount) {
        if (id == get(i).id) {
            return i
        }
    }
    return 0
}

fun RadioGroup.getCheckedIndex(): Int {
    for (i in 0 until this.childCount) {
        if (checkedRadioButtonId == get(i).id) {
            return i
        }
    }
    return 0
}

fun RadioGroup.checkByIndex(index: Int) {
    check(get(index).id)
}

fun TextView.setHtml(html: String, imageGetter: GlideImageGetter? = null, textViewTagHandler: TextViewTagHandler? = null) {
    text = html.parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, imageGetter, textViewTagHandler)
}

fun TextView.setHtml(html: String, imageGetter: GlideImageGetter? = null, textViewTagHandler: TextViewTagHandler? = null, imgOnLongClickListener: (source: String) -> Unit, imgOnClickListener: (click: String) -> Unit) {
    val spanned = html.parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, imageGetter, textViewTagHandler)
    val imageSpans = extractImageSpans(spanned)
    text = spanned
    if (imageSpans.isNotEmpty()) {
        movementMethod = ImageTouchMovementMethod(
            imageSpans = imageSpans,
            onLongClick = imgOnLongClickListener,
            onClick = imgOnClickListener
        )
    }
}

/**
 * 从 Spanned 中提取图片 Span 信息
 */
private fun extractImageSpans(spanned: Spanned): List<ImageTouchMovementMethod.ImageSpanInfo> {
    val imageSpans = spanned.getSpans(0, spanned.length, ImageSpan::class.java)
    val result = mutableListOf<ImageTouchMovementMethod.ImageSpanInfo>()
    for (imageSpan in imageSpans) {
        val start = spanned.getSpanStart(imageSpan)
        val end = spanned.getSpanEnd(imageSpan)
        if (start >= 0 && end >= 0) {
            val source = imageSpan.source ?: continue
            var clickUrl: String? = null
            val urlMatcher = paramPattern.matcher(source)
            if (urlMatcher.find()) {
                val urlOptionStr = source.substring(urlMatcher.end())
                GSON.fromJsonObject<Map<String, String>>(urlOptionStr).getOrNull()?.let {
                    clickUrl = it["click"]
                }
            }
            result.add(ImageTouchMovementMethod.ImageSpanInfo(start..end, source, clickUrl))
        }
    }
    return result
}

fun TextView.setMarkdown(markwon: Markwon, spanned: Spanned, imgOnLongClickListener: (source: String) -> Unit) {
    val imageSpans = extractMarkdownImageSpans(spanned)
    if (imageSpans.isNotEmpty()) {
        movementMethod = ImageTouchMovementMethod(
            imageSpans = imageSpans,
            onLongClick = imgOnLongClickListener,
            onClick = null
        )
    }
    markwon.setParsedMarkdown(this, spanned)
}

/**
 * 从 Markdown Spanned 中提取图片 Span 信息
 */
private fun extractMarkdownImageSpans(spanned: Spanned): List<ImageTouchMovementMethod.ImageSpanInfo> {
    val imageSpans = spanned.getSpans(0, spanned.length, AsyncDrawableSpan::class.java)
    val result = mutableListOf<ImageTouchMovementMethod.ImageSpanInfo>()
    for (imageSpan in imageSpans) {
        val start = spanned.getSpanStart(imageSpan)
        val end = spanned.getSpanEnd(imageSpan)
        if (start >= 0 && end >= 0) {
            val source = imageSpan.drawable.destination
            result.add(ImageTouchMovementMethod.ImageSpanInfo(start..end, source, null))
        }
    }
    return result
}

fun TextView.setTextIfNotEqual(charSequence: CharSequence?) {
    if (text != charSequence) {
        text = charSequence
    }
}

@SuppressLint("RestrictedApi")
fun PopupMenu.show(x: Int, y: Int) {
    kotlin.runCatching {
        val field: Field = this.javaClass.getDeclaredField("mPopup")
        field.isAccessible = true
        (field.get(this) as MenuPopupHelper).show(x, y)
    }.onFailure {
        it.printOnDebug()
    }
}

fun View.shouldHideSoftInput(event: MotionEvent): Boolean {
    if (this is EditText) {
        val l = intArrayOf(0, 0)
        getLocationInWindow(l)
        val left = l[0]
        val top = l[1]
        val bottom = top + getHeight()
        val right = left + getWidth()
        return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
    }
    return false
}

fun View.applyStatusBarPadding(withInitialPadding: Boolean = false) {
    val initialPadding = if (withInitialPadding) topPadding else 0
    setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        topPadding = initialPadding + insets.top
        windowInsets
    }
}

fun View.applyNavigationBarPadding(withInitialPadding: Boolean = false) {
    val initialPadding = if (withInitialPadding) bottomPadding else 0
    setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
        bottomPadding = initialPadding + windowInsets.navigationBarHeight
        windowInsets
    }
}

fun View.applyNavigationBarMargin(withInitialMargin: Boolean = false) {
    val initialMargin = if (withInitialMargin) marginBottom else 0
    setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = initialMargin + windowInsets.navigationBarHeight
        }
        windowInsets
    }
}

fun View.setBackgroundKeepPadding(@DrawableRes backgroundResId: Int) {
    val paddingLeft = paddingLeft
    val paddingTop = paddingTop
    val paddingRight = paddingRight
    val paddingBottom = paddingBottom
    setBackgroundResource(backgroundResId)
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
}

fun View.canScroll(direction: Int): Boolean {
    return canScrollVertically(direction) || canScrollHorizontally(direction)
}

private val requestLayoutBroken = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
        || Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.Q

fun View.setOnApplyWindowInsetsListenerCompat(listener: (View, WindowInsetsCompat) -> WindowInsetsCompat) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val windowInsets = listener(view, insets)
        if (requestLayoutBroken && isLayoutRequested) {
            post {
                requestLayout()
            }
        }
        windowInsets
    }
}

fun Spinner.setSelectionSafely(position: Int) {
    val count = adapter?.count ?: 0
    if (count > 0) {
        setSelection(position.coerceIn(0, count - 1))
    }
}