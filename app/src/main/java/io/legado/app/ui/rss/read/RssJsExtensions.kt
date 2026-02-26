package io.legado.app.ui.rss.read

import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.script.rhino.runScriptWithContext
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.help.JsExtensions
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.ui.association.AddToBookshelfDialog
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.openUrl
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URL


@Suppress("unused")
open class RssJsExtensions(activity: AppCompatActivity?, source: BaseSource?) : JsExtensions {

    val activityRef: WeakReference<AppCompatActivity> = WeakReference(activity)
    val sourceRef: WeakReference<BaseSource?> = WeakReference(source)

    override fun getSource(): BaseSource? {
        return sourceRef.get()
    }

    override fun getTag(): String? {
        return getSource()?.getTag()
    }

    @JavascriptInterface
    fun put(key: String, value: String): String {
        getSource()?.put(key, value)
        return value
    }

    @JavascriptInterface
    fun get(key: String): String {
        return getSource()?.get(key) ?: ""
    }

    @JavascriptInterface
    fun searchBook(key: String) {
        searchBook(key, null)
    }

    @JavascriptInterface
    fun searchBook(key: String, searchScope: String?) {
        activityRef.get()?.let {
            SearchActivity.start(it, key, searchScope)
        }
    }

    @JavascriptInterface
    fun addBook(bookUrl: String) {
        activityRef.get()?.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

    @JavascriptInterface
    fun showPhoto(src: String) {
        activityRef.get()?.showDialogFragment(PhotoDialog(src, getSource()?.getKey()))
    }


    @JavascriptInterface
    @JvmOverloads
    fun open(name: String, url: String? = null, title: String? = null, origin: String? = null) {
        val activity = activityRef.get() ?: return
        activity.lifecycleScope.launch(IO) {
            val source = getSource() ?: return@launch
            when (name) {
                "login" -> {
                    if (activity is SourceLoginActivity) {
                        activity.toastOnUi("已在登录界面")
                        return@launch
                    }
                    val toSource = origin?.let { o ->
                        appDb.bookSourceDao.getBookSource(o)
                    } ?: source
                    if (toSource.loginUrl.isNullOrBlank()) {
                        activity.toastOnUi("源未配置登录")
                        return@launch
                    }
                    when (toSource) {
                        is BookSource -> {
                            withContext(Main) {
                                activity.startActivity<SourceLoginActivity> {
                                    putExtra("type", "bookSource")
                                    putExtra("key", toSource.bookSourceUrl)
                                }
                            }
                        }

                        is RssSource -> {
                            withContext(Main) {
                                activity.startActivity<SourceLoginActivity> {
                                    putExtra("type", "rssSource")
                                    putExtra("key", toSource.sourceUrl)
                                }
                            }
                        }
                    }
                }

                "sort" -> {
                    val toSource = origin?.let { o ->
                        appDb.rssSourceDao.getByKey(o)
                    } ?: (source as? RssSource) ?: return@launch
                    val sortUrl = if (url.isJsonObject()) {
                        url
                    } else {
                        title?.let {
                            JSONObject().put(title, url).toString()
                        } ?: url
                    }
                    val sourceUrl = toSource.sourceUrl
                    withContext(Main) {
                        RssSortActivity.start(activity, sortUrl, sourceUrl)
                    }
                }

                "rss" -> {
                    val toSource = origin?.let { o ->
                        appDb.rssSourceDao.getByKey(o)
                    } ?: (source as? RssSource) ?: return@launch
                    val title = title ?: toSource.sourceName
                    val sourceUrl = toSource.sourceUrl
                    if (url.isNullOrBlank()) {
                        if (toSource.singleUrl) {
                            if (sourceUrl.startsWith("http", true)) {
                                withContext(Main) {
                                    ReadRssActivity.start(
                                        activity,
                                        origin.isNullOrEmpty(),
                                        sourceUrl,
                                        title
                                    )
                                }
                            } else {
                                activity.openUrl(sourceUrl)
                            }
                            return@launch
                        }
                        val startHtml = toSource.startHtml?.let {
                            when {
                                it.startsWith("@js:") -> runScriptWithContext {
                                    toSource.evalJS(it.substring(4)).toString()
                                }

                                it.startsWith("<js>") -> runScriptWithContext {
                                    toSource.evalJS(it.substring(4, it.lastIndexOf("<"))).toString()
                                }

                                else -> it
                            }
                        }
                        withContext(Main) {
                            if (startHtml.isNullOrBlank()) {
                                activity.startActivity<RssSortActivity> {
                                    putExtra("sourceUrl", sourceUrl)
                                }
                            } else {
                                ReadRssActivity.start(
                                    activity,
                                    origin.isNullOrEmpty(),
                                    sourceUrl,
                                    title,
                                    startHtml = startHtml
                                )
                            }
                        }
                        return@launch
                    }
                    val rss =appDb.rssStarDao.get(sourceUrl, url)?.toRecord() ?: appDb.rssArticleDao.getByLink(sourceUrl, url)?.toRecord()
                    val rssReadRecord = rss ?: RssReadRecord(
                        record = url,
                        title = title,
                        origin = sourceUrl,
                        readTime = System.currentTimeMillis()
                    )
                    appDb.rssReadRecordDao.insertRecord(rssReadRecord) //留下历史记录
                    withContext(Main) {
                        ReadRssActivity.start(
                            activity,
                            origin.isNullOrEmpty(),
                            sourceUrl,
                            title,
                            url
                        )
                    }
                }

                "search" -> {
                    title?.let {
                        val searchScope = origin?.let { o  ->
                            appDb.bookSourceDao.getBookSource(o)?.let { s ->
                                "${s.bookSourceName.replace(":", "")}::${o}"
                            }
                        }
                        withContext(Main) {
                            searchBook(it, searchScope)
                        }
                    }
                }

                "explore" -> {
                    val toSource = origin?.let { o ->
                        appDb.bookSourceDao.getBookSource(o)
                    } ?: (source as? BookSource) ?: return@launch
                    val sourceUrl = toSource.bookSourceUrl
                    withContext(Main) {
                        activity.startActivity<ExploreShowActivity> {
                            putExtra("exploreName", title)
                            putExtra("sourceUrl", sourceUrl)
                            putExtra("exploreUrl", url)
                        }
                    }
                }
            }
        }
    }

    /** AnalyzeRule实现 **/
    open val analyzeRule by lazy { AnalyzeRule(source = getSource()) }

    @JavascriptInterface
    @JvmOverloads
    fun setContent(content: Any?, baseUrl: String? = null): AnalyzeRule {
        return analyzeRule.setContent(content, baseUrl)
    }

    @JavascriptInterface
    fun setBaseUrl(baseUrl: String?): AnalyzeRule {
        return analyzeRule.setBaseUrl(baseUrl)
    }

    @JavascriptInterface
    fun setRedirectUrl(url: String): URL? {
        return analyzeRule.setRedirectUrl(url)
    }

    @JvmOverloads
    fun getStringList(rule: String?, mContent: Any? = null, isUrl: Boolean = false): List<String>? {
        return analyzeRule.getStringList(rule, mContent, isUrl)
    }

    @JvmOverloads
    fun getString(ruleStr: String?, mContent: Any? = null, isUrl: Boolean = false): String {
        return analyzeRule.getString(ruleStr, mContent, isUrl)
    }

    @JavascriptInterface
    fun getString(ruleStr: String?, unescape: Boolean): String {
        return analyzeRule.getString(ruleStr, unescape)
    }

    fun getElement(ruleStr: String): Any? {
        return analyzeRule.getElement(ruleStr)
    }

    fun getElements(ruleStr: String): List<Any> {
        return analyzeRule.getElements(ruleStr)
    }

}
