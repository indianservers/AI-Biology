package com.indianservers.biology.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class InfographicDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE infographics (
                infographic_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                summary TEXT NOT NULL,
                category TEXT NOT NULL,
                tags_json TEXT NOT NULL,
                thumbnail_url TEXT,
                file_url TEXT NOT NULL,
                media_type TEXT NOT NULL,
                file_size INTEGER,
                checksum TEXT,
                version INTEGER NOT NULL,
                grade_levels_json TEXT NOT NULL,
                source_title TEXT,
                source_url TEXT,
                reviewed_at TEXT,
                local_file_path TEXT,
                saved_at INTEGER,
                status TEXT NOT NULL,
                progress REAL NOT NULL,
                error_message TEXT,
                catalog_updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX infographic_category_idx ON infographics(category)")
        db.execSQL("CREATE INDEX infographic_saved_idx ON infographics(status)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun upsertFromCatalog(infographic: Infographic) {
        val existing = get(infographic.id)
        val merged = if (existing?.isSaved == true) {
            val updateAvailable =
                existing.version != infographic.version ||
                    (
                        !existing.checksumSha256.isNullOrBlank() &&
                            !infographic.checksumSha256.isNullOrBlank() &&
                            !existing.checksumSha256.equals(
                                infographic.checksumSha256,
                                ignoreCase = true
                            )
                        )
            infographic.copy(
                localFilePath = existing.localFilePath,
                savedAtEpochMs = existing.savedAtEpochMs,
                status =
                    if (updateAvailable) InfographicDownloadStatus.UPDATE_AVAILABLE
                    else InfographicDownloadStatus.SAVED,
                progress = 1f
            )
        } else {
            infographic
        }
        upsert(merged)
    }

    fun upsert(infographic: Infographic) {
        writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            infographic.toValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun get(id: String): Infographic? =
        readableDatabase.query(
            TABLE,
            COLUMNS,
            "infographic_id = ?",
            arrayOf(id),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toInfographic() else null
        }

    fun all(): List<Infographic> =
        readableDatabase.query(
            TABLE,
            COLUMNS,
            null,
            null,
            null,
            null,
            "title COLLATE NOCASE"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toInfographic())
            }
        }

    private fun Infographic.toValues() = ContentValues().apply {
        put("infographic_id", id)
        put("title", title)
        put("summary", summary)
        put("category", category)
        put("tags_json", JSONArray(tags).toString())
        put("thumbnail_url", thumbnailUrl)
        put("file_url", fileUrl)
        put("media_type", mediaType)
        fileSizeBytes?.let { put("file_size", it) } ?: putNull("file_size")
        put("checksum", checksumSha256)
        put("version", version)
        put("grade_levels_json", JSONArray(gradeLevels).toString())
        put("source_title", sourceTitle)
        put("source_url", sourceUrl)
        put("reviewed_at", reviewedAt)
        put("local_file_path", localFilePath)
        savedAtEpochMs?.let { put("saved_at", it) } ?: putNull("saved_at")
        put("status", status.name)
        put("progress", progress)
        put("error_message", errorMessage)
        put("catalog_updated_at", System.currentTimeMillis())
    }

    private fun Cursor.toInfographic() = Infographic(
        id = string("infographic_id").orEmpty(),
        title = string("title").orEmpty(),
        summary = string("summary").orEmpty(),
        category = string("category").orEmpty(),
        tags = jsonList(string("tags_json")),
        thumbnailUrl = string("thumbnail_url"),
        fileUrl = string("file_url").orEmpty(),
        mediaType = string("media_type").orEmpty(),
        fileSizeBytes = longOrNull("file_size"),
        checksumSha256 = string("checksum"),
        version = getInt(getColumnIndexOrThrow("version")),
        gradeLevels = jsonList(string("grade_levels_json")),
        sourceTitle = string("source_title"),
        sourceUrl = string("source_url"),
        reviewedAt = string("reviewed_at"),
        localFilePath = string("local_file_path"),
        savedAtEpochMs = longOrNull("saved_at"),
        status = runCatching {
            InfographicDownloadStatus.valueOf(string("status").orEmpty())
        }.getOrDefault(InfographicDownloadStatus.FAILED),
        progress = getFloat(getColumnIndexOrThrow("progress")),
        errorMessage = string("error_message")
    )

    private fun Cursor.string(column: String): String? =
        getColumnIndexOrThrow(column).let { index ->
            if (isNull(index)) null else getString(index)
        }

    private fun Cursor.longOrNull(column: String): Long? =
        getColumnIndexOrThrow(column).let { index ->
            if (isNull(index)) null else getLong(index)
        }

    private fun jsonList(value: String?): List<String> =
        runCatching {
            val array = JSONArray(value ?: "[]")
            buildList {
                repeat(array.length()) { index ->
                    array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())

    companion object {
        private const val DATABASE_NAME = "biology_infographics.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE = "infographics"
        private val COLUMNS = arrayOf(
            "infographic_id",
            "title",
            "summary",
            "category",
            "tags_json",
            "thumbnail_url",
            "file_url",
            "media_type",
            "file_size",
            "checksum",
            "version",
            "grade_levels_json",
            "source_title",
            "source_url",
            "reviewed_at",
            "local_file_path",
            "saved_at",
            "status",
            "progress",
            "error_message"
        )
    }
}
