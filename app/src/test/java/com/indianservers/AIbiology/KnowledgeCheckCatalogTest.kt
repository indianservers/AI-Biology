package com.indianservers.AIbiology

import com.indianservers.AIbiology.data.KnowledgeCheckCatalog
import com.indianservers.AIbiology.data.KnowledgeQuestionKind
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.ModelPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeCheckCatalogTest {
    private val catalog = listOf(
        concept("Mitochondrion", "Mitochondria release usable energy through aerobic respiration."),
        concept("Chloroplast", "Chloroplasts capture light energy for photosynthesis."),
        concept("Neuron", "Neurons receive and transmit electrical and chemical signals."),
        concept("Ribosome", "Ribosomes translate messenger RNA into protein."),
        concept("Cell Membrane", "The cell membrane selectively controls movement into and out of a cell."),
        concept("Vacuole", "The plant vacuole stores solutes and contributes to turgor pressure."),
        concept("White Blood Cell", "White blood cells protect the body against infection."),
        BiologyModel(
            fileName = "PlantCell.glb",
            title = "Plant Cell",
            shortTitle = "Plant Cell",
            badge = "PC",
            description = "A plant cell contains specialised structures for support and photosynthesis.",
            parts = listOf(
                part("wall", "Cell Wall", "Provides strength and resists excessive expansion."),
                part("chloroplast", "Chloroplast", "Captures light energy for photosynthesis."),
                part("vacuole", "Central Vacuole", "Stores solutes and maintains turgor pressure."),
                part("nucleus", "Nucleus", "Stores and controls access to genetic information.")
            )
        )
    )

    @Test
    fun everyConceptHasAtLeastTwentyValidQuestions() {
        catalog.forEach { concept ->
            val questions = KnowledgeCheckCatalog.forConcept(concept, catalog)
            assertTrue(
                "${concept.title} has only ${questions.size} questions",
                questions.size >= KnowledgeCheckCatalog.MINIMUM_QUESTION_COUNT
            )
            questions.forEach { question ->
                assertTrue(question.choices.size >= 2)
                assertTrue(question.correctIndex in question.choices.indices)
                assertTrue(question.explanation.isNotBlank())
            }
            assertTrue(questions.any { it.kind == KnowledgeQuestionKind.MATCHING })
        }
    }

    @Test
    fun generationIsDeterministicForOfflineSessions() {
        val concept = catalog.first()
        assertEquals(
            KnowledgeCheckCatalog.forConcept(concept, catalog),
            KnowledgeCheckCatalog.forConcept(concept, catalog)
        )
    }

    private fun concept(title: String, description: String) = BiologyModel(
        fileName = "$title.glb",
        title = title,
        shortTitle = title,
        badge = title.take(2).uppercase(),
        description = description,
        parts = emptyList()
    )

    private fun part(id: String, title: String, description: String) = ModelPart(
        id = id,
        nodeNames = listOf(id),
        title = title,
        shortDescription = description,
        position = "0 0 0"
    )
}
