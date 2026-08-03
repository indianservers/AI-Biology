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
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

class ModelRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = ModelDownloadDatabase(appContext)
    private val installedMetadata = ModelCatalogDatabase(appContext, "installed")
    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeDownloads = ConcurrentHashMap<String, Future<*>>()
    private val activeConnections = ConcurrentHashMap<String, HttpURLConnection>()
    private val pauseRequests = ConcurrentHashMap.newKeySet<String>()
    private val cancelRequests = ConcurrentHashMap.newKeySet<String>()
    private val resumeRequests = ConcurrentHashMap.newKeySet<String>()
    private val closed = AtomicBoolean(false)
    private val modelDirectory = File(appContext.filesDir, "biology/3d").apply { mkdirs() }
    private val transientDirectory = File(appContext.cacheDir, "biology_models").apply { mkdirs() }

    fun records(): List<ModelDownloadRecord> = database.all()

    fun downloadedIds(): Set<String> =
        records().filter { it.status == ModelDownloadStatus.DOWNLOADED }
            .mapTo(mutableSetOf(), ModelDownloadRecord::modelId)

    fun hydrateInstalledModel(model: BiologyModel): BiologyModel {
        installedMetadata.get(model.id)?.let { saved ->
            return saved.copy(
                thumbnailUrl = model.thumbnailUrl,
                glbUrl = model.glbUrl,
                manifestUrl = model.manifestUrl,
                packageUrl = model.packageUrl,
                packageSizeBytes = model.packageSizeBytes,
                packageChecksumSha256 = model.packageChecksumSha256,
                isDownloaded = true
            )
        }
        val safeId = model.id.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
        val manifestFile = File(modelDirectory, "$safeId.manifest.json")
        if (!manifestFile.isFile) return model

        return runCatching {
            val manifest = JSONObject(manifestFile.readText())
            check(manifest.optString("id") == model.id)
            model.withManifest(manifest).also {
                installedMetadata.upsert(it)
                manifestFile.delete()
            }
        }.getOrDefault(model)
    }

    fun storageSummary(): Pair<Int, Long> {
        val downloaded = records().filter { it.status == ModelDownloadStatus.DOWNLOADED }
        return downloaded.size to downloaded.sumOf(ModelDownloadRecord::fileSizeBytes)
    }

    fun markOpened(modelId: String) = database.markOpened(modelId)

    /**
     * Verifies every installed GLB against its binary header, recorded size and
     * catalogue checksum. Damaged entries are removed and downloaded again
     * without requiring the user to manually clear the App Library.
     */
    fun detectAndRepairDamagedDownloads(
        models: List<BiologyModel>,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        if (closed.get()) return
        val modelsById = models.associateBy(BiologyModel::id)
        executor.execute {
            records()
                .filter { it.status == ModelDownloadStatus.DOWNLOADED }
                .forEach { record ->
                    val model = modelsById[record.modelId] ?: return@forEach
                    val file = record.localFilePath?.let(::File)
                    val expectedChecksum =
                        model.checksumSha256?.takeIf(String::isNotBlank)
                            ?: record.checksumSha256?.takeIf(String::isNotBlank)
                    val valid = runCatching {
                        file != null &&
                            isValidGlb(file) &&
                            (record.fileSizeBytes <= 0L || file.length() == record.fileSizeBytes) &&
                            (
                                expectedChecksum == null ||
                                    sha256(file).equals(expectedChecksum, ignoreCase = true)
                                )
                    }.getOrDefault(false)
                    if (valid || closed.get() || activeDownloads.containsKey(model.id)) {
                        return@forEach
                    }

                    file?.delete()
                    val damaged = record.copy(
                        localFilePath = null,
                        fileSizeBytes = model.fileSizeBytes ?: 0L,
                        status = ModelDownloadStatus.FAILED,
                        progress = 0f,
                        errorMessage = "Damaged download detected. Repairing automatically."
                    )
                    database.upsert(damaged)
                    callbackOnMain(damaged, callback)
                    mainHandler.post {
                        if (!closed.get()) {
                            download(model, record.explicitlySaved, callback)
                        }
                    }
                }
        }
    }

    fun download(
        model: BiologyModel,
        explicitlySaved: Boolean,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        if (closed.get()) return
        if (activeDownloads.containsKey(model.id)) {
            if (database.get(model.id)?.status == ModelDownloadStatus.PAUSED) {
                resumeRequests.add(model.id)
                activeConnections.remove(model.id)?.disconnect()
                activeDownloads[model.id]?.cancel(true)
            }
            return
        }
        // A paused/cancelled task may have completed before the user taps again.
        // In that case there is no worker left to consume its request flag, and
        // carrying the flag into a fresh attempt would immediately stop it.
        pauseRequests.remove(model.id)
        cancelRequests.remove(model.id)
        resumeRequests.remove(model.id)
        if (!model.packageUrl.isNullOrBlank()) {
            if (!NetworkAvailability.isInternetAvailable(appContext)) {
                reportFailure(
                    model,
                    explicitlySaved,
                    NetworkAvailability.MODEL_DOWNLOAD_WARNING,
                    callback
                )
                return
            }
            downloadPackage(model, explicitlySaved, callback)
            return
        }
        val remoteUrl = model.glbUrl
        if (remoteUrl.isNullOrBlank()) {
            reportFailure(
                model,
                explicitlySaved,
                "No download source is configured for this model.",
                callback
            )
            return
        }
        if (!NetworkAvailability.isInternetAvailable(appContext)) {
            reportFailure(
                model,
                explicitlySaved,
                NetworkAvailability.MODEL_DOWNLOAD_WARNING,
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

        submitDownload(model, explicitlySaved, callback) {
            val target = File(modelDirectory, model.fileName)
            val partial = File(modelDirectory, "${model.fileName}.part")
            var connection: HttpURLConnection? = null
            try {
                val existingBytes = partial.length().takeIf { it > 0L } ?: 0L
                connection = URL(remoteUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                activeConnections[model.id] = connection
                if (existingBytes > 0L) {
                    connection.setRequestProperty("Range", "bytes=$existingBytes-")
                }
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Server returned ${connection.responseCode}"
                }
                val append = existingBytes > 0L &&
                    connection.responseCode == HttpURLConnection.HTTP_PARTIAL
                val resumedBytes = if (append) existingBytes else 0L
                val expected = connection.contentLengthLong.takeIf { it > 0 }
                    ?.plus(resumedBytes)
                    ?: model.fileSizeBytes
                    ?: -1L
                var copied = resumedBytes
                var lastReported = -1
                connection.inputStream.use { input ->
                    FileOutputStream(partial, append).use { output ->
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
                checkDownloadStillActive(model.id)
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
                handleStoppedDownload(model, explicitlySaved, partial, error, callback)
            } finally {
                connection?.disconnect()
                activeConnections.remove(model.id)
            }
        }
    }

    fun pause(modelId: String): ModelDownloadRecord? {
        val current = database.get(modelId) ?: return null
        if (current.status != ModelDownloadStatus.DOWNLOADING &&
            current.status != ModelDownloadStatus.QUEUED
        ) return current
        pauseRequests.add(modelId)
        activeConnections.remove(modelId)?.disconnect()
        activeDownloads[modelId]?.cancel(true)
        return current.copy(status = ModelDownloadStatus.PAUSED, errorMessage = null).also {
            database.upsert(it)
        }
    }

    fun cancel(modelId: String): ModelDownloadRecord? {
        val current = database.get(modelId) ?: return null
        cancelRequests.add(modelId)
        pauseRequests.remove(modelId)
        resumeRequests.remove(modelId)
        activeConnections.remove(modelId)?.disconnect()
        activeDownloads[modelId]?.cancel(true)
        current.localFilePath?.let(::File)?.delete()
        database.delete(modelId)
        return current.copy(
            localFilePath = null,
            fileSizeBytes = 0L,
            status = ModelDownloadStatus.NOT_DOWNLOADED,
            progress = 0f,
            errorMessage = null
        )
    }

    fun remove(modelId: String, requireExplicitConfirmation: Boolean = true): Boolean {
        val record = database.get(modelId) ?: return true
        if (record.explicitlySaved && requireExplicitConfirmation) return false
        record.localFilePath?.let(::File)?.delete()
        val safeId = modelId.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
        File(modelDirectory, "$safeId.manifest.json").delete()
        installedMetadata.delete(modelId)
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
        if (!closed.compareAndSet(false, true)) return
        val activeModelIds = activeDownloads.keys.toList()
        resumeRequests.clear()
        pauseRequests.addAll(activeModelIds)
        activeConnections.values.forEach(HttpURLConnection::disconnect)
        activeConnections.clear()
        activeDownloads.values.forEach { it.cancel(true) }
        executor.shutdownNow()
        runCatching { executor.awaitTermination(1, TimeUnit.SECONDS) }
        activeModelIds.forEach { modelId ->
            database.get(modelId)
                ?.takeIf {
                    it.status == ModelDownloadStatus.QUEUED ||
                        it.status == ModelDownloadStatus.DOWNLOADING
                }
                ?.copy(status = ModelDownloadStatus.PAUSED, errorMessage = null)
                ?.let(database::upsert)
        }
        mainHandler.removeCallbacksAndMessages(null)
        database.close()
        installedMetadata.close()
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

        submitDownload(model, explicitlySaved, callback) {
            val safeId = model.id.lowercase().replace(Regex("[^a-z0-9._-]"), "_")
            val partial = File(modelDirectory, "$safeId.package.part")
            val extractionDirectory = File(modelDirectory, ".$safeId-extract")
            var connection: HttpURLConnection? = null
            try {
                ensureManagedDirectory(extractionDirectory)
                extractionDirectory.deleteRecursively()
                extractionDirectory.mkdirs()
                val existingBytes = partial.length().takeIf { it > 0L } ?: 0L
                connection = URL(packageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 45_000
                connection.instanceFollowRedirects = true
                activeConnections[model.id] = connection
                if (existingBytes > 0L) {
                    connection.setRequestProperty("Range", "bytes=$existingBytes-")
                }
                connection.connect()
                check(connection.responseCode in 200..299) {
                    "Server returned ${connection.responseCode}"
                }
                val append = existingBytes > 0L &&
                    connection.responseCode == HttpURLConnection.HTTP_PARTIAL
                val resumedBytes = if (append) existingBytes else 0L
                val expected = connection.contentLengthLong.takeIf { it > 0L }
                    ?.plus(resumedBytes)
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
                    existingBytes = resumedBytes,
                    append = append,
                    callback = callback
                )
                checkDownloadStillActive(model.id)
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
                extractZip(partial, extractionDirectory, maximumExtractedBytes, model.id)
                checkDownloadStillActive(model.id)
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
                installedMetadata.upsert(model.withManifest(manifest))
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
                handleStoppedDownload(model, explicitlySaved, partial, error, callback)
            } finally {
                connection?.disconnect()
                if (model.id !in pauseRequests) partial.delete()
                runCatching {
                    ensureManagedDirectory(extractionDirectory)
                    extractionDirectory.deleteRecursively()
                }
                activeConnections.remove(model.id)
            }
        }
    }

    /**
     * Registers a task before it can start and always unregisters it from
     * FutureTask.done(). Unlike a worker-body finally block, done() also runs
     * when a queued task is cancelled before its body ever starts.
     */
    private fun submitDownload(
        model: BiologyModel,
        explicitlySaved: Boolean,
        callback: (ModelDownloadRecord) -> Unit,
        task: () -> Unit
    ) {
        lateinit var future: FutureTask<Unit>
        future = object : FutureTask<Unit>(Callable {
            task()
            Unit
        }) {
            override fun done() {
                activeDownloads.remove(model.id, this)
                resumeAfterStopIfRequested(model, explicitlySaved, callback)
            }
        }
        if (activeDownloads.putIfAbsent(model.id, future) != null) return
        try {
            executor.execute(future)
        } catch (error: RuntimeException) {
            activeDownloads.remove(model.id, future)
            if (!closed.get()) {
                reportFailure(
                    model,
                    explicitlySaved,
                    error.message ?: "Could not start the model download.",
                    callback
                )
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
        existingBytes: Long,
        append: Boolean,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        var copied = existingBytes
        var lastReported = -1
        input.use { source ->
            FileOutputStream(target, append).use { output ->
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

    private fun BiologyModel.withManifest(manifest: JSONObject): BiologyModel {
        val capabilities = manifest.optJSONObject("capabilities") ?: JSONObject()
        val supportsParts = capabilities.optBoolean("partSelection", false)
        val manifestParts = manifest.optJSONArray("parts")
        val importedParts =
            if (supportsParts && manifestParts != null) {
                buildList {
                    repeat(manifestParts.length()) { index ->
                        val part = manifestParts.optJSONObject(index) ?: return@repeat
                        val recognition = part.optJSONObject("recognition") ?: JSONObject()
                        val descriptions = part.optJSONObject("description") ?: JSONObject()
                        val hotspot = recognition.optJSONObject("fallbackHotspot")
                        val camera = part.optJSONObject("cameraPreset")
                        val partId = part.optString("id").trim()
                        val partTitle = part.optString("title").trim()
                        if (partId.isBlank() || partTitle.isBlank()) return@repeat
                        add(
                            ModelPart(
                                id = partId,
                                nodeNames = recognition.stringList("visibleNodeNames"),
                                title = partTitle,
                                scientificName = part.optString("scientificName")
                                    .trim().takeIf(String::isNotBlank),
                                shortDescription = descriptions.optString("beginner")
                                    .ifBlank { descriptions.optString("student") },
                                detailedDescription = descriptions.optString("advanced")
                                    .takeIf(String::isNotBlank),
                                parentPartId = part.optString("parentPartId")
                                    .trim().takeIf(String::isNotBlank),
                                animationName = part.optString("animationName")
                                    .trim().takeIf(String::isNotBlank),
                                cameraPreset = camera?.let {
                                    CameraPreset(
                                        key = it.optString("key", partId),
                                        shortLabel = it.optString("shortLabel", partTitle),
                                        title = it.optString("title", partTitle),
                                        orbit = it.optString("orbit").takeIf(String::isNotBlank),
                                        target = it.optString("target").takeIf(String::isNotBlank)
                                    )
                                },
                                selectable = part.optBoolean("selectable", true),
                                position = hotspot?.optString("position")
                                    ?.takeIf(String::isNotBlank) ?: "0 0 0",
                                normal = hotspot?.optString("normal")
                                    ?.takeIf(String::isNotBlank) ?: "0 0 1",
                                hitNodeNames = recognition.stringList("hitNodeNames")
                            )
                        )
                    }
                }
            } else {
                emptyList()
            }
        return copy(
            parts = importedParts,
            alternativeNames = manifest.stringList("alternativeNames").ifEmpty {
                alternativeNames
            },
            gradeLevels = manifest.stringList("gradeLevels").ifEmpty { gradeLevels },
            supportsAr = capabilities.optBoolean("ar", supportsAr),
            supportsAnimations = capabilities.optBoolean("animations", supportsAnimations),
            supportsExplodedView = capabilities.optBoolean("explodedView", supportsExplodedView),
            supportsSectionView = capabilities.optBoolean("sectionView", supportsSectionView),
            supportsPartSelection = supportsParts,
            isDownloaded = true
        )
    }

    private fun resumeAfterStopIfRequested(
        model: BiologyModel,
        explicitlySaved: Boolean,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        if (!resumeRequests.remove(model.id)) return
        pauseRequests.remove(model.id)
        if (closed.get()) return
        mainHandler.post { download(model, explicitlySaved, callback) }
    }

    private fun checkDownloadStillActive(modelId: String) {
        if (closed.get() ||
            Thread.currentThread().isInterrupted ||
            modelId in pauseRequests ||
            modelId in cancelRequests
        ) {
            error("Download stopped")
        }
    }

    private fun handleStoppedDownload(
        model: BiologyModel,
        explicitlySaved: Boolean,
        partial: File,
        error: Exception,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        if (closed.get()) return
        when {
            cancelRequests.remove(model.id) -> {
                partial.delete()
                database.delete(model.id)
                callbackOnMain(
                    baseRecord(model, explicitlySaved).copy(
                        status = ModelDownloadStatus.NOT_DOWNLOADED
                    ),
                    callback
                )
            }
            model.id in pauseRequests -> {
                val paused = baseRecord(model, explicitlySaved).copy(
                    localFilePath = partial.absolutePath,
                    fileSizeBytes = partial.length(),
                    status = ModelDownloadStatus.PAUSED,
                    progress = database.get(model.id)?.progress ?: 0f
                )
                database.upsert(paused)
                callbackOnMain(paused, callback)
            }
            else -> {
                partial.delete()
                val failureMessage =
                    if (!NetworkAvailability.isInternetAvailable(appContext)) {
                        NetworkAvailability.MODEL_DOWNLOAD_WARNING
                    } else {
                        error.message ?: "Download failed"
                    }
                val failed = failedRecord(model, failureMessage)
                    .copy(explicitlySaved = explicitlySaved)
                database.upsert(failed)
                callbackOnMain(failed, callback)
            }
        }
    }

    private fun extractZip(
        zipFile: File,
        destination: File,
        maximumBytes: Long,
        modelId: String
    ) {
        val destinationRoot = destination.canonicalFile
        val destinationPath = destinationRoot.path + File.separator
        var extractedBytes = 0L
        var entryCount = 0
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            while (true) {
                checkDownloadStillActive(modelId)
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
                            checkDownloadStillActive(modelId)
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

    private fun reportFailure(
        model: BiologyModel,
        explicitlySaved: Boolean,
        message: String,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        val failed = failedRecord(model, message).copy(explicitlySaved = explicitlySaved)
        database.upsert(failed)
        callbackOnMain(failed, callback)
    }

    private fun callbackOnMain(
        record: ModelDownloadRecord,
        callback: (ModelDownloadRecord) -> Unit
    ) {
        if (closed.get()) return
        mainHandler.post {
            if (closed.get()) return@post
            val current = database.get(record.modelId)
            val isCurrent =
                if (record.status == ModelDownloadStatus.NOT_DOWNLOADED) {
                    current == null
                } else {
                    current?.status == record.status
                }
            if (isCurrent) callback(record)
        }
    }

    companion object {
        private const val MAX_PACKAGE_BYTES = 1024L * 1024L * 1024L
        private const val MAX_EXTRACTED_PACKAGE_BYTES = 2L * 1024L * 1024L * 1024L

        fun isValidGlb(file: File): Boolean {
            if (!file.isFile || file.length() < 20) return false
            return FileInputStream(file).use { input ->
                val header = ByteArray(12)
                if (input.read(header) != header.size) return@use false
                val magicMatches =
                    header.copyOfRange(0, 4)
                        .contentEquals(byteArrayOf(0x67, 0x6c, 0x54, 0x46))
                val version = littleEndianUnsignedInt(header, 4)
                val declaredLength = littleEndianUnsignedInt(header, 8)
                magicMatches && version == 2L && declaredLength == file.length()
            }
        }

        private fun littleEndianUnsignedInt(bytes: ByteArray, offset: Int): Long =
            (bytes[offset].toLong() and 0xffL) or
                ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
                ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
                ((bytes[offset + 3].toLong() and 0xffL) shl 24)

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
