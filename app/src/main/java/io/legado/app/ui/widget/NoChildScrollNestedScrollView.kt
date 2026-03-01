package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

class NoChildScrollNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NestedScrollView(context, attrs) {

    private var isRequestChildFocus = false

    override fun requestChildFocus(child: View?, focused: View?) {
        isRequestChildFocus = true
        super.requestChildFocus(child, focused)
        isRequestChildFocus = false
    }

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect?): Int {
        if (isRequestChildFocus) {
            return 0
        }
        return super.computeScrollDeltaToGetChildRectOnScreen(rect)
    }

}
