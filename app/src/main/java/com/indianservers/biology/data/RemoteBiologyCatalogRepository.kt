package com.indianservers.biology.data

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

class RemoteBiologyCatalogRepository(
    context: Context,
    private val catalogUrl: String
) {
    private val appContext = context.applicationContext
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeThumbnails = ConcurrentHashMap.newKeySet<String>()
    private val catalogDirectory = File(appContext.filesDir, "biology/catalog").apply { mkdirs() }
    private val catalogCache = File(catalogDirectory, "biology-catalog.json")
    private val thumbnailDirectory =
        File(appContext.cacheDir, "biology-catalog-thumbnails").apply { mkdirs() }
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(callback: (CatalogLoadResult) -> Unit) {
        executor.execute {
            val result = when {
                catalogUrl.isBlank() -> loadCachedResult(
                    warning = "Set the biologyCatalogUrl Gradle property to enable the online library."
                )
                else -> runCatching { downloadCatalog() }
                    .getOrElse { error ->
                        loadCachedResult(
                            warning = "Using the saved catalogue. ${error.message ?: "Network unavailable."}"
                        )
                    }
            }
            mainHandler.post { callback(result) }
        }
    }

    fun thumbnailFile(model: BiologyModel): File? {
        val extension = thumbnailExtension(model.thumbnailUrl)
        return File(thumbnailDirectory, "${safeId(model.id)}.$extension")
            .takeIf { it.isFile && it.length() > 0L }
    }

    fun loadThumbnail(model: BiologyModel, callback: (File?) -> Unit) {
        thumbnailFile(model)?.let {
            callback(it)
            return
        }
        val remoteUrl = model.thumbnailUrl
        if (remoteUrl.isNullOrBlank() || !activeThumbnails.add(model.id)) {
            callback(null)
            return
        }
        executor.execute {
            val target = File(
                thumbnailDirectory,
                "${safeId(model.id)}.${thumbnailExtension(remoteUrl)}"
            )
            val partial = File(target.parentFile, "${target.name}.part")
            val result = runCatching {
                downloadFile(remoteUrl, partial, maxBytes = MAX_THUMBNAIL_BYTES)
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not save thumbnail." }
                target
            }.getOrNull()
            partial.delete()
            activeThumbnails.remove(model.id)
            mainHandler.post { callback(result) }
        }
    }

    fun close() {
        executor.shutdownNow()
    }

    private fun downloadCatalog(): CatalogLoadResult {
        val connection = URL(catalogUrl).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            val cachedSource =
                preferences.getString(PREFERENCE_CATALOG_SOURCE_URL, null)
            if (cachedSource == catalogUrl) {
                preferences.getString(PREFERENCE_CATALOG_ETAG, null)
                    ?.let { connection.setRequestProperty("If-None-Match", it) }
            }
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return loadCachedResult()
            }
            check(connection.responseCode in 200..299) {
                "Catalogue server returned ${connection.responseCode}."
            }
            val bytes = connection.inputStream.use {
                readBounded(it, MAX_CATALOG_BYTES)
            }
            val json = bytes.toString(Charsets.UTF_8)
            val models = parseCatalog(json, catalogUrl)
            check(models.isNotEmpty()) { "Catalogue contains no models." }
            FileOutputStream(catalogCache).use { it.write(bytes) }
            preferences.edit()
                .putString(PREFERENCE_CATALOG_ETAG, connection.getHeaderField("ETag"))
                .putString(PREFERENCE_CATALOG_SOURCE_URL, catalogUrl)
                .apply()
            CatalogLoadResult(models, CatalogSource.NETWORK)
        } finally {
            connection.disconnect()
        }
    }

    private fun loadCachedResult(warning: String? = null): CatalogLoadResult {
        if (!catalogCache.isFile || catalogCache.length() == 0L) {
            return CatalogLoadResult(emptyList(), CatalogSource.NONE, warning)
        }
        return runCatching {
            val sourceUrl =
                preferences.getString(PREFERENCE_CATALOG_SOURCE_URL, "").orEmpty()
                    .ifBlank { catalogUrl }
            CatalogLoadResult(
                parseCatalog(catalogCache.readText(), sourceUrl),
                CatalogSource.CACHE,
                warning
            )
        }.getOrElse {
            CatalogLoadResult(emptyList(), CatalogSource.NONE, warning ?: it.message)
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "biology_remote_catalog"
        private const val PREFERENCE_CATALOG_ETAG = "catalog_etag"
        private const val PREFERENCE_CATALOG_SOURCE_URL = "catalog_source_url"
        private const val MAX_CATALOG_BYTES = 5 * 1024 * 1024
        private const val MAX_THUMBNAIL_BYTES = 8 * 1024 * 1024

        fun parseCatalog(json: String, sourceUrl: String): List<BiologyModel> {
            val root = JSONObject(json)
            val models = root.optJSONArray("models") ?: JSONArray()
            return buildList {
                repeat(models.length()) { index ->
                    val item = models.optJSONObject(index) ?: return@repeat
                    val id = item.optString("id").trim()
                    val title = item.optString("title").trim()
                    val packagePath = item.optString("packagePath").trim()
                    if (id.isBlank() || title.isBlank() || packagePath.isBlank()) return@repeat
                    val partReady = item.optBoolean("partIdentificationReady", false)
                    add(
                        BiologyModel(
                            id = id,
                            fileName = "${safeId(id)}.glb",
                            title = title,
                            shortTitle = title,
                            badge = badgeFor(title),
                            parts = emptyList(),
                            scientificName = item.optNullableString("scientificName"),
                            description = item.optString("shortDescription"),
                            categoryId = displayCategory(item.optString("categoryId")),
                            tags = item.optStringList("tags"),
                            thumbnailUrl = resolveUrl(sourceUrl, item.optString("thumbnailPath")),
                            manifestUrl = resolveUrl(sourceUrl, item.optString("manifestPath")),
                            packageUrl = resolveUrl(sourceUrl, packagePath),
                            packageSizeBytes = item.optLongOrNull("packageSizeBytes"),
                            packageChecksumSha256 =
                                item.optNullableString("packageSha256"),
                            fileSizeBytes = item.optLongOrNull("modelSizeBytes"),
                            version = item.optInt("modelVersion", 1),
                            supportsAr = item.optBoolean("supportsAr", false),
                            supportsAnimations = item.optBoolean("supportsAnimations", false),
                            supportsPartSelection = partReady
                        )
                    )
                }
            }
        }

        private fun resolveUrl(base: String, relative: String): String? {
            if (relative.isBlank()) return null
            return runCatching {
                URL(URL(base), relative.replace(" ", "%20")).toString()
            }.getOrNull()
        }

        private fun displayCategory(category: String): String = when (category.uppercase()) {
            "CELL_BIOLOGY" -> BiologyCategories.CELLS
            "HUMAN_PHYSIOLOGY" -> BiologyCategories.HUMAN_BODY
            else -> category.replace('_', ' ').lowercase()
                .replaceFirstChar(Char::uppercase)
        }

        private fun badgeFor(title: String): String =
            title.split(' ')
                .filter(String::isNotBlank)
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
                .ifBlank { "3D" }

        private fun safeId(id: String): String =
            id.lowercase().replace(Regex("[^a-z0-9._-]"), "_")

        private fun thumbnailExtension(url: String?): String =
            url?.substringBefore('?')
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in setOf("png", "jpg", "jpeg", "webp") }
                ?: "png"

        private fun JSONObject.optNullableString(key: String): String? =
            optString(key).trim().takeIf(String::isNotBlank)

        private fun JSONObject.optLongOrNull(key: String): Long? =
            if (has(key) && !isNull(key)) optLong(key).takeIf { it > 0L } else null

        private fun JSONObject.optStringList(key: String): List<String> {
            val values = optJSONArray(key) ?: return emptyList()
            return buildList {
                repeat(values.length()) { index ->
                    values.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }

        private fun downloadFile(url: String, target: File, maxBytes: Int) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 12_000
                connection.readTimeout = 20_000
                connection.instanceFollowRedirects = true
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Server returned ${connection.responseCode}."
                }
                var copied = 0
                connection.inputStream.use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            check(copied <= maxBytes) { "Downloaded file is too large." }
                            output.write(buffer, 0, count)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

        private fun readBounded(input: java.io.InputStream, maxBytes: Int): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                copied += count
                check(copied <= maxBytes) { "Catalogue is unexpectedly large." }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }
}

data class CatalogLoadResult(
    val models: List<BiologyModel>,
    val source: CatalogSource,
    val warning: String? = null
)

enum class CatalogSource {
    NETWORK,
    CACHE,
    NONE
}
