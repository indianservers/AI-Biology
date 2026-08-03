package com.indianservers.AIbiology

import com.indianservers.AIbiology.data.LessonCatalog
import com.indianservers.AIbiology.data.LessonLevel
import com.indianservers.AIbiology.data.WorkbookCellLessonContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCatalogTest {

    @Test
    fun curriculumContainsMasterAreasPlusCompleteGradeSixAndSevenTracks() {
        assertEquals((1..46).toList(), LessonCatalog.areas.map { it.number })
        assertTrue(LessonCatalog.conceptCount >= 400)
        LessonLevel.entries.forEach { level ->
            assertTrue(
                "No curriculum area starts at ${level.label}",
                LessonCatalog.areas.any { it.startingLevel == level }
            )
        }
    }

    @Test
    fun gradeSixAndSevenLessonsAreFullyAuthoredAndDoNotUseFallbackCopy() {
        val grade6 = LessonCatalog.areas.single { it.number == 45 }
        val grade7 = LessonCatalog.areas.single { it.number == 46 }
        assertEquals(24, LessonCatalog.lessons(grade6).size)
        assertEquals(22, LessonCatalog.lessons(grade7).size)

        (LessonCatalog.lessons(grade6) + LessonCatalog.lessons(grade7)).forEach { lesson ->
            assertTrue(lesson.phases.first().title.contains("Learn the idea"))
            assertTrue(
                "${lesson.id} needs a fuller opening explanation",
                lesson.phases.first().explanation.split(Regex("\\s+")).size >= 15
            )
            assertTrue(lesson.phases.all { it.example.startsWith("Example:") })
            assertTrue(lesson.phases.none { it.explanation.contains("important idea in") })
        }
    }

    @Test
    fun everyConceptHasThreeCompleteEnglishPhasesAndDiscoveryMetadata() {
        LessonCatalog.allLessons().forEach { lesson ->
            assertEquals("en", lesson.languageTag)
            assertEquals(3, lesson.phases.size)
            lesson.phases.forEach { phase ->
                assertTrue(phase.title.isNotBlank())
                assertTrue(phase.explanation.isNotBlank())
                assertTrue(phase.example.isNotBlank())
                assertTrue(phase.didYouKnow.isNotBlank())
                assertTrue(
                    "${lesson.id} has a thin explanation",
                    phase.explanation.split(Regex("\\s+")).size >= 8
                )
                assertTrue(
                    "${lesson.id} has a thin example",
                    phase.example.split(Regex("\\s+")).size >= 8
                )
            }
            assertTrue(lesson.learningContent.detailedExplanation.isNotBlank())
            assertEquals(4, lesson.learningContent.easyWayToLearn.size)
            assertEquals(3, lesson.learningContent.realLifeExamples.size)
            assertEquals(5, lesson.learningContent.importantPoints.size)
            assertTrue(
                lesson.learningContent.importantPoints.any {
                    it.startsWith("Definition and foundation:")
                }
            )
            assertTrue(
                lesson.learningContent.importantPoints.any {
                    it.startsWith("Example to remember:")
                }
            )
            assertTrue(lesson.learningContent.commonMistake.isNotBlank())
            assertTrue(lesson.learningContent.quickCheckQuestion.isNotBlank())
            assertTrue(lesson.learningContent.quickCheckAnswer.isNotBlank())
            assertTrue(lesson.relatedConcepts.isNotEmpty())
            assertTrue(lesson.tags.isNotEmpty())
        }
    }

    @Test
    fun coreFoundationLessonsUseAuthoredConceptSpecificContent() {
        val biology = LessonCatalog.allLessons().single { it.title == "What is Biology?" }
        val cellTheory = LessonCatalog.allLessons().single { it.title == "Cell Theory" }

        assertTrue(biology.phases.first().explanation.contains("study of life"))
        assertTrue(cellTheory.phases.first().explanation.contains("basic unit of life"))
    }

    @Test
    fun noLessonUsesTheOldUniversalGenericFallback() {
        LessonCatalog.allLessons().forEach { lesson ->
            lesson.phases.forEach { phase ->
                assertTrue(
                    "${lesson.id} still contains old generic copy",
                    !phase.explanation.contains("is an important idea in")
                )
            }
        }
    }

    @Test
    fun everyTopicAndLessonHasEditorialGuidanceAndOutcomes() {
        assertEquals(
            LessonCatalog.areas.size,
            LessonCatalog.areas.map { it.caption }.distinct().size
        )
        LessonCatalog.allLessons().forEach { lesson ->
            assertTrue(lesson.area.caption.isNotBlank())
            assertTrue(lesson.subtopic.isNotBlank())
            assertTrue(lesson.subtopicCaption.isNotBlank())
            assertTrue(lesson.subtitle.isNotBlank())
            assertEquals(3, lesson.learningOutcomes.size)
            lesson.learningOutcomes.forEach { assertTrue(it.isNotBlank()) }
        }
    }

    @Test
    fun allSeventyWorkbookTopicsAreConnectedToRealLessonsWithVerifiedChecks() {
        val records = WorkbookCellLessonContent.sourceRecords()
        val appTitles = LessonCatalog.allLessons().map { it.title }.toSet()

        assertEquals(70, records.size)
        assertEquals((1..70).toList(), records.map { it.sourceId })
        records.forEach { record ->
            assertTrue(
                "Workbook topic ${record.sourceId} is not connected to an app lesson",
                record.appTitles.any { it in appTitles }
            )
            assertTrue(record.explanation.split(Regex("\\s+")).size >= 20)
            assertTrue(record.keyPoints.isNotEmpty())
            assertTrue(record.realLifeExample.isNotBlank())
            assertTrue(record.quizQuestion.isNotBlank())
            assertTrue(record.quizAnswer.isNotBlank())
        }

        assertEquals(7, records.map { it.sourceFile }.distinct().size)
        assertEquals("What is a Cell?", records.first().sourceTopic)
        assertEquals("Applications of Cell Biology", records.last().sourceTopic)
    }
}
