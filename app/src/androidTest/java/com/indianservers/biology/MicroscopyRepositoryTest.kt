package com.indianservers.biology

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.biology.data.MicroscopyDatabase
import com.indianservers.biology.data.MicroscopyRepository
import com.indianservers.biology.data.MicroscopySourceType
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
