package io.legado.app.help

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress
import io.legado.app.help.config.AppConfig
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.UrlUtil
import io.legado.app.utils.FileDoc
import io.legado.app.utils.createFileIfNotExist
import io.legado.app.utils.createFolderIfNotExist
import io.legado.app.utils.exists
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.isJson
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.list
import io.legado.app.utils.normalizeFileName
import io.legado.app.utils.delete
import io.legado.app.utils.inputStream
import io.legado.app.utils.outputStream
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.InputStreamReader

object AppLocalSync {

    private const val bookProgressDirName = "bookProgress"

    val isOk: Boolean
        get() {
            val path = AppConfig.backupPath ?: return false
            return try {
                val uri = Uri.parse(path)
                appCtx.contentResolver.persistedUriPermissions.any {
                    it.uri == uri && it.isWritePermission
                }
            } catch (e: Exception) {
                false
            }
        }

    private val localSyncUri: Uri?
        get() {
            val path = AppConfig.backupPath ?: return null
            return try {
                Uri.parse(path)
            } catch (e: Exception) {
                null
            }
        }

    private fun getProgressFileName(name: String, author: String): String {
        return UrlUtil.replaceReservedChar("${name}_${author}".normalizeFileName()) + ".json"
    }

    suspend fun uploadBookProgress(
        book: Book,
        toast: Boolean = false,
        onSuccess: (() -> Unit)? = null
    ) {
        if (!isOk) return
        if (!AppConfig.localAutoSyncProgress) return
        val uri = localSyncUri ?: return
        if (!uri.isContentScheme()) return

        try {
            val bookProgress = BookProgress(book)
            val json = GSON.toJson(bookProgress)
            val fileName = getProgressFileName(book.name, book.author)

            withContext(IO) {
                val rootDoc = DocumentFile.fromTreeUri(appCtx, uri)!!
                val bookProgressDirDoc = DocumentUtils.createFolderIfNotExist(rootDoc, bookProgressDirName)!!
                val fileDoc = DocumentUtils.createFileIfNotExist(bookProgressDirDoc, fileName)
                fileDoc?.uri?.outputStream(appCtx)?.getOrNull()?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                }
                book.syncTime = System.currentTimeMillis()
            }
            onSuccess?.invoke()
        } catch (e: Exception) {
            AppLog.put("上传进度到本地失败\n${e.localizedMessage}", e, toast)
        }
    }

    suspend fun uploadBookProgress(
        bookProgress: BookProgress,
        onSuccess: (() -> Unit)? = null
    ) {
        if (!isOk) return
        if (!AppConfig.localAutoSyncProgress) return
        val uri = localSyncUri ?: return
        if (!uri.isContentScheme()) return

        try {
            val json = GSON.toJson(bookProgress)
            val fileName = getProgressFileName(bookProgress.name, bookProgress.author)

            withContext(IO) {
                val rootDoc = DocumentFile.fromTreeUri(appCtx, uri)!!
                val bookProgressDirDoc = DocumentUtils.createFolderIfNotExist(rootDoc, bookProgressDirName)!!
                val fileDoc = DocumentUtils.createFileIfNotExist(bookProgressDirDoc, fileName)
                fileDoc?.uri?.outputStream(appCtx)?.getOrNull()?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                }
            }
            onSuccess?.invoke()
        } catch (e: Exception) {
            AppLog.put("上传进度到本地失败\n${e.localizedMessage}", e)
        }
    }

    suspend fun getBookProgress(book: Book): BookProgress? {
        if (!isOk) return null
        if (!AppConfig.localAutoSyncProgress) return null
        val uri = localSyncUri ?: return null
        if (!uri.isContentScheme()) return null

        try {
            val fileName = getProgressFileName(book.name, book.author)
            val rootDoc = DocumentFile.fromTreeUri(appCtx, uri)!!
            val bookProgressDirDoc = DocumentUtils.createFolderIfNotExist(rootDoc, bookProgressDirName)!!
            val fileDoc = DocumentUtils.createFileIfNotExist(bookProgressDirDoc, fileName)

            if (fileDoc == null || !fileDoc.exists()) {
                return null
            }

            return withContext(IO) {
                val json = fileDoc.uri.inputStream(appCtx).getOrNull()?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }
                if (json != null && json.isJson()) {
                    GSON.fromJsonObject<BookProgress>(json).getOrNull()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            AppLog.put("获取本地书籍进度失败\n${e.localizedMessage}", e)
            return null
        }
    }

    suspend fun downloadAllBookProgress() {
        if (!isOk) return
        if (!AppConfig.localAutoSyncProgress) return
        val uri = localSyncUri ?: return
        if (!uri.isContentScheme()) return

        try {
            withContext(IO) {
                val bookProgressDir = FileDoc.fromDir(uri).createFolderIfNotExist(bookProgressDirName)
                val fileList = bookProgressDir.list() ?: return@withContext
                
                val bookProgressFiles = mutableMapOf<String, FileDoc>()
                fileList.forEach { fileDoc ->
                    if (fileDoc.name.endsWith(".json")) {
                        bookProgressFiles[fileDoc.name] = fileDoc
                    }
                }

                val allBooks = appDb.bookDao.all
                allBooks.forEach { book ->
                    val progressFileName = getProgressFileName(book.name, book.author)
                    val progressFileDoc = bookProgressFiles[progressFileName] ?: return@forEach

                    try {
                        val json = progressFileDoc.uri.inputStream(appCtx).getOrNull()?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        }
                        if (json != null && json.isJson()) {
                            val bookProgress = GSON.fromJsonObject<BookProgress>(json).getOrNull()
                            bookProgress ?: return@forEach

                            if (bookProgress.durChapterIndex <= book.durChapterIndex &&
                                (bookProgress.durChapterIndex != book.durChapterIndex ||
                                        bookProgress.durChapterPos <= book.durChapterPos)
                            ) {
                                return@forEach
                            }

                            book.durChapterIndex = bookProgress.durChapterIndex
                            book.durChapterPos = bookProgress.durChapterPos
                            book.durChapterTitle = bookProgress.durChapterTitle
                            book.durChapterTime = bookProgress.durChapterTime
                            book.syncTime = System.currentTimeMillis()
                            appDb.bookDao.update(book)
                        }
                    } catch (e: Exception) {
                        AppLog.put("同步书籍进度失败 ${book.name}\n${e.localizedMessage}", e)
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.put("下载所有书籍进度失败\n${e.localizedMessage}", e)
        }
    }

    suspend fun clearLocalProgress() {
        if (!isOk) return
        val uri = localSyncUri ?: return
        if (!uri.isContentScheme()) return

        try {
            withContext(IO) {
                val bookProgressDir = FileDoc.fromDir(uri).createFolderIfNotExist(bookProgressDirName)
                val fileList = bookProgressDir.list() ?: return@withContext

                fileList.forEach { fileDoc ->
                    if (fileDoc.name.endsWith(".json")) {
                        try {
                            fileDoc.delete()
                        } catch (e: Exception) {
                            AppLog.put("删除进度文件失败 ${fileDoc.name}\n${e.localizedMessage}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.put("清除本地进度失败\n${e.localizedMessage}", e)
        }
    }

    suspend fun backupToLocalSync() {
        if (!isOk) return
        val path = AppConfig.backupPath ?: return
        Backup.backupLocked(appCtx, path)
    }

    suspend fun getLocalBackupNames(): List<String> {
        if (!isOk) return emptyList()
        val uri = localSyncUri ?: return emptyList()
        if (!uri.isContentScheme()) return emptyList()

        return withContext(IO) {
            val rootDoc = DocumentFile.fromTreeUri(appCtx, uri)!!
            rootDoc.listFiles()
                .filter { it.name?.endsWith(".zip") == true }
                .mapNotNull { it.name }
                .sortedDescending()
        }
    }

    suspend fun restoreFromLocalSync(backupFileName: String) {
        if (!isOk) return
        val uri = localSyncUri ?: return
        if (!uri.isContentScheme()) return

        val rootDoc = DocumentFile.fromTreeUri(appCtx, uri)!!
        val fileDoc = rootDoc.findFile(backupFileName)
        if (fileDoc == null) {
            AppLog.put("本地同步目录中未找到备份文件: $backupFileName")
            return
        }
        Restore.restore(appCtx, fileDoc.uri)
    }

    suspend fun getDeviceBackupInfo(deviceName: String): Triple<String, Uri, Long>? {
        if (!isOk) return null
        val uri = localSyncUri ?: return null
        if (!uri.isContentScheme()) return null

        return withContext(IO) {
            val rootDoc = DocumentFile.fromTreeUri(appCtx, uri)!!
            rootDoc.listFiles()
                .filter { f -> f.name?.endsWith(".zip") == true && f.name?.contains(deviceName) == true }
                .maxByOrNull { it.lastModified() }
                ?.let { Triple(it.name!!, it.uri, it.lastModified()) }
        }
    }
}