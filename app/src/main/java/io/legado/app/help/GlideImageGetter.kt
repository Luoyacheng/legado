package io.legado.app.help

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Size
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.legado.app.model.analyzeRule.AnalyzeUrl.Companion.paramPattern
import io.legado.app.utils.GSON
import io.legado.app.utils.dpToPx
import io.legado.app.utils.fromJsonObject
import java.lang.ref.WeakReference
import io.legado.app.utils.lifecycle
import java.io.ByteArrayInputStream
import kotlin.io.encoding.Base64
import io.legado.app.utils.SvgUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.dropLast
import kotlin.text.endsWith
import kotlin.text.toIntOrNull
import androidx.core.graphics.drawable.toDrawable
import android.graphics.Color

class GlideImageGetter(
    context: Context,
    textView: TextView,
    private val lifecycle: Lifecycle
) : Html.ImageGetter, Drawable.Callback {
    private val textViewRef = WeakReference(textView)
    private val contextRef = WeakReference(context)
    private val cacheDrawable = ConcurrentHashMap<String, GlideUrlDrawable>()
    private val pendingImages = mutableSetOf<String>()
    private val emptyDrawable by lazy {
        Color.TRANSPARENT.toDrawable()
    }
    private val availableWidth by lazy {
        val textView = textViewRef.get() ?: return@lazy 800
        textView.width - textView.paddingLeft - textView.paddingRight - 8.dpToPx() //8是为了文字对齐额外的右边距
    }

    override fun getDrawable(source: String?): Drawable {
        val context = contextRef.get()
        if (context == null || source.isNullOrBlank()) {
            return emptyDrawable
        }
        var style: Map<String, String>? = null
        if (source.startsWith("data:image/svg")) {
            var data: String? = null
            val urlMatcher = paramPattern.matcher(source)
            if (urlMatcher.find()) {
                val urlOptionStr = source.substring(urlMatcher.end())
                style = GSON.fromJsonObject<Map<String, String>>(urlOptionStr).getOrNull()
                data = source.take(urlMatcher.start())
            }
            val inputStream =
                ByteArrayInputStream(Base64.decode((data ?: source).substringAfter(",")))
            val (pictureDrawable, size) = SvgUtils.createDrawable(inputStream)
                ?: return emptyDrawable
            val rect = getDrawableRect(size, style)
            pictureDrawable.bounds = rect
            return pictureDrawable
        }
        cacheDrawable[source]?.let {
            return it
        }
        val urlDrawable = GlideUrlDrawable()
        cacheDrawable[source] = urlDrawable
        pendingImages.add(source)
        val urlMatcher = paramPattern.matcher(source)
        if (urlMatcher.find()) {
            val urlOptionStr = source.substring(urlMatcher.end())
            style = GSON.fromJsonObject<Map<String, String>>(urlOptionStr).getOrNull()
        }
        val target = ImageTarget(urlDrawable, source, style)
        Glide.with(context).lifecycle(lifecycle)
            .load(source)
            .into(target)
        return urlDrawable
    }

    private fun getDrawableRect(size: Size, style: Map<String, String>?): Rect {
        val drawableWidth = size.width.coerceAtLeast(1)
        val drawableHeight = size.height.coerceAtLeast(1)
        if (style == null) {
            return Rect(0, 0, drawableWidth, drawableHeight)
        }
        val styleWidth = style["width"]
        val styleType = style["style"]
        if (styleWidth == null && styleType == null) {
            return Rect(0, 0, drawableWidth, drawableHeight)
        }
        val imgWidth = if (styleWidth?.endsWith("%") == true) {
            val sWidth = styleWidth.dropLast(1).toIntOrNull() ?: 80
            availableWidth * sWidth / 100
        } else {
            val sWidth = styleWidth?.toIntOrNull() ?: 0
            if (sWidth > 0) {
                sWidth
            } else {
                drawableWidth
            }
        }
        val showWidth = availableWidth.takeIf { it in 1..<imgWidth } ?: imgWidth
        val showHeight = (drawableHeight * showWidth / drawableWidth.toFloat()).toInt()
        val left = when (styleType) {
            "center" -> (availableWidth - showWidth) / 2
            "right" -> availableWidth - showWidth
            else -> 0
        }
        return Rect(left, 0, left + showWidth, showHeight)
    }

    private fun notifyImageLoaded(source: String) {
        pendingImages.remove(source)
        if (pendingImages.isEmpty()) {
            val textView = textViewRef.get() ?: return
            textView.text = textView.text
        }
    }

    override fun invalidateDrawable(who: Drawable) {
        textViewRef.get()?.invalidate()
    }

    override fun scheduleDrawable(
        who: Drawable,
        what: Runnable,
        `when`: Long
    ) {
    }

    override fun unscheduleDrawable(
        who: Drawable,
        what: Runnable
    ) {
    }

    private inner class GlideUrlDrawable() : Drawable(), Animatable {
        private var mDrawable: Drawable? = null
        private var gDrawable: GifDrawable? = null

        fun setDrawable(drawable: Drawable?) {
            if (drawable is GifDrawable) {
                gDrawable?.apply {
                    callback = null
                    stop()
                }
                gDrawable = drawable.apply {
                    if (!isRunning) {
                        callback = this@GlideImageGetter
                        start()
                    }
                }
                mDrawable = null
            } else {
                gDrawable?.apply {
                    callback = null
                    stop()
                }
                gDrawable = null
                mDrawable = drawable
            }
        }

        fun clear() {
            gDrawable?.apply {
                callback = null
                stop()
            }
            gDrawable = null
            mDrawable = null
        }

        override fun draw(canvas: Canvas) {
            (mDrawable ?: gDrawable)?.draw(canvas)
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            (mDrawable ?: gDrawable)?.bounds = bounds
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            val drawable = mDrawable ?: gDrawable
            return if (drawable != null) {
                when {
                    drawable.alpha == 0 -> PixelFormat.TRANSPARENT
                    drawable.alpha == 255 -> PixelFormat.OPAQUE
                    else -> PixelFormat.TRANSLUCENT
                }
            } else {
                PixelFormat.TRANSLUCENT
            }
        }

        override fun setAlpha(alpha: Int) {
            mDrawable?.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            mDrawable?.colorFilter = colorFilter
        }

        override fun getIntrinsicWidth(): Int {
            return (mDrawable ?: gDrawable)?.intrinsicWidth ?: 0
        }

        override fun getIntrinsicHeight(): Int {
            return (mDrawable ?: gDrawable)?.intrinsicHeight ?: 0
        }

        override fun isRunning(): Boolean {
            return gDrawable?.isRunning == true
        }

        override fun start() {
            gDrawable?.start()
        }

        override fun stop() {
            gDrawable?.stop()
        }
    }

    private inner class ImageTarget(
        private val urlDrawable: GlideUrlDrawable,
        private val source: String,
        private val style: Map<String, String>?
    ) : CustomTarget<Drawable>() {

        override fun onResourceReady(
            drawable: Drawable,
            transition: Transition<in Drawable>?
        ) {
            urlDrawable.setDrawable(drawable)
            val rect =
                getDrawableRect(Size(drawable.intrinsicWidth, drawable.intrinsicHeight), style)
            urlDrawable.bounds = rect
            notifyImageLoaded(source)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            urlDrawable.clear()
            cacheDrawable.remove(source)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            urlDrawable.setDrawable(errorDrawable)
            notifyImageLoaded(source)
        }
    }

    fun start() {
        cacheDrawable.values.forEach { drawable ->
            drawable.start()
        }
    }

    fun stop() {
        cacheDrawable.values.forEach { drawable ->
            drawable.stop()
        }
    }

    fun clear() {
        cacheDrawable.values.forEach { drawable ->
            drawable.clear()
        }
        cacheDrawable.clear()
        contextRef.clear()
        textViewRef.clear()
    }
}