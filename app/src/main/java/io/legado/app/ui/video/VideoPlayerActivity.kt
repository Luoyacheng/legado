package io.legado.app.ui.video

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ActivityVideoPlayerBinding
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.help.gsyVideo.VideoPlayer
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.VideoPlay
import io.legado.app.service.VideoPlayService
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.model.SourceCallBack
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.favorites.RssFavoritesDialog
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.video.config.SettingsDialog
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.gone
import io.legado.app.utils.observeEvent
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setTintMutate
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible

class VideoPlayerActivity : VMBaseActivity<ActivityVideoPlayerBinding, VideoPlayerViewModel>(),
    SettingsDialog.CallBack,RssFavoritesDialog.Callback {
    override val binding by viewBinding(ActivityVideoPlayerBinding::inflate)
    override val viewModel by viewModels<VideoPlayerViewModel>()
    private val playerView: VideoPlayer by lazy { binding.playerView }
    private var starMenuItem: MenuItem? = null
    private var isNew = true
    private var isFullScreen = false
    private var orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var menuCustomBtn: MenuItem? = null
    private val bookSourceEditResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upSource {
                    menuCustomBtn?.isVisible = (VideoPlay.source as? BookSource)?.customButton == true
                }
            }
        }
    private val rssSourceEditResult =
        registerForActivityResult(StartActivityContract(RssSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upSource()
            }
        }
    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            if (it[2] as Boolean) {
                VideoPlay.chapterInVolumeIndex = it[0] as Int
                val durChapterPos = it[1] as Int
                VideoPlay.durVolumeIndex = it[3] as Int
                VideoPlay.chapterInVolumeIndex = it[4] as Int
                VideoPlay.upEpisodes()
                VideoPlay.saveRead(durChapterPos)
                if (VideoPlay.episodes.isNullOrEmpty()) {
                    binding.chapters.visibility = View.GONE
                } else {
                    binding.chapters.visibility = View.VISIBLE
                    val adapter = binding.chapters.adapter as? ChapterAdapter
                    adapter?.updateData(VideoPlay.episodes)
                }
                upView()
                VideoPlay.startPlay(playerView)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        playerView.enlargeImageRes = R.drawable.ic_fullscreen
        isNew = intent.getBooleanExtra("isNew", true)
        if (isNew) {
            intent.getStringExtra("videoUrl")?.let {
                VideoPlay.videoUrl = it
                VideoPlay.singleUrl = true
            }
            intent.getStringExtra("videoTitle")?.let {
                binding.titleBar.title = it
                VideoPlay.videoTitle = it
            }
            val sourceKey = intent.getStringExtra("sourceKey")
            val sourceType = intent.getIntExtra("sourceType", 0)
            val bookUrl = intent.getStringExtra("bookUrl")
            val record = intent.getStringExtra("record")
            VideoPlay.inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            if (!VideoPlay.initSource(sourceKey, sourceType, bookUrl, record)) {
                finish()
                return
            }
            VideoPlay.startPlay(playerView)
            VideoPlay.saveRead()
        } else {
            VideoPlay.clonePlayState(playerView)
            playerView.setSurfaceToPlay()
            playerView.startAfterPrepared()
            binding.titleBar.title = VideoPlay.videoTitle
        }
        setupPlayerView()
        initView()
        upView()
        onBackPressedDispatcher.addCallback(this) {
            if (isFullScreen) {
                toggleFullScreen()
                return@addCallback
            }
            finish()
        }
    }

    private fun initView() {
        viewModel.upStarMenuData.observe(this) { upStarMenu() }
        binding.root.setBackgroundColor(backgroundColor)
        if (VideoPlay.book != null) {
            showBook(VideoPlay.book!!)
            if (VideoPlay.episodes.isNullOrEmpty()) {
                binding.chapters.visibility = View.GONE
            } else {
                binding.chapters.visibility = View.VISIBLE
                showToc(VideoPlay.episodes!!)
            }
            if (VideoPlay.volumes.isEmpty()) {
                binding.volumes.visibility = View.GONE
            } else {
                binding.volumes.visibility = View.VISIBLE
                showVolumes(VideoPlay.volumes)
            }
        } else {
            binding.data.visibility = View.INVISIBLE
            binding.chaptersContainer.visibility = View.INVISIBLE
        }
    }

    private fun showBook(book: Book) {
        binding.run {
            showCover(book)
            tvName.text = book.name
            book.getRealAuthor().takeIf { it.isNotEmpty() }?.let {
                tvAuthor.text = it
            } ?: tvAuthor.gone()
            tvIntro.text = book.getDisplayIntro()
        }
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book, false)
    }

    private fun showToc(toc: List<BookChapter>) {
        binding.ivChapter.setOnClickListener {
            VideoPlay.book?.bookUrl?.let {
                tocActivityResult.launch(it)
            }
        }
        val recyclerView = binding.chapters
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val adapter = ChapterAdapter(toc,VideoPlay.chapterInVolumeIndex, false) { chapter, index ->
            if (index != VideoPlay.chapterInVolumeIndex) {
                VideoPlay.chapterInVolumeIndex = index
                VideoPlay.saveRead(0)
                upEpisodesView()
                VideoPlay.startPlay(playerView)
            }
        }
        recyclerView.adapter = adapter
        scrollToDurChapter(recyclerView, VideoPlay.chapterInVolumeIndex)
    }

    private fun showVolumes(volumes: List<BookChapter>) {
        val recyclerView = binding.volumes
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val adapter = ChapterAdapter(volumes,VideoPlay.durVolumeIndex, true) { chapter, index ->
            if (index != VideoPlay.durVolumeIndex) {
                VideoPlay.durVolumeIndex = index
                VideoPlay.chapterInVolumeIndex = 0
                VideoPlay.upEpisodes()
                if (VideoPlay.episodes.isNullOrEmpty()) {
                    binding.chapters.visibility = View.GONE
                } else {
                    binding.chapters.visibility = View.VISIBLE
                    val adapter = binding.chapters.adapter as? ChapterAdapter
                    adapter?.updateData(VideoPlay.episodes)
                }
                VideoPlay.saveRead(0)
                upVolumesView()
                VideoPlay.startPlay(playerView)
            }
        }
        recyclerView.adapter = adapter
        scrollToDurChapter(recyclerView, VideoPlay.durVolumeIndex)
    }

    private fun scrollToDurChapter(recyclerView: RecyclerView, index: Int) {
        recyclerView.postDelayed({
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.run {
                val smoothScroller = object : LinearSmoothScroller(this@VideoPlayerActivity) {
                    override fun getHorizontalSnapPreference(): Int {
                        return SNAP_TO_START // 滚动到最左边
                    }
                }
                smoothScroller.targetPosition = index
                this.startSmoothScroll(smoothScroller)
            }
            val adapter = recyclerView.adapter as? ChapterAdapter
            adapter?.updateSelectedPosition(index)
        }, 200)
    }

    private fun upView() {
        upEpisodesView()
        upVolumesView()
    }

    private fun upEpisodesView() {
        if (!VideoPlay.episodes.isNullOrEmpty()) {
            scrollToDurChapter(binding.chapters, VideoPlay.chapterInVolumeIndex)
        }
    }

    private fun upVolumesView() {
        if (!VideoPlay.volumes.isEmpty()) {
            scrollToDurChapter(binding.volumes, VideoPlay.durVolumeIndex)
        }
    }

    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        toggleSystemBar(!isFullScreen)
        if (isFullScreen) {
            orientation = requestedOrientation
            requestedOrientation = if (VideoPlay.isPortraitVideo) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT //竖屏
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE //横屏
            }
            supportActionBar?.hide()
            binding.chaptersContainer.gone()
            binding.data.gone()
            playerView.startWindowFullscreen(this, false, false)
        } else {
            requestedOrientation = orientation
            supportActionBar?.show()
            if (VideoPlay.book != null) {
                binding.chaptersContainer.visible()
                binding.data.visible()
            }
            playerView.postDelayed({
                playerView.backFromFull(this)
            }, if (VideoPlay.isPortraitVideo) 300 else 0)
            upView()
        }
    }


    @Suppress("DEPRECATION")
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isFullScreen) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            when (newConfig.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    if (!VideoPlay.isPortraitVideo) {
                        toggleFullScreen()
                    }
                }
                Configuration.ORIENTATION_PORTRAIT -> {
                    if (VideoPlay.isPortraitVideo) {
                        toggleFullScreen()
                    }
                }
            }
        }
    }

    private fun setupPlayerView() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val layoutParams = playerView.layoutParams
        layoutParams.width = screenWidth
        val videoWidth = playerView.currentVideoWidth
        val videoHeight = playerView.currentVideoHeight
        val height = if (videoWidth > 0 && videoHeight > 0) (screenWidth * videoHeight / videoWidth) else (screenWidth * 9 / 16) //默认16:9
        //高度不超过一半屏幕
        layoutParams.height = if (height < screenHeight / 2) height else screenHeight / 2
        playerView.layoutParams = layoutParams
        playerView.isNeedOrientationUtils = false //关闭自带的屏幕方向控制
        playerView.fullscreenButton.setOnClickListener { toggleFullScreen() }
        playerView.setBackFromFullScreenListener { toggleFullScreen() }
        playerView.setVideoAllCallBack(object : GSYSampleCallBack() {
            @SuppressLint("SourceLockedOrientationActivity")
            override fun onPrepared(url: String?, vararg objects: Any?) {
                super.onPrepared(url, *objects)
                playerView.post {
                    val player = playerView.getCurrentPlayer()
                    if (VideoPlay.lockCurScreen &&  !player.getLockCurScreen()) {
                        player.lockTouchLogic()
                    }
                    //根据实际视频比例再次调整
                    val videoWidth = playerView.currentVideoWidth
                    val videoHeight = playerView.currentVideoHeight
                    if (videoWidth > 0 && videoHeight > 0) {
                        val layoutParams = playerView.layoutParams
                        val parentWidth = playerView.width
                        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                        val isPortraitVideo = if (aspectRatio > 1.2) true else false
                        VideoPlay.isPortraitVideo = isPortraitVideo
                        if (isFullScreen && isPortraitVideo) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT //提前进入了全屏，并且默认横屏了，纠正回来
                            return@post
                        }
                        if (VideoPlay.startFull && VideoPlay.autoPlay && !isFullScreen) {
                            toggleFullScreen()
                            return@post
                        }
                        val height = (parentWidth * aspectRatio).toInt()
                        val displayMetrics = resources.displayMetrics
                        val screenHeight = displayMetrics.heightPixels
                        //高度不超过一半屏幕
                        layoutParams.height = if (height < screenHeight / 2) height else screenHeight / 2
                        playerView.layoutParams = layoutParams
                    }
                }
            }
        })
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.video_play, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menuCustomBtn = menu.findItem(R.id.menu_custom_btn)?.also {
            it.isVisible = (VideoPlay.source as? BookSource)?.customButton == true
        }
        starMenuItem = menu.findItem(R.id.menu_rss_star)
        upStarMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun upStarMenu() {
        if (VideoPlay.rssStar != null) {
            starMenuItem?.isVisible = true
            starMenuItem?.setIcon(R.drawable.ic_star)
            starMenuItem?.setTitle(R.string.in_favorites)
            starMenuItem?.icon?.setTintMutate(primaryTextColor)
        } else if(VideoPlay.rssRecord != null) {
            starMenuItem?.isVisible = true
            starMenuItem?.setIcon(R.drawable.ic_star_border)
            starMenuItem?.setTitle(R.string.out_favorites)
            starMenuItem?.icon?.setTintMutate(primaryTextColor)
        } else {
            starMenuItem?.isVisible = false
        }
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !VideoPlay.source?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_custom_btn -> {
                (VideoPlay.source as? BookSource)?.let {source ->
                    VideoPlay.book?.let { book ->
                        SourceCallBack.callBackBtn(this, SourceCallBack.CLICK_CUSTOM_BUTTON, source, book, VideoPlay.chapter)
                    }
                }
            }
            R.id.menu_rss_star -> viewModel.addFavorite {
                VideoPlay.rssStar?.let { showDialogFragment(RssFavoritesDialog(it)) }
            }
            R.id.menu_float_window -> startFloatingWindow()
            R.id.menu_config_settings -> showDialogFragment(SettingsDialog(this))
            R.id.menu_login -> VideoPlay.source?.let {s ->
               when (s) {
                    is BookSource -> {
                        startActivity<SourceLoginActivity> {
                            putExtra("bookType", BookType.video)
                        }
                    }
                    is RssSource -> {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "rssSource")
                            putExtra("key", s.getKey())
                        }
                    }
                }
            }

            R.id.menu_copy_video_url -> {
                val url = VideoPlay.videoUrl
                if (url.isNullOrBlank()){
                    this.toastOnUi("暂无播放地址")
                    return true
                }
                VideoPlay.book?.let {
                    SourceCallBack.callBackBtn(this, SourceCallBack.CLICK_COPY_PLAY_URL, VideoPlay.source as? BookSource, it, VideoPlay.chapter) {
                        sendToClip(url)
                    }
                }
            }
            R.id.menu_open_other_video_player -> {
                val url = VideoPlay.videoUrl
                if (url.isNullOrBlank()){
                    this.toastOnUi("暂无播放地址")
                    return true
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(url.toUri(), "video/*")
                }
                startActivity(intent)
            }
            R.id.menu_edit_source -> VideoPlay.source?.let {s  ->
                when (s) {
                    is BookSource -> bookSourceEditResult.launch {
                        putExtra("sourceUrl", s.getKey())
                    }
                    is RssSource -> rssSourceEditResult.launch {
                        putExtra("sourceUrl", s.getKey())
                    }
                }
            }

            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun startFloatingWindow() {
        VideoPlay.savePlayState(playerView)
        // 启动悬浮窗服务
        val intent = Intent(this, VideoPlayService::class.java).apply {
            putExtra("isNew", false)
        }
        ContextCompat.startForegroundService(this, intent)
        playerView.needDestroy = false
        finish() //如果在播放器复刻前活动被销毁，会导致状态继承异常（这里服务创建很快，没发现异常）
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoPlay.saveRead()
        VideoPlay.stopLoading()
        playerView.getCurrentPlayer().release()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun observeLiveBus() {

        observeEventSticky<String>(EventBus.VIDEO_SUB_TITLE) {
            binding.titleBar.title = it
        }

        observeEvent<ArrayList<Int>>(EventBus.UP_VIDEO_INFO) {
            it.forEach { value ->
                when (value) {
                    1 -> upEpisodesView()
                }
            }
        }

    }

    override fun finish() {
        val book = VideoPlay.book ?: return super.finish()
        if (VideoPlay.inBookshelf) {
            callBackBookEnd()
            return super.finish()
        }
        if (!AppConfig.showAddToShelfAlert) {
            callBackBookEnd()
            viewModel.removeFromBookshelf { super.finish() }
        } else {
            alert(title = getString(R.string.add_to_bookshelf)) {
                setMessage(getString(R.string.check_add_bookshelf, book.name))
                okButton {
                    VideoPlay.book?.removeType(BookType.notShelf)
                    VideoPlay.book?.save()
                    VideoPlay.inBookshelf = true
                    setResult(RESULT_OK)
                }
                noButton {
                    callBackBookEnd()
                    viewModel.removeFromBookshelf { super.finish() }
                }
            }
        }
    }

    private fun callBackBookEnd() {
        SourceCallBack.callBackBook(SourceCallBack.END_READ, VideoPlay.source as BookSource?, VideoPlay.book, VideoPlay.chapter)
    }

    override fun updateFavorite(title: String?, group: String?) {
        viewModel.updateFavorite(title, group)
    }

    override fun deleteFavorite() {
        viewModel.delFavorite()
    }
}