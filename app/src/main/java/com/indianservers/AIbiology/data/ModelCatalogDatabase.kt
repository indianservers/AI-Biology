package com.indianservers.AIbiology.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Offline, relational model catalogue. Remote JSON and package manifests are import
 * formats only; the app reads model and learning metadata from this database.
 */
class ModelCatalogDatabase(context: Context, namespace: String = "biology") :
    SQLiteOpenHelper(
        context,
        "biology_catalog_${safeNamespace(namespace)}.db",
        null,
        DATABASE_VERSION
    ) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE models (
                id TEXT PRIMARY KEY,
                file_name TEXT NOT NULL,
                title TEXT NOT NULL,
                short_title TEXT NOT NULL,
                badge TEXT NOT NULL,
                scientific_name TEXT,
                description TEXT NOT NULL,
                category_id TEXT NOT NULL,
                system_name TEXT,
                thumbnail_url TEXT,
                glb_url TEXT,
                manifest_url TEXT,
                package_url TEXT,
                package_size INTEGER,
                package_checksum TEXT,
                file_size INTEGER,
                checksum TEXT,
                model_version INTEGER NOT NULL,
                supports_ar INTEGER NOT NULL,
                supports_animations INTEGER NOT NULL,
                supports_exploded_view INTEGER NOT NULL,
                supports_section_view INTEGER NOT NULL,
                supports_part_selection INTEGER NOT NULL,
                is_premium INTEGER NOT NULL,
                added_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE model_terms (
                model_id TEXT NOT NULL,
                term_type TEXT NOT NULL,
                position INTEGER NOT NULL,
                value TEXT NOT NULL,
                PRIMARY KEY (model_id, term_type, position),
                FOREIGN KEY (model_id) REFERENCES models(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE model_parts (
                model_id TEXT NOT NULL,
                position INTEGER NOT NULL,
                part_id TEXT NOT NULL,
                title TEXT NOT NULL,
                scientific_name TEXT,
                short_description TEXT NOT NULL,
                detailed_description TEXT,
                parent_part_id TEXT,
                audio_url TEXT,
                animation_name TEXT,
                selectable INTEGER NOT NULL,
                hotspot_position TEXT NOT NULL,
                hotspot_normal TEXT NOT NULL,
                camera_key TEXT,
                camera_short_label TEXT,
                camera_title TEXT,
                camera_orbit TEXT,
                camera_target TEXT,
                PRIMARY KEY (model_id, part_id),
                FOREIGN KEY (model_id) REFERENCES models(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE part_node_names (
                model_id TEXT NOT NULL,
                part_id TEXT NOT NULL,
                node_type TEXT NOT NULL,
                position INTEGER NOT NULL,
                value TEXT NOT NULL,
                PRIMARY KEY (model_id, part_id, node_type, position),
                FOREIGN KEY (model_id, part_id)
                    REFERENCES model_parts(model_id, part_id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun replaceAll(models: List<BiologyModel>) {
        writableDatabase.transaction {
            delete("models", null, null)
            models.forEach { insertModel(it) }
        }
    }

    fun upsert(model: BiologyModel) {
        writableDatabase.transaction {
            delete("models", "id = ?", arrayOf(model.id))
            insertModel(model)
        }
    }

    fun all(): List<BiologyModel> {
        val db = readableDatabase
        return db.query("models", null, null, null, null, null, "title COLLATE NOCASE")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toModel(db))
                }
            }
    }

    fun get(modelId: String): BiologyModel? {
        val db = readableDatabase
        return db.query("models", null, "id = ?", arrayOf(modelId), null, null, null, "1")
            .use { cursor -> if (cursor.moveToFirst()) cursor.toModel(db) else null }
    }

    fun delete(modelId: String) {
        writableDatabase.delete("models", "id = ?", arrayOf(modelId))
    }

    private fun SQLiteDatabase.insertModel(model: BiologyModel) {
        insertOrThrow("models", null, ContentValues().apply {
            put("id", model.id)
            put("file_name", model.fileName)
            put("title", model.title)
            put("short_title", model.shortTitle)
            put("badge", model.badge)
            put("scientific_name", model.scientificName)
            put("description", model.description)
            put("category_id", model.categoryId)
            put("system_name", model.system)
            put("thumbnail_url", model.thumbnailUrl)
            put("glb_url", model.glbUrl)
            put("manifest_url", model.manifestUrl)
            put("package_url", model.packageUrl)
            put("package_size", model.packageSizeBytes)
            put("package_checksum", model.packageChecksumSha256)
            put("file_size", model.fileSizeBytes)
            put("checksum", model.checksumSha256)
            put("model_version", model.version)
            putBoolean("supports_ar", model.supportsAr)
            putBoolean("supports_animations", model.supportsAnimations)
            putBoolean("supports_exploded_view", model.supportsExplodedView)
            putBoolean("supports_section_view", model.supportsSectionView)
            putBoolean("supports_part_selection", model.supportsPartSelection)
            putBoolean("is_premium", model.isPremium)
            put("added_at", model.addedAtEpochMs)
        })
        insertTerms(model.id, "alternative_name", model.alternativeNames)
        insertTerms(model.id, "tag", model.tags)
        insertTerms(model.id, "grade_level", model.gradeLevels)
        model.parts.forEachIndexed { position, part -> insertPart(model.id, position, part) }
    }

    private fun SQLiteDatabase.insertTerms(modelId: String, type: String, values: List<String>) {
        values.forEachIndexed { position, value ->
            insertOrThrow("model_terms", null, ContentValues().apply {
                put("model_id", modelId)
                put("term_type", type)
                put("position", position)
                put("value", value)
            })
        }
    }

    private fun SQLiteDatabase.insertPart(modelId: String, position: Int, part: ModelPart) {
        insertOrThrow("model_parts", null, ContentValues().apply {
            put("model_id", modelId)
            put("position", position)
            put("part_id", part.id)
            put("title", part.title)
            put("scientific_name", part.scientificName)
            put("short_description", part.shortDescription)
            put("detailed_description", part.detailedDescription)
            put("parent_part_id", part.parentPartId)
            put("audio_url", part.audioUrl)
            put("animation_name", part.animationName)
            putBoolean("selectable", part.selectable)
            put("hotspot_position", part.position)
            put("hotspot_normal", part.normal)
            put("camera_key", part.cameraPreset?.key)
            put("camera_short_label", part.cameraPreset?.shortLabel)
            put("camera_title", part.cameraPreset?.title)
            put("camera_orbit", part.cameraPreset?.orbit)
            put("camera_target", part.cameraPreset?.target)
        })
        insertNodeNames(modelId, part.id, "visible", part.nodeNames)
        insertNodeNames(modelId, part.id, "hit", part.hitNodeNames)
    }

    private fun SQLiteDatabase.insertNodeNames(
        modelId: String,
        partId: String,
        type: String,
        values: List<String>
    ) {
        values.forEachIndexed { position, value ->
            insertOrThrow("part_node_names", null, ContentValues().apply {
                put("model_id", modelId)
                put("part_id", partId)
                put("node_type", type)
                put("position", position)
                put("value", value)
            })
        }
    }

    private fun Cursor.toModel(db: SQLiteDatabase): BiologyModel {
        val id = string("id")
        return BiologyModel(
            id = id,
            fileName = string("file_name"),
            title = string("title"),
            shortTitle = string("short_title"),
            badge = string("badge"),
            parts = db.parts(id),
            alternativeNames = db.terms(id, "alternative_name"),
            scientificName = nullableString("scientific_name"),
            description = string("description"),
            categoryId = string("category_id"),
            tags = db.terms(id, "tag"),
            system = nullableString("system_name"),
            thumbnailUrl = nullableString("thumbnail_url"),
            glbUrl = nullableString("glb_url"),
            manifestUrl = nullableString("manifest_url"),
            packageUrl = nullableString("package_url"),
            packageSizeBytes = nullableLong("package_size"),
            packageChecksumSha256 = nullableString("package_checksum"),
            fileSizeBytes = nullableLong("file_size"),
            checksumSha256 = nullableString("checksum"),
            version = int("model_version"),
            supportsAr = boolean("supports_ar"),
            supportsAnimations = boolean("supports_animations"),
            supportsExplodedView = boolean("supports_exploded_view"),
            supportsSectionView = boolean("supports_section_view"),
            supportsPartSelection = boolean("supports_part_selection"),
            gradeLevels = db.terms(id, "grade_level"),
            isPremium = boolean("is_premium"),
            addedAtEpochMs = long("added_at")
        )
    }

    private fun SQLiteDatabase.terms(modelId: String, type: String): List<String> =
        query(
            "model_terms",
            arrayOf("value"),
            "model_id = ? AND term_type = ?",
            arrayOf(modelId, type),
            null,
            null,
            "position"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private fun SQLiteDatabase.parts(modelId: String): List<ModelPart> =
        query(
            "model_parts",
            null,
            "model_id = ?",
            arrayOf(modelId),
            null,
            null,
            "position"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val partId = cursor.string("part_id")
                    val cameraKey = cursor.nullableString("camera_key")
                    add(
                        ModelPart(
                            id = partId,
                            nodeNames = nodeNames(modelId, partId, "visible"),
                            title = cursor.string("title"),
                            scientificName = cursor.nullableString("scientific_name"),
                            shortDescription = cursor.string("short_description"),
                            detailedDescription = cursor.nullableString("detailed_description"),
                            parentPartId = cursor.nullableString("parent_part_id"),
                            audioUrl = cursor.nullableString("audio_url"),
                            animationName = cursor.nullableString("animation_name"),
                            cameraPreset = cameraKey?.let {
                                CameraPreset(
                                    key = it,
                                    shortLabel = cursor.nullableString("camera_short_label")
                                        ?: cursor.string("title"),
                                    title = cursor.nullableString("camera_title")
                                        ?: cursor.string("title"),
                                    orbit = cursor.nullableString("camera_orbit"),
                                    target = cursor.nullableString("camera_target")
                                )
                            },
                            selectable = cursor.boolean("selectable"),
                            position = cursor.string("hotspot_position"),
                            normal = cursor.string("hotspot_normal"),
                            hitNodeNames = nodeNames(modelId, partId, "hit")
                        )
                    )
                }
            }
        }

    private fun SQLiteDatabase.nodeNames(
        modelId: String,
        partId: String,
        type: String
    ): List<String> =
        query(
            "part_node_names",
            arrayOf("value"),
            "model_id = ? AND part_id = ? AND node_type = ?",
            arrayOf(modelId, partId, type),
            null,
            null,
            "position"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun ContentValues.putBoolean(key: String, value: Boolean) =
        put(key, if (value) 1 else 0)

    private fun Cursor.string(column: String) = getString(getColumnIndexOrThrow(column))
    private fun Cursor.nullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }
    private fun Cursor.int(column: String) = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.nullableLong(column: String): Long? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getLong(index)
    }
    private fun Cursor.boolean(column: String) = int(column) == 1

    companion object {
        private const val DATABASE_VERSION = 1

        private fun safeNamespace(value: String): String =
            value.lowercase().replace(Regex("[^a-z0-9._-]"), "_").ifBlank { "biology" }
    }
}
