package com.indianservers.biology.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

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
        database.delete(modelId)
        return true
    }

    fun removeAllTransient() {
        records().filterNot(ModelDownloadRecord::explicitlySaved).forEach {
            remove(it.modelId, requireExplicitConfirmation = false)
        }
    }

    fun close() {
        activeDownloads.values.forEach { it.cancel(true) }
        executor.shutdownNow()
        database.close()
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
