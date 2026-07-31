package com.indianservers.AIbiology

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.AIbiology.data.MicroscopyDatabase
import com.indianservers.AIbiology.data.MicroscopyRepository
import com.indianservers.AIbiology.data.MicroscopySourceType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MicroscopyRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetDatabase() {
        context.deleteDatabase("biology_microscopy.db")
    }

    @After
    fun cleanDatabase() {
        context.deleteDatabase("biology_microscopy.db")
    }

    @Test
    fun parsesIiifSlideAndNormalizedAnnotations() {
        val slides = MicroscopyRepository.parseCatalog(
            """
            {
              "slides": [{
                "id": "KIDNEY_CORTEX",
                "title": "Kidney Cortex",
                "summary": "Renal histology",
                "thumbnailPath": "kidney/thumb.webp",
                "source": {
                  "type": "iiif",
                  "path": "kidney/info.json",
                  "width": 12000,
                  "height": 8000
                },
                "annotations": [{
                  "id": "GLOMERULUS",
                  "label": "Glomerulus",
                  "description": "A capillary tuft.",
                  "x": 0.42,
                  "y": 0.36,
                  "radius": 0.05
                }]
              }]
            }
            """.trimIndent(),
            "https://cdn.example.com/microscopy/catalog.json"
        )

        val slide = slides.single()
        assertEquals(MicroscopySourceType.IIIF, slide.source.type)
        assertEquals("https://cdn.example.com/microscopy/kidney/info.json", slide.source.url)
        assertEquals("https://cdn.example.com/microscopy/kidney/thumb.webp", slide.thumbnailUrl)
        assertEquals("Find Glomerulus", slide.annotations.single().challengePrompt)
    }

    @Test
    fun parsesRelativeDziSlideAndAnnotations() {
        val slides = MicroscopyRepository.parseCatalog(
            """
            {
              "slides": [{
                "id": "blood-smear",
                "title": "Human Blood Smear",
                "thumbnailPath": "blood/thumbnail.jpg",
                "source": {
                  "type": "dzi",
                  "path": "blood/blood.dzi",
                  "width": 1536,
                  "height": 1024
                },
                "annotations": [{
                  "id": "neutrophil",
                  "label": "Neutrophil",
                  "x": 0.43,
                  "y": 0.39,
                  "radius": 0.08
                }]
              }]
            }
            """.trimIndent(),
            "https://indianservers.com/edutech/biology/microscopy/catalog.json"
        )

        assertEquals(1, slides.size)
        assertEquals(MicroscopySourceType.DZI, slides.single().source.type)
        assertEquals(
            "https://indianservers.com/edutech/biology/microscopy/blood/blood.dzi",
            slides.single().source.url
        )
        assertTrue(slides.single().thumbnailUrl!!.endsWith("/blood/thumbnail.jpg"))
        assertEquals("Neutrophil", slides.single().annotations.single().label)
    }

    @Test
    fun sqliteKeepsBestChallengeResult() {
        val database = MicroscopyDatabase(context)
        database.recordAttempt("KIDNEY_CORTEX", 2, 4)
        val progress = database.recordAttempt("KIDNEY_CORTEX", 1, 4)

        assertEquals(2, progress.bestScore)
        assertEquals(2, progress.attempts)
        assertTrue(progress.questionCount == 4)
        database.close()
    }
}
