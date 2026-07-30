package com.indianservers.biology.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MicroscopyDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE microscopy_slides (
                slide_id TEXT PRIMARY KEY,
                payload_json TEXT NOT NULL,
                catalog_updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE microscopy_progress (
                slide_id TEXT PRIMARY KEY,
                best_score INTEGER NOT NULL,
                question_count INTEGER NOT NULL,
                attempts INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun replaceCatalog(entries: List<Pair<String, String>>) {
        writableDatabase.beginTransaction()
        try {
            entries.forEach { (id, payload) ->
                writableDatabase.insertWithOnConflict(
                    SLIDES_TABLE,
                    null,
                    ContentValues().apply {
                        put("slide_id", id)
                        put("payload_json", payload)
                        put("catalog_updated_at", System.currentTimeMillis())
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun cachedPayloads(): List<String> =
        readableDatabase.query(
            SLIDES_TABLE,
            arrayOf("payload_json"),
            null,
            null,
            null,
            null,
            "slide_id"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    fun progress(slideId: String): MicroscopyProgress =
        readableDatabase.query(
            PROGRESS_TABLE,
            arrayOf("best_score", "question_count", "attempts"),
            "slide_id = ?",
            arrayOf(slideId),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                MicroscopyProgress(slideId, cursor.getInt(0), cursor.getInt(1), cursor.getInt(2))
            } else {
                MicroscopyProgress(slideId, 0, 0, 0)
            }
        }

    fun recordAttempt(slideId: String, score: Int, questionCount: Int): MicroscopyProgress {
        val previous = progress(slideId)
        val updated = MicroscopyProgress(
            slideId,
            maxOf(previous.bestScore, score),
            maxOf(previous.questionCount, questionCount),
            previous.attempts + 1
        )
        writableDatabase.insertWithOnConflict(
            PROGRESS_TABLE,
            null,
            ContentValues().apply {
                put("slide_id", slideId)
                put("best_score", updated.bestScore)
                put("question_count", updated.questionCount)
                put("attempts", updated.attempts)
                put("updated_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return updated
    }

    companion object {
        private const val DATABASE_NAME = "biology_microscopy.db"
        private const val DATABASE_VERSION = 1
        private const val SLIDES_TABLE = "microscopy_slides"
        private const val PROGRESS_TABLE = "microscopy_progress"
    }
}
