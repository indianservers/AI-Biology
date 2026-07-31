package com.indianservers.AIbiology

import com.indianservers.AIbiology.data.BiologyCatalogQuery
import com.indianservers.AIbiology.data.BiologyCategories
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.ModelPart
import com.indianservers.AIbiology.data.ModelSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BiologyCatalogQueryTest {
    private val nucleus = ModelPart(
        id = "CELL_NUCLEUS",
        nodeNames = listOf("BIO_CELL_NUCLEUS_01"),
        hitNodeNames = listOf("HIT_CELL_NUCLEUS"),
        title = "Nucleus",
        shortDescription = "Stores genetic material.",
        position = "0 0 0"
    )
    private val model = BiologyModel(
        fileName = "cell.glb",
        title = "Animal Cell",
        shortTitle = "Cell",
        badge = "AC",
        parts = listOf(nucleus),
        alternativeNames = listOf("Eukaryotic cell"),
        scientificName = "Cellula animalis",
        categoryId = BiologyCategories.CELLS,
        tags = listOf("DNA", "organelles"),
        system = "Cell biology",
        gradeLevels = listOf("Student")
    )

    @Test
    fun searchMatchesEverySupportedMetadataField() {
        listOf("animal", "eukaryotic", "cellula", "cells", "dna", "biology", "student")
            .forEach { query ->
                val result = BiologyCatalogQuery.filter(
                    models = listOf(model),
                    query = query,
                    category = BiologyCategories.ALL,
                    sort = ModelSort.A_TO_Z,
                    downloadedIds = emptySet(),
                    recentIds = emptyList(),
                    limit = 20
                )
                assertEquals("Search should match $query", listOf(model), result)
            }
    }

    @Test
    fun downloadedFilterAndPaginationAreApplied() {
        val other = model.copy(fileName = "other.glb", id = "other", title = "Other")
        val result = BiologyCatalogQuery.filter(
            models = listOf(model, other),
            query = "",
            category = BiologyCategories.ALL,
            sort = ModelSort.DOWNLOADED,
            downloadedIds = setOf(other.id),
            recentIds = emptyList(),
            limit = 1
        )
        assertEquals(listOf(other), result)
    }

    @Test
    fun nodeNamesResolveToStableSemanticParts() {
        assertEquals(
            nucleus,
            BiologyCatalogQuery.semanticPartForNode(model, "HIT_CELL_NUCLEUS")
        )
        assertNull(BiologyCatalogQuery.semanticPartForNode(model, "decorative_mesh"))
    }
}
