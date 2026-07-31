package com.indianservers.AIbiology

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.AIbiology.data.BiologyCategories
import com.indianservers.AIbiology.data.RemoteBiologyCatalogRepository
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

    @Test
    fun parsesAnatomySystemCatalogueForOnDemandDownload() {
        val models = RemoteBiologyCatalogRepository.parseCatalog(
            """
            {
              "models": [{
                "id": "ANATOMY_SKELETON",
                "title": "Skeleton",
                "scientificName": "Systema skeletale",
                "categoryId": "HUMAN_ANATOMY",
                "shortDescription": "The structural framework of the body.",
                "thumbnailPath": "Skeleton/thumbnail.png",
                "manifestPath": "Skeleton.manifest.json",
                "packagePath": "Skeleton.zip",
                "packageSizeBytes": 15200000,
                "packageSha256": "abc123",
                "modelSizeBytes": 17926088,
                "partIdentificationReady": false
              }]
            }
            """.trimIndent(),
            "https://www.indianservers.com/biology/3d/Anatomy/_cdn_packages/catalog.json"
        )

        val skeleton = models.single()
        assertEquals("Human anatomy", skeleton.categoryId)
        assertEquals(
            "https://www.indianservers.com/biology/3d/Anatomy/_cdn_packages/Skeleton.zip",
            skeleton.packageUrl
        )
        assertEquals(
            "https://www.indianservers.com/biology/3d/Anatomy/_cdn_packages/Skeleton/thumbnail.png",
            skeleton.thumbnailUrl
        )
        assertFalse(skeleton.supportsPartSelection)
    }
}
