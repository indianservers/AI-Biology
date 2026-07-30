package com.indianservers.biology

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.biology.data.BiologyCategories
import com.indianservers.biology.data.RemoteBiologyCatalogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteBiologyCatalogRepositoryTest {

    @Test
    fun parsesRelativeCdnAssetsWithoutCreatingAGlbRequest() {
        val models = RemoteBiologyCatalogRepository.parseCatalog(
            json = """
                {
                  "models": [
                    {
                      "id": "HUMAN_HEART",
                      "title": "Human Heart",
                      "scientificName": "Cor",
                      "categoryId": "HUMAN_PHYSIOLOGY",
                      "tags": ["heart", "circulation"],
                      "shortDescription": "A muscular pump.",
                      "thumbnailPath": "7. Human Physiology/_cdn_packages/Heart/thumbnail.png",
                      "manifestPath": "7. Human Physiology/_cdn_packages/Heart.manifest.json",
                      "packagePath": "7. Human Physiology/_cdn_packages/Heart.zip",
                      "packageSizeBytes": 18901860,
                      "packageSha256": "abc123",
                      "modelSizeBytes": 23377720,
                      "modelVersion": 2,
                      "partIdentificationReady": false
                    }
                  ]
                }
            """.trimIndent(),
            sourceUrl = "https://cdn.example.com/biology/biology-catalog.json"
        )

        val heart = models.single()
        assertEquals("HUMAN_HEART", heart.id)
        assertEquals(BiologyCategories.HUMAN_BODY, heart.categoryId)
        assertEquals(
            "https://cdn.example.com/biology/7.%20Human%20Physiology/_cdn_packages/Heart/thumbnail.png",
            heart.thumbnailUrl
        )
        assertEquals(
            "https://cdn.example.com/biology/7.%20Human%20Physiology/_cdn_packages/Heart.zip",
            heart.packageUrl
        )
        assertEquals(18_901_860L, heart.packageSizeBytes)
        assertEquals("abc123", heart.packageChecksumSha256)
        assertEquals(2, heart.version)
        assertNull(heart.glbUrl)
        assertFalse(heart.supportsPartSelection)
    }

    @Test
    fun ignoresEntriesWithoutDownloadPackages() {
        val models = RemoteBiologyCatalogRepository.parseCatalog(
            """{"models":[{"id":"BROKEN","title":"Broken"}]}""",
            "https://cdn.example.com/biology-catalog.json"
        )

        assertEquals(emptyList<Any>(), models)
    }
}
