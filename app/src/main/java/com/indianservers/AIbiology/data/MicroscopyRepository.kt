package com.indianservers.AIbiology.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MicroscopyRepository(
    context: Context,
    private val catalogUrl: String
) {
    private val database = MicroscopyDatabase(context.applicationContext)
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeThumbnails = ConcurrentHashMap.newKeySet<String>()
    private val thumbnailDirectory =
        File(context.filesDir, "biology/microscopy/thumbnails").apply { mkdirs() }
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun refresh(callback: (MicroscopyCatalogResult) -> Unit) {
        executor.execute {
            val result = if (catalogUrl.isBlank()) {
                cachedResult("Microscopy catalogue URL has not been configured yet.")
            } else {
                runCatching { downloadCatalog() }.getOrElse { error ->
                    cachedResult(
                        "Using saved slide details. ${error.message ?: "Network unavailable."}"
                    )
                }
            }
            mainHandler.post { callback(result) }
        }
    }

    fun progress(slideId: String): MicroscopyProgress = database.progress(slideId)

    fun recordAttempt(
        slideId: String,
        score: Int,
        questionCount: Int
    ): MicroscopyProgress = database.recordAttempt(slideId, score, questionCount)

    fun thumbnailFile(slide: MicroscopySlide): File? =
        File(thumbnailDirectory, "${safeId(slide.id)}.${thumbnailExtension(slide.thumbnailUrl)}")
            .takeIf { it.isFile && it.length() > 0L }

    fun loadThumbnail(slide: MicroscopySlide, callback: (File?) -> Unit) {
        thumbnailFile(slide)?.let {
            callback(it)
            return
        }
        val url = slide.thumbnailUrl
        if (url.isNullOrBlank() || !activeThumbnails.add(slide.id)) {
            callback(null)
            return
        }
        executor.execute {
            val target =
                File(thumbnailDirectory, "${safeId(slide.id)}.${thumbnailExtension(url)}")
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
            activeThumbnails.remove(slide.id)
            mainHandler.post { callback(result) }
        }
    }

    fun close() {
        executor.shutdownNow()
        database.close()
    }

    private fun downloadCatalog(): MicroscopyCatalogResult {
        val connection = openHttp(catalogUrl, useEtag = true)
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return cachedResult()
            }
            val json = connection.inputStream.use {
                readBounded(it, MAX_CATALOG_BYTES).toString(Charsets.UTF_8)
            }
            val root = JSONObject(json)
            val entries = root.optJSONArray("slides") ?: JSONArray()
            val parsed = parseCatalog(json, catalogUrl)
            check(parsed.isNotEmpty()) { "Microscopy catalogue contains no slides." }
            val payloads = buildList {
                repeat(entries.length()) { index ->
                    val item = entries.optJSONObject(index) ?: return@repeat
                    val id = item.optString("id").trim()
                    if (id.isNotBlank()) add(id to item.toString())
                }
            }
            database.replaceCatalog(payloads)
            preferences.edit()
                .putString(PREFERENCE_ETAG, connection.getHeaderField("ETag"))
                .putString(PREFERENCE_SOURCE_URL, catalogUrl)
                .apply()
            MicroscopyCatalogResult(parsed)
        } finally {
            connection.disconnect()
        }
    }

    private fun cachedResult(warning: String? = null): MicroscopyCatalogResult {
        val slides = database.cachedPayloads().mapNotNull { payload ->
            parseSlide(JSONObject(payload), catalogUrl.takeIf(String::isNotBlank) ?: return@mapNotNull null)
        }
        return MicroscopyCatalogResult(slides, warning)
    }

    private fun openHttp(url: String, useEtag: Boolean = false): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 35_000
        connection.instanceFollowRedirects = true
        if (useEtag && preferences.getString(PREFERENCE_SOURCE_URL, null) == catalogUrl) {
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

    companion object {
        private const val PREFERENCES_NAME = "biology_microscopy_catalog"
        private const val PREFERENCE_ETAG = "catalog_etag"
        private const val PREFERENCE_SOURCE_URL = "catalog_source_url"
        private const val MAX_CATALOG_BYTES = 8L * 1024L * 1024L
        private const val MAX_THUMBNAIL_BYTES = 8L * 1024L * 1024L

        fun parseCatalog(json: String, sourceUrl: String): List<MicroscopySlide> {
            val entries = JSONObject(json).optJSONArray("slides") ?: JSONArray()
            return buildList {
                repeat(entries.length()) { index ->
                    parseSlide(entries.optJSONObject(index) ?: return@repeat, sourceUrl)?.let(::add)
                }
            }.distinctBy(MicroscopySlide::id)
        }

        private fun parseSlide(item: JSONObject, sourceUrl: String): MicroscopySlide? {
            val id = item.optString("id").trim()
            val title = item.optString("title").trim()
            val source = item.optJSONObject("source") ?: return null
            val sourceReference =
                source.optString("url").ifBlank { source.optString("path") }.trim()
            val slideUrl = resolveUrl(sourceUrl, sourceReference) ?: return null
            if (id.isBlank() || title.isBlank()) return null
            val annotationsJson = item.optJSONArray("annotations") ?: JSONArray()
            val annotations = buildList {
                repeat(annotationsJson.length()) { index ->
                    val annotation = annotationsJson.optJSONObject(index) ?: return@repeat
                    val annotationId = annotation.optString("id").trim()
                    val label = annotation.optString("label").trim()
                    val x = annotation.optDouble("x", Double.NaN)
                    val y = annotation.optDouble("y", Double.NaN)
                    if (
                        annotationId.isBlank() || label.isBlank() ||
                        !x.isFinite() || !y.isFinite() || x !in 0.0..1.0 || y !in 0.0..1.0
                    ) return@repeat
                    add(
                        MicroscopyAnnotation(
                            id = annotationId,
                            label = label,
                            scientificName = annotation.optString("scientificName")
                                .takeIf(String::isNotBlank),
                            description = annotation.optString("description"),
                            challengePrompt = annotation.optString("challengePrompt")
                                .ifBlank { "Find $label" },
                            x = x,
                            y = y,
                            radius = annotation.optDouble("radius", 0.04)
                                .coerceIn(0.005, 0.25)
                        )
                    )
                }
            }
            val sourceType = runCatching {
                MicroscopySourceType.valueOf(
                    source.optString("type", "image").uppercase()
                )
            }.getOrDefault(MicroscopySourceType.IMAGE)
            val attribution = item.optJSONObject("attribution")
            return MicroscopySlide(
                id = id,
                title = title,
                summary = item.optString("summary"),
                category = item.optString("category", "Histology"),
                tissue = item.optString("tissue").takeIf(String::isNotBlank),
                organ = item.optString("organ").takeIf(String::isNotBlank),
                species = item.optString("species").takeIf(String::isNotBlank),
                scientificName = item.optString("scientificName").takeIf(String::isNotBlank),
                stain = item.optString("stain").takeIf(String::isNotBlank),
                magnification = item.optString("magnification").takeIf(String::isNotBlank),
                thumbnailUrl = resolveUrl(
                    sourceUrl,
                    item.optString("thumbnailUrl").ifBlank { item.optString("thumbnailPath") }
                ),
                source = MicroscopySource(
                    sourceType,
                    slideUrl,
                    source.optInt("width").takeIf { it > 0 },
                    source.optInt("height").takeIf { it > 0 }
                ),
                annotations = annotations,
                attribution = attribution?.optString("title")?.takeIf(String::isNotBlank),
                reviewedAt = item.optString("reviewedAt").takeIf(String::isNotBlank)
            )
        }

        private fun resolveUrl(base: String, value: String): String? {
            if (value.isBlank()) return null
            return runCatching { URL(URL(base), value.replace(" ", "%20")).toString() }.getOrNull()
        }

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
            url?.substringBefore('?')?.substringAfterLast('.', "")?.lowercase()
                ?.takeIf { it in setOf("png", "jpg", "jpeg", "webp") } ?: "jpg"
    }
}
