package com.indianservers.biology.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

class InfographicRepository(
    context: Context,
    private val catalogUrl: String
) {
    private val appContext = context.applicationContext
    private val database = InfographicDatabase(appContext)
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeDownloads = ConcurrentHashMap<String, Future<*>>()
    private val activeThumbnails = ConcurrentHashMap.newKeySet<String>()
    private val infographicDirectory =
        File(appContext.filesDir, "biology/infographics").apply { mkdirs() }
    private val thumbnailDirectory =
        File(appContext.cacheDir, "biology-infographic-thumbnails").apply { mkdirs() }
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun cached(): List<Infographic> = database.all().map(::validateSavedFile)

    fun refresh(callback: (InfographicCatalogResult) -> Unit) {
        executor.execute {
            val result = if (catalogUrl.isBlank()) {
                cachedResult("Infographic catalogue URL has not been configured yet.")
            } else {
                runCatching { downloadCatalog() }.getOrElse { error ->
                    cachedResult(
                        "Using saved infographic details. ${error.message ?: "Network unavailable."}"
                    )
                }
            }
            callbackOnMain(result, callback)
        }
    }

    fun save(
        infographic: Infographic,
        callback: (Infographic) -> Unit
    ) {
        if (activeDownloads.containsKey(infographic.id)) return
        val queued = infographic.copy(
            status = InfographicDownloadStatus.QUEUED,
            progress = 0f,
            errorMessage = null
        )
        database.upsert(queued)
        callbackOnMain(queued, callback)

        activeDownloads[infographic.id] = executor.submit {
            val extension = fileExtension(infographic.fileUrl, infographic.mediaType)
            val target = File(infographicDirectory, "${safeId(infographic.id)}.$extension")
            val partial = File(infographicDirectory, "${target.name}.part")
            var connection: HttpURLConnection? = null
            try {
                connection = openHttp(infographic.fileUrl)
                val reportedBytes = connection.contentLengthLong.takeIf { it > 0L }
                    ?: infographic.fileSizeBytes
                    ?: -1L
                val maximumBytes = reportedBytes.takeIf { it > 0L }
                    ?.let { reported ->
                        (reported + maxOf(reported / 20L, 512L * 1024L))
                            .coerceAtMost(MAX_INFOGRAPHIC_BYTES)
                    }
                    ?: MAX_INFOGRAPHIC_BYTES
                copyWithProgress(
                    infographic,
                    connection.inputStream,
                    partial,
                    reportedBytes,
                    maximumBytes,
                    callback
                )
                check(partial.length() > 0L) { "Downloaded infographic is empty." }
                infographic.checksumSha256?.let { checksum ->
                    check(sha256(partial).equals(checksum, ignoreCase = true)) {
                        "Infographic checksum did not match."
                    }
                }
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not save infographic." }
                val saved = infographic.copy(
                    localFilePath = target.absolutePath,
                    savedAtEpochMs = System.currentTimeMillis(),
                    status = InfographicDownloadStatus.SAVED,
                    progress = 1f,
                    errorMessage = null
                )
                database.upsert(saved)
                callbackOnMain(saved, callback)
            } catch (error: Exception) {
                partial.delete()
                val failed = infographic.copy(
                    status = InfographicDownloadStatus.FAILED,
                    progress = 0f,
                    errorMessage = error.message ?: "Download failed."
                )
                database.upsert(failed)
                callbackOnMain(failed, callback)
            } finally {
                connection?.disconnect()
                activeDownloads.remove(infographic.id)
            }
        }
    }

    fun remove(infographic: Infographic): Infographic {
        infographic.localFilePath?.let(::File)?.delete()
        val removed = infographic.copy(
            localFilePath = null,
            savedAtEpochMs = null,
            status = InfographicDownloadStatus.NOT_SAVED,
            progress = 0f,
            errorMessage = null
        )
        database.upsert(removed)
        return removed
    }

    fun thumbnailFile(infographic: Infographic): File? =
        File(
            thumbnailDirectory,
            "${safeId(infographic.id)}.${thumbnailExtension(infographic.thumbnailUrl)}"
        ).takeIf { it.isFile && it.length() > 0L }

    fun loadThumbnail(infographic: Infographic, callback: (File?) -> Unit) {
        thumbnailFile(infographic)?.let {
            callback(it)
            return
        }
        val url = infographic.thumbnailUrl
        if (url.isNullOrBlank() || !activeThumbnails.add(infographic.id)) {
            callback(null)
            return
        }
        executor.execute {
            val target = File(
                thumbnailDirectory,
                "${safeId(infographic.id)}.${thumbnailExtension(url)}"
            )
            val partial = File(thumbnailDirectory, "${target.name}.part")
            val result = runCatching {
                val connection = openHttp(url)
                try {
                    copyBounded(connection.inputStream, partial, MAX_THUMBNAIL_BYTES)
                } finally {
                    connection.disconnect()
                }
                if (target.exists()) target.delete()
                check(partial.renameTo(target))
                target
            }.getOrNull()
            partial.delete()
            activeThumbnails.remove(infographic.id)
            mainHandler.post { callback(result) }
        }
    }

    fun close() {
        activeDownloads.values.forEach { it.cancel(true) }
        executor.shutdownNow()
        database.close()
    }

    private fun downloadCatalog(): InfographicCatalogResult {
        val connection = openHttp(catalogUrl, useEtag = true)
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return cachedResult()
            }
            val json = connection.inputStream.use {
                readBounded(it, MAX_CATALOG_BYTES).toString(Charsets.UTF_8)
            }
            val parsed = parseCatalog(json, catalogUrl)
            check(parsed.isNotEmpty()) { "Infographic catalogue contains no items." }
            parsed.forEach(database::upsertFromCatalog)
            preferences.edit()
                .putString(PREFERENCE_ETAG, connection.getHeaderField("ETag"))
                .putString(PREFERENCE_SOURCE_URL, catalogUrl)
                .apply()
            InfographicCatalogResult(
                database.all().map(::validateSavedFile),
                InfographicCatalogSource.NETWORK
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun cachedResult(warning: String? = null): InfographicCatalogResult {
        val cached = database.all().map(::validateSavedFile)
        return InfographicCatalogResult(
            cached,
            if (cached.isEmpty()) InfographicCatalogSource.NONE
            else InfographicCatalogSource.DATABASE,
            warning
        )
    }

    private fun validateSavedFile(infographic: Infographic): Infographic {
        if (!infographic.isSaved) return infographic
        val file = infographic.localFilePath?.let(::File)
        if (file?.isFile == true && file.length() > 0L) return infographic
        val missing = infographic.copy(
            localFilePath = null,
            savedAtEpochMs = null,
            status = InfographicDownloadStatus.NOT_SAVED,
            progress = 0f
        )
        database.upsert(missing)
        return missing
    }

    private fun openHttp(url: String, useEtag: Boolean = false): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 35_000
        connection.instanceFollowRedirects = true
        if (
            useEtag &&
            preferences.getString(PREFERENCE_SOURCE_URL, null) == catalogUrl
        ) {
            preferences.getString(PREFERENCE_ETAG, null)
                ?.let { connection.setRequestProperty("If-None-Match", it) }
        }
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
            check(connection.responseCode in 200..299) {
                "Server returned ${connection.responseCode}."
            }
        }
        return connection
    }

    private fun copyWithProgress(
        infographic: Infographic,
        input: java.io.InputStream,
        target: File,
        expectedBytes: Long,
        maximumBytes: Long,
        callback: (Infographic) -> Unit
    ) {
        var copied = 0L
        var lastPercent = -1
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    if (Thread.currentThread().isInterrupted) error("Download cancelled.")
                    val count = source.read(buffer)
                    if (count < 0) break
                    copied += count
                    check(copied <= maximumBytes) { "Infographic exceeds the size limit." }
                    output.write(buffer, 0, count)
                    val percent =
                        if (expectedBytes > 0L) ((copied * 100L) / expectedBytes).toInt() else 0
                    if (percent != lastPercent && percent % 5 == 0) {
                        lastPercent = percent
                        val progress = infographic.copy(
                            status = InfographicDownloadStatus.DOWNLOADING,
                            progress = (percent / 100f).coerceIn(0f, 1f)
                        )
                        database.upsert(progress)
                        callbackOnMain(progress, callback)
                    }
                }
            }
        }
    }

    private fun copyBounded(input: java.io.InputStream, target: File, maximumBytes: Long) {
        var copied = 0L
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    copied += count
                    check(copied <= maximumBytes) { "Thumbnail is too large." }
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    private fun callbackOnMain(
        result: InfographicCatalogResult,
        callback: (InfographicCatalogResult) -> Unit
    ) {
        mainHandler.post { callback(result) }
    }

    private fun callbackOnMain(
        infographic: Infographic,
        callback: (Infographic) -> Unit
    ) {
        mainHandler.post { callback(infographic) }
    }

    companion object {
        private const val PREFERENCES_NAME = "biology_infographic_catalog"
        private const val PREFERENCE_ETAG = "catalog_etag"
        private const val PREFERENCE_SOURCE_URL = "catalog_source_url"
        private const val MAX_CATALOG_BYTES = 5L * 1024L * 1024L
        private const val MAX_THUMBNAIL_BYTES = 8L * 1024L * 1024L
        private const val MAX_INFOGRAPHIC_BYTES = 80L * 1024L * 1024L

        fun parseCatalog(json: String, sourceUrl: String): List<Infographic> {
            val root = JSONObject(json)
            val entries = root.optJSONArray("infographics") ?: JSONArray()
            return buildList {
                repeat(entries.length()) { index ->
                    val item = entries.optJSONObject(index) ?: return@repeat
                    val id = item.optString("id").trim()
                    val title = item.optString("title").trim()
                    val fileReference =
                        item.optString("fileUrl").ifBlank { item.optString("filePath") }.trim()
                    val fileUrl = resolveUrl(sourceUrl, fileReference)
                    if (id.isBlank() || title.isBlank() || fileUrl == null) return@repeat
                    val source = item.optJSONObject("source")
                    add(
                        Infographic(
                            id = id,
                            title = title,
                            summary = item.optString("summary"),
                            category = item.optString("category", "General Biology"),
                            tags = item.stringList("tags"),
                            thumbnailUrl = resolveUrl(
                                sourceUrl,
                                item.optString("thumbnailUrl")
                                    .ifBlank { item.optString("thumbnailPath") }
                            ),
                            fileUrl = fileUrl,
                            mediaType = item.optString("mediaType", "image/png"),
                            fileSizeBytes = item.positiveLong("fileSizeBytes"),
                            checksumSha256 =
                                item.optString("sha256").takeIf(String::isNotBlank),
                            version = item.optInt("version", 1).coerceAtLeast(1),
                            gradeLevels = item.stringList("gradeLevels"),
                            sourceTitle = source?.optString("title")
                                ?.takeIf(String::isNotBlank),
                            sourceUrl = source?.optString("url")
                                ?.takeIf(String::isNotBlank),
                            reviewedAt = item.optString("reviewedAt")
                                .takeIf(String::isNotBlank)
                        )
                    )
                }
            }.distinctBy(Infographic::id)
        }

        private fun resolveUrl(base: String, value: String): String? {
            if (value.isBlank()) return null
            return runCatching {
                URL(URL(base), value.replace(" ", "%20")).toString()
            }.getOrNull()
        }

        private fun JSONObject.stringList(key: String): List<String> {
            val values = optJSONArray(key) ?: return emptyList()
            return buildList {
                repeat(values.length()) { index ->
                    values.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }

        private fun JSONObject.positiveLong(key: String): Long? =
            if (has(key) && !isNull(key)) optLong(key).takeIf { it > 0L } else null

        private fun readBounded(input: java.io.InputStream, maximumBytes: Long): ByteArray {
            val output = ByteArrayOutputStream()
            var copied = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                copied += count
                check(copied <= maximumBytes) { "Catalogue is unexpectedly large." }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }

        private fun safeId(id: String): String =
            id.lowercase().replace(Regex("[^a-z0-9._-]"), "_")

        private fun thumbnailExtension(url: String?): String =
            url?.substringBefore('?')
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in setOf("png", "jpg", "jpeg", "webp") }
                ?: "png"

        private fun fileExtension(url: String, mediaType: String): String {
            val fromUrl = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            if (fromUrl in setOf("png", "jpg", "jpeg", "webp")) return fromUrl
            return when (mediaType.lowercase()) {
                "image/jpeg" -> "jpg"
                "image/webp" -> "webp"
                else -> "png"
            }
        }

        private fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
