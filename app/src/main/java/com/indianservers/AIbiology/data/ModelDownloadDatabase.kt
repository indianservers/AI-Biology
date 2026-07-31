package com.indianservers.AIbiology.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ModelDownloadDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE model_downloads (
                model_id TEXT PRIMARY KEY,
                local_file_path TEXT,
                download_date INTEGER,
                last_opened_date INTEGER,
                file_size INTEGER NOT NULL,
                version INTEGER NOT NULL,
                checksum TEXT,
                status TEXT NOT NULL,
                explicitly_saved INTEGER NOT NULL,
                progress REAL NOT NULL,
                error_message TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun upsert(record: ModelDownloadRecord) {
        writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            record.toValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun get(modelId: String): ModelDownloadRecord? =
        readableDatabase.query(
            TABLE,
            COLUMNS,
            "model_id = ?",
            arrayOf(modelId),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.toRecord()
        }

    fun all(): List<ModelDownloadRecord> =
        readableDatabase.query(
            TABLE,
            COLUMNS,
            null,
            null,
            null,
            null,
            "last_opened_date DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toRecord())
            }
        }

    fun delete(modelId: String) {
        writableDatabase.delete(TABLE, "model_id = ?", arrayOf(modelId))
    }

    fun markOpened(modelId: String, openedAt: Long = System.currentTimeMillis()) {
        writableDatabase.update(
            TABLE,
            ContentValues().apply { put("last_opened_date", openedAt) },
            "model_id = ?",
            arrayOf(modelId)
        )
    }

    private fun ModelDownloadRecord.toValues() = ContentValues().apply {
        put("model_id", modelId)
        put("local_file_path", localFilePath)
        put("download_date", downloadDateEpochMs)
        put("last_opened_date", lastOpenedDateEpochMs)
        put("file_size", fileSizeBytes)
        put("version", version)
        put("checksum", checksumSha256)
        put("status", status.name)
        put("explicitly_saved", if (explicitlySaved) 1 else 0)
        put("progress", progress)
        put("error_message", errorMessage)
    }

    private fun android.database.Cursor.toRecord() = ModelDownloadRecord(
        modelId = getString(getColumnIndexOrThrow("model_id")),
        localFilePath = getString(getColumnIndexOrThrow("local_file_path")),
        downloadDateEpochMs =
            getLong(getColumnIndexOrThrow("download_date")).takeIf { !isNull(getColumnIndexOrThrow("download_date")) },
        lastOpenedDateEpochMs =
            getLong(getColumnIndexOrThrow("last_opened_date")).takeIf { !isNull(getColumnIndexOrThrow("last_opened_date")) },
        fileSizeBytes = getLong(getColumnIndexOrThrow("file_size")),
        version = getInt(getColumnIndexOrThrow("version")),
        checksumSha256 = getString(getColumnIndexOrThrow("checksum")),
        status = runCatching {
            ModelDownloadStatus.valueOf(getString(getColumnIndexOrThrow("status")))
        }.getOrDefault(ModelDownloadStatus.FAILED),
        explicitlySaved = getInt(getColumnIndexOrThrow("explicitly_saved")) == 1,
        progress = getFloat(getColumnIndexOrThrow("progress")),
        errorMessage = getString(getColumnIndexOrThrow("error_message"))
    )

    companion object {
        private const val DATABASE_NAME = "biology_models.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE = "model_downloads"
        private val COLUMNS = arrayOf(
            "model_id",
            "local_file_path",
            "download_date",
            "last_opened_date",
            "file_size",
            "version",
            "checksum",
            "status",
            "explicitly_saved",
            "progress",
            "error_message"
        )
    }
}

