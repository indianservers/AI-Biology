package com.indianservers.AIbiology

import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.BiologyTheoryCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiologyTheoryCatalogTest {
    private val packagedConcepts = listOf(
        "Bacteriacell.glb",
        "Cell Membrane.glb",
        "Chloroplast.glb",
        "epithelial microvilli.glb",
        "Lysosome.glb",
        "Mitochondrion.glb",
        "Neuron.glb",
        "plant cell wall.glb",
        "PlantCell.glb",
        "Ribosomes.glb",
        "Rough Endoplasmic Reticulum.glb",
        "Smooth Endoplasmic Reticulum.glb",
        "Vacuole.glb",
        "WhiteBloodCell.glb"
    )

    @Test
    fun everyPackagedConceptHasCoreTheoryExampleAndAdvancedLearning() {
        packagedConcepts.forEach { fileName ->
            val model = BiologyModel(
                fileName = fileName,
                title = fileName.substringBeforeLast("."),
                shortTitle = fileName,
                badge = "3D",
                parts = emptyList()
            )
            val theory = BiologyTheoryCatalog.forModel(model)
            assertTrue("$fileName needs substantive core theory", theory.coreTheory.length > 250)
            assertTrue("$fileName needs an example", theory.example.startsWith("Example:"))
            assertTrue("$fileName needs advanced concepts", theory.knowMore.size >= 3)
            assertTrue("$fileName needs curriculum connections", theory.syllabusLinks.size >= 2)
        }
    }

    @Test
    fun everyLabelCanReceiveExpandedTheoryAndKnowMoreContent() {
        val expanded = BiologyTheoryCatalog.detailedPartTheory(
            "Cell membrane",
            "Controls transport."
        )
        val advanced = BiologyTheoryCatalog.partKnowMore(
            "Cell membrane",
            "Controls transport."
        )
        assertTrue(expanded.length > "Controls transport.".length)
        assertEquals(3, advanced.size)
        assertTrue(advanced.all(String::isNotBlank))
    }
}
