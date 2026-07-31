package com.indianservers.AIbiology

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.AIbiology.data.Infographic
import com.indianservers.AIbiology.data.InfographicDatabase
import com.indianservers.AIbiology.data.InfographicDownloadStatus
import com.indianservers.AIbiology.data.InfographicRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InfographicRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetDatabase() {
        context.deleteDatabase("biology_infographics.db")
    }

    @After
    fun cleanDatabase() {
        context.deleteDatabase("biology_infographics.db")
    }

    @Test
    fun parsesRelativeImageReferencesAndSourceDetails() {
        val entries = InfographicRepository.parseCatalog(
            """
            {
              "infographics": [{
                "id": "ANIMAL_CELL_OVERVIEW",
                "title": "Inside the Animal Cell",
                "summary": "A visual guide to organelles.",
                "category": "Cell Biology",
                "tags": ["cell", "organelles"],
                "thumbnailPath": "cell biology/animal-cell-thumb.webp",
                "filePath": "cell biology/animal-cell.png",
                "mediaType": "image/png",
                "fileSizeBytes": 2457600,
                "sha256": "abc123",
                "version": 2,
                "gradeLevels": ["Beginner", "Student"],
                "source": {
                  "title": "AI Explorer Biology Review",
                  "url": "https://example.com/review"
                },
                "reviewedAt": "2026-07-30"
              }]
            }
            """.trimIndent(),
            "https://cdn.example.com/biology/infographics/catalog.json"
        )

        val item = entries.single()
        assertEquals("ANIMAL_CELL_OVERVIEW", item.id)
        assertEquals(
            "https://cdn.example.com/biology/infographics/cell%20biology/animal-cell.png",
            item.fileUrl
        )
        assertEquals(
            "https://cdn.example.com/biology/infographics/cell%20biology/animal-cell-thumb.webp",
            item.thumbnailUrl
        )
        assertEquals(listOf("cell", "organelles"), item.tags)
        assertEquals("AI Explorer Biology Review", item.sourceTitle)
        assertEquals(2, item.version)
    }

    @Test
    fun sqliteRetainsOfflineCopyAndMarksNewCatalogueVersion() {
        val database = InfographicDatabase(context)
        val original = sampleInfographic(version = 1).copy(
            localFilePath = "/data/user/0/test/animal-cell.png",
            savedAtEpochMs = 100L,
            status = InfographicDownloadStatus.SAVED,
            progress = 1f
        )
        database.upsert(original)
        database.upsertFromCatalog(sampleInfographic(version = 2))

        val updated = database.get(original.id)!!
        assertEquals(original.localFilePath, updated.localFilePath)
        assertEquals(InfographicDownloadStatus.UPDATE_AVAILABLE, updated.status)
        assertTrue(updated.isSaved)
        database.close()
    }

    private fun sampleInfographic(version: Int) = Infographic(
        id = "ANIMAL_CELL_OVERVIEW",
        title = "Inside the Animal Cell",
        summary = "A visual guide.",
        category = "Cell Biology",
        tags = listOf("cell"),
        thumbnailUrl = "https://cdn.example.com/thumb.png",
        fileUrl = "https://cdn.example.com/full.png",
        mediaType = "image/png",
        fileSizeBytes = 2048,
        checksumSha256 = "checksum-$version",
        version = version,
        gradeLevels = listOf("Student"),
        sourceTitle = "Biology Review",
        sourceUrl = null,
        reviewedAt = "2026-07-30"
    )
}
