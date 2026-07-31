package com.indianservers.AIbiology.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.ZipInputStream

class ModelRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = ModelDownloadDatabase(appContext)
    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeDownloads = ConcurrentHashMap<String, Future<*>>()
    private val modelDirectory = File(appContext.filesDir, "biology/3d").apply { mkdirs() }
    private val transientDirectory = File(appContext.cacheDir, "biology_models").apply { mkdirs() }

    fun records(): List<ModelDownloadRecord> = database.all()

    fun downloadedIds(): Set<String> =
        records().filter { it.status == ModelDownloadStatus.DOWNLOADED }
            .mapTo(mutableSetOf(), ModelDownloadRecord::modelId)

    fun hydrateInstalledModel(model: BiologyModel): BiologyModel {
        val safeId = model.id.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
        val manifestFile = File(modelDirectory, "$safeId.manifest.json")
        if (!manifestFile.isFile) return model

        return runCatching {
            val manifest = JSONObject(manifestFile.readText())
            check(manifest.optString("id") == model.id)
            val capabilities = manifest.optJSONObject("capabilities") ?: JSONObject()
            val supportsParts = capabilities.optBoolean("partSelection", false)
            val manifestParts = manifest.optJSONArray("parts")
            val parts = if (supportsParts && manifestParts != null) {
                buildList {
                    repeat(manifestParts.length()) { index ->
                        val part = manifestParts.optJSONObject(index) ?: return@repeat
                        val recognition = part.optJSONObject("recognition") ?: JSONObject()
                        val descriptions = part.optJSONObject("description") ?: JSONObject()
                        val hotspot = recognition.optJSONObject("fallbackHotspot")
                        val camera = part.optJSONObject("cameraPreset")
                        val id = part.optString("id").trim()
                        val title = part.optString("title").trim()
                        if (id.isBlank() || title.isBlank()) return@repeat
                        add(
                            ModelPart(
                                id = id,
                                nodeNames = recognition.stringList("visibleNodeNames"),
                                title = title,
                                scientificName = part.optString("scientificName")
                                    .trim()
                                    .takeIf(String::isNotBlank),
                                shortDescription = descriptions.optString("beginner")
                                    .ifBlank { descriptions.optString("student") },
                                detailedDescription = descriptions.optString("advanced")
                                    .takeIf(String::isNotBlank),
                                parentPartId = part.optString("parentPartId")
                                    .trim()
                                    .takeIf(String::isNotBlank),
                                animationName = part.optString("animationName")
                                    .trim()
                                    .takeIf(String::isNotBlank),
                                cameraPreset = camera?.let {
                                    CameraPreset(
                                        key = it.optString("key", id),
                                        shortLabel = it.optString("shortLabel", title),
                                        title = it.optString("title", title),
                                        orbit = it.optString("orbit")
                                            .takeIf(String::isNotBlank),
                                        target = it.optString("target")
                                            .takeIf(String::isNotBlank)
                                    )
                                },
                                selectable = part.optBoolean("selectable", true),
                                position = hotspot?.optString("position")
                                    ?.takeIf(String::isNotBlank)
                                    ?: "0 0 0",
                                normal = hotspot?.optString("normal")
                                    ?.takeIf(String::isNotBlank)
                                    ?: "0 0 1",
                                hitNodeNames = recognition.stringList("hitNodeNames")
                            )
                        )
                    }
                }
            } else {
                emptyList()
            }
            model.copy(
                parts = parts,
                alternativeNames = manifest.stringList("alternativeNames"),
                gradeLevels = manifest.stringList("gradeLevels").ifEmpty {
                    model.gradeLevels
                },
                supportsAr = capabilities.optBoolean("ar", model.supportsAr),
                supportsAnimations =
                    capabilities.optBoolean("animations", model.supportsAnimations),
                supportsExplodedView =
                    capabilities.optBoolean("explodedView", model.supportsExplodedView),
                supportsSectionView =
                    capabilities.optBoolean("sectionView", model.supportsSectionView),
                supportsPartSelection = supportsParts,
                isDownloaded = true
            )
        }.getOrDefault(model)
    }

    fun storageSummary(): Pair<Int, Long> {
        val downloaded = records().filter { it.status == ModelDownloadStatus.DOWNLOADED }
        return downloaded.size to downloaded.sumOf(ModelDownloadRecord::fileSizeBytes)
    }

    fun markOpened(modelId: String) = database.markOpened(modelId)

    fun download(
        model: BiologyModel,
        explicitlySaved: Boolean,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        if (!model.packageUrl.isNullOrBlank()) {
            downloadPackage(model, explicitlySaved, callback)
            return
        }
        val remoteUrl = model.glbUrl
        if (remoteUrl.isNullOrBlank()) {
            callbackOnMain(
                failedRecord(model, "No download source is configured for this model."),
                callback
            )
            return
        }
        if (activeDownloads.containsKey(model.id)) return

        val queued = baseRecord(model, explicitlySaved).copy(
            status = ModelDownloadStatus.QUEUED
        )
        database.upsert(queued)
        callbackOnMain(queued, callback)

        activeDownloads[model.id] = executor.submit {
            val target = File(modelDirectory, model.fileName)
            val partial = File(modelDirectory, "${model.fileName}.part")
            var connection: HttpURLConnection? = null
            try {
                connection = URL(remoteUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Server returned ${connection.responseCode}"
                }
                val expected = connection.contentLengthLong.takeIf { it > 0 }
                    ?: model.fileSizeBytes
                    ?: -1L
                var copied = 0L
                var lastReported = -1
                connection.inputStream.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            if (Thread.currentThread().isInterrupted) error("Download cancelled")
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            copied += count
                            val percent =
                                if (expected > 0) ((copied * 100) / expected).toInt() else 0
                            if (percent != lastReported && percent % 2 == 0) {
                                lastReported = percent
                                val progress = baseRecord(model, explicitlySaved).copy(
                                    localFilePath = partial.absolutePath,
                                    fileSizeBytes = copied,
                                    status = ModelDownloadStatus.DOWNLOADING,
                                    progress = (percent / 100f).coerceIn(0f, 1f)
                                )
                                database.upsert(progress)
                                callbackOnMain(progress, callback)
                            }
                        }
                    }
                }
                check(isValidGlb(partial)) { "Downloaded file is not a valid GLB." }
                model.checksumSha256?.let { expectedChecksum ->
                    check(sha256(partial).equals(expectedChecksum, ignoreCase = true)) {
                        "Model checksum did not match."
                    }
                }
                if (target.exists()) target.delete()
                check(partial.renameTo(target)) { "Could not finalize model file." }
                val completed = baseRecord(model, explicitlySaved).copy(
                    localFilePath = target.absolutePath,
                    downloadDateEpochMs = System.currentTimeMillis(),
                    lastOpenedDateEpochMs = System.currentTimeMillis(),
                    fileSizeBytes = target.length(),
                    status = ModelDownloadStatus.DOWNLOADED,
                    progress = 1f
                )
                database.upsert(completed)
                callbackOnMain(completed, callback)
                trimTransientCache()
            } catch (error: Exception) {
                partial.delete()
                val failed = failedRecord(model, error.message ?: "Download failed")
                    .copy(explicitlySaved = explicitlySaved)
                database.upsert(failed)
                callbackOnMain(failed, callback)
            } finally {
                connection?.disconnect()
                activeDownloads.remove(model.id)
            }
        }
    }

    fun cancel(modelId: String) {
        activeDownloads.remove(modelId)?.cancel(true)
    }

    fun remove(modelId: String, requireExplicitConfirmation: Boolean = true): Boolean {
        val record = database.get(modelId) ?: return true
        if (record.explicitlySaved && requireExplicitConfirmation) return false
        record.localFilePath?.let(::File)?.delete()
        val safeId = modelId.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
        File(modelDirectory, "$safeId.manifest.json").delete()
        database.delete(modelId)
        return true
    }

    fun removeAllTransient() {
        records().filterNot(ModelDownloadRecord::explicitlySaved).forEach {
            remove(it.modelId, requireExplicitConfirmation = false)
        }
    }

    fun removeAllDownloaded(): Pair<Int, Long> {
        val downloaded = records().filter {
            it.status == ModelDownloadStatus.DOWNLOADED
        }
        downloaded.forEach {
            remove(it.modelId, requireExplicitConfirmation = false)
        }
        return downloaded.size to downloaded.sumOf(ModelDownloadRecord::fileSizeBytes)
    }

    fun close() {
        activeDownloads.values.forEach { it.cancel(true) }
        executor.shutdownNow()
        database.close()
    }

    private fun downloadPackage(
        model: BiologyModel,
        explicitlySaved: Boolean,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        val packageUrl = model.packageUrl ?: return
        if (activeDownloads.containsKey(model.id)) return
        val queued = baseRecord(model, explicitlySaved).copy(
            fileSizeBytes = model.packageSizeBytes ?: model.fileSizeBytes ?: 0L,
            status = ModelDownloadStatus.QUEUED
        )
        database.upsert(queued)
        callbackOnMain(queued, callback)

        activeDownloads[model.id] = executor.submit {
            val safeId = model.id.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
            val partial = File(modelDirectory, "$safeId.package.part")
            val extractionDirectory = File(modelDirectory, ".$safeId-extract")
            var connection: HttpURLConnection? = null
            try {
                ensureManagedDirectory(extractionDirectory)
                extractionDirectory.deleteRecursively()
                extractionDirectory.mkdirs()
                connection = URL(packageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 45_000
                connection.instanceFollowRedirects = true
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Server returned ${connection.responseCode}"
                }
                val expected = connection.contentLengthLong.takeIf { it > 0L }
                    ?: model.packageSizeBytes
                    ?: -1L
                val maximumDownloadBytes = expected.takeIf { it > 0L }
                    ?.let { reported ->
                        (reported + maxOf(reported / 20L, 1024L * 1024L))
                            .coerceAtMost(MAX_PACKAGE_BYTES)
                    }
                    ?: MAX_PACKAGE_BYTES
                copyDownload(
                    model = model,
                    explicitlySaved = explicitlySaved,
                    input = connection.inputStream,
                    target = partial,
                    expectedBytes = expected,
                    maximumBytes = maximumDownloadBytes,
                    callback = callback
                )
                model.packageChecksumSha256?.let { expectedChecksum ->
                    check(sha256(partial).equals(expectedChecksum, ignoreCase = true)) {
                        "Package checksum did not match."
                    }
                }
                val maximumExtractedBytes = maxOf(
                    model.fileSizeBytes ?: 0L,
                    (model.packageSizeBytes ?: partial.length()) * 3L
                ).coerceAtMost(MAX_EXTRACTED_PACKAGE_BYTES - 16L * 1024L * 1024L) +
                    16L * 1024L * 1024L
                extractZip(partial, extractionDirectory, maximumExtractedBytes)
                val manifestFile = File(extractionDirectory, "manifest.json")
                check(manifestFile.isFile) { "Package has no manifest.json." }
                val manifest = JSONObject(manifestFile.readText())
                check(manifest.optString("id") == model.id) {
                    "Package model ID does not match the catalogue."
                }
                val modelJson = manifest.getJSONObject("model")
                val relativeModelPath = modelJson.optString("path", "model.glb")
                val extractedModel = managedChild(extractionDirectory, relativeModelPath)
                check(isValidGlb(extractedModel)) { "Package does not contain a valid GLB." }
                modelJson.optString("sha256").takeIf(String::isNotBlank)?.let { expectedChecksum ->
                    check(sha256(extractedModel).equals(expectedChecksum, ignoreCase = true)) {
                        "Model checksum did not match the manifest."
                    }
                }
                val target = File(modelDirectory, model.fileName)
                if (target.exists()) target.delete()
                extractedModel.copyTo(target, overwrite = true)
                check(isValidGlb(target)) { "Installed model validation failed." }
                manifestFile.copyTo(
                    File(modelDirectory, "$safeId.manifest.json"),
                    overwrite = true
                )
                val completed = baseRecord(model, explicitlySaved).copy(
                    localFilePath = target.absolutePath,
                    downloadDateEpochMs = System.currentTimeMillis(),
                    lastOpenedDateEpochMs = System.currentTimeMillis(),
                    fileSizeBytes = target.length(),
                    status = ModelDownloadStatus.DOWNLOADED,
                    progress = 1f
                )
                database.upsert(completed)
                callbackOnMain(completed, callback)
                trimTransientCache()
            } catch (error: Exception) {
                val failed = failedRecord(model, error.message ?: "Package download failed")
                    .copy(explicitlySaved = explicitlySaved)
                database.upsert(failed)
                callbackOnMain(failed, callback)
            } finally {
                connection?.disconnect()
                partial.delete()
                runCatching {
                    ensureManagedDirectory(extractionDirectory)
                    extractionDirectory.deleteRecursively()
                }
                activeDownloads.remove(model.id)
            }
        }
    }

    private fun copyDownload(
        model: BiologyModel,
        explicitlySaved: Boolean,
        input: java.io.InputStream,
        target: File,
        expectedBytes: Long,
        maximumBytes: Long,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        var copied = 0L
        var lastReported = -1
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    if (Thread.currentThread().isInterrupted) error("Download cancelled")
                    val count = source.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    copied += count
                    check(copied <= maximumBytes) {
                        "Package is larger than the allowed download size."
                    }
                    val percent =
                        if (expectedBytes > 0L) ((copied * 100) / expectedBytes).toInt() else 0
                    if (percent != lastReported && percent % 2 == 0) {
                        lastReported = percent
                        val progress = baseRecord(model, explicitlySaved).copy(
                            localFilePath = target.absolutePath,
                            fileSizeBytes = copied,
                            status = ModelDownloadStatus.DOWNLOADING,
                            progress = (percent / 100f).coerceIn(0f, 1f)
                        )
                        database.upsert(progress)
                        callbackOnMain(progress, callback)
                    }
                }
            }
        }
    }

    private fun extractZip(zipFile: File, destination: File, maximumBytes: Long) {
        val destinationRoot = destination.canonicalFile
        val destinationPath = destinationRoot.path + File.separator
        var extractedBytes = 0L
        var entryCount = 0
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                check(entryCount <= 200) { "Package contains too many files." }
                val target = File(destination, entry.name).canonicalFile
                val isPackageRoot = entry.isDirectory && target == destinationRoot
                check(isPackageRoot || target.path.startsWith(destinationPath)) {
                    "Package contains an unsafe file path."
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            extractedBytes += count
                            check(extractedBytes <= maximumBytes) {
                                "Package expands beyond its expected size."
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun managedChild(parent: File, relativePath: String): File {
        val parentPath = parent.canonicalPath + File.separator
        val child = File(parent, relativePath).canonicalFile
        check(child.path.startsWith(parentPath)) { "Manifest contains an unsafe model path." }
        return child
    }

    private fun ensureManagedDirectory(directory: File) {
        val root = modelDirectory.canonicalPath + File.separator
        check(directory.canonicalPath.startsWith(root)) {
            "Refusing to modify a directory outside managed model storage."
        }
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val values = optJSONArray(key) ?: return emptyList()
        return buildList {
            repeat(values.length()) { index ->
                values.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private fun trimTransientCache(maxBytes: Long = 350L * 1024L * 1024L) {
        val transient = records()
            .filter { !it.explicitlySaved && it.status == ModelDownloadStatus.DOWNLOADED }
            .sortedBy { it.lastOpenedDateEpochMs ?: 0L }
            .toMutableList()
        var total = transient.sumOf(ModelDownloadRecord::fileSizeBytes)
        while (total > maxBytes && transient.isNotEmpty()) {
            val oldest = transient.removeAt(0)
            total -= oldest.fileSizeBytes
            remove(oldest.modelId, requireExplicitConfirmation = false)
        }
        transientDirectory.listFiles()?.sortedBy(File::lastModified)?.let { files ->
            files.dropLast(30).forEach(File::delete)
        }
    }

    private fun baseRecord(model: BiologyModel, explicitlySaved: Boolean) =
        ModelDownloadRecord(
            modelId = model.id,
            localFilePath = null,
            downloadDateEpochMs = null,
            lastOpenedDateEpochMs = null,
            fileSizeBytes = model.fileSizeBytes ?: 0,
            version = model.version,
            checksumSha256 = model.checksumSha256,
            status = ModelDownloadStatus.NOT_DOWNLOADED,
            explicitlySaved = explicitlySaved
        )

    private fun failedRecord(model: BiologyModel, message: String) =
        baseRecord(model, false).copy(
            status = ModelDownloadStatus.FAILED,
            errorMessage = message
        )

    private fun callbackOnMain(
        record: ModelDownloadRecord,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        mainHandler.post { callback(record) }
    }

    companion object {
        private const val MAX_PACKAGE_BYTES = 1024L * 1024L * 1024L
        private const val MAX_EXTRACTED_PACKAGE_BYTES = 2L * 1024L * 1024L * 1024L

        fun isValidGlb(file: File): Boolean {
            if (!file.isFile || file.length() < 20) return false
            return FileInputStream(file).use { input ->
                val magic = ByteArray(4)
                input.read(magic) == 4 &&
                    magic.contentEquals(byteArrayOf(0x67, 0x6c, 0x54, 0x46))
            }
        }

        fun sha256(file: File): String {
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
