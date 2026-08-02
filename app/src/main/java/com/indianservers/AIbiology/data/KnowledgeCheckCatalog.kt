package com.indianservers.AIbiology.data

import kotlin.random.Random

data class KnowledgeQuestion(
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val partIndex: Int = -1,
    val kind: KnowledgeQuestionKind = KnowledgeQuestionKind.MULTIPLE_CHOICE
)

enum class KnowledgeQuestionKind {
    MULTIPLE_CHOICE,
    MATCHING
}

/**
 * Builds deterministic offline knowledge checks from reviewed concept theory,
 * model metadata, and labelled parts. Each concept receives at least 20 questions.
 */
object KnowledgeCheckCatalog {
    const val MINIMUM_QUESTION_COUNT = 20

    fun forConcept(
        concept: BiologyModel,
        fullCatalog: List<BiologyModel>
    ): List<KnowledgeQuestion> {
        val catalog = (listOf(concept) + fullCatalog)
            .distinctBy(BiologyModel::id)
        val related = catalog.filterNot { it.id == concept.id }
        val random = Random(concept.id.hashCode())
        val questions = mutableListOf<KnowledgeQuestion>()
        val conceptFacts = factsFor(concept)

        conceptFacts.take(6).forEachIndexed { index, fact ->
            questions += identificationQuestion(
                fact,
                concept,
                related,
                Random(random.nextInt() + index)
            )
            correctStatementQuestion(
                concept,
                fact,
                related,
                Random(random.nextInt() + index * 31)
            )?.let(questions::add)
        }

        concept.scientificName?.takeIf(String::isNotBlank)?.let { scientificName ->
            val candidates = related.mapNotNull(BiologyModel::scientificName)
            if (candidates.any { !it.equals(scientificName, ignoreCase = true) }) {
                questions += choiceQuestion(
                    prompt = "What is the scientific name associated with ${concept.title}?",
                    correct = scientificName,
                    distractors = candidates,
                    explanation = "${concept.title} is identified as $scientificName.",
                    random = Random(random.nextInt())
                )
            }
        }

        val categoryDistractors = related.map(BiologyModel::categoryId)
            .filterNot { it.equals(concept.categoryId, ignoreCase = true) }
        if (categoryDistractors.isNotEmpty()) {
            questions += choiceQuestion(
                prompt = "${concept.title} belongs primarily to which biology category?",
                correct = concept.categoryId,
                distractors = categoryDistractors,
                explanation = "${concept.title} is catalogued under ${concept.categoryId}.",
                random = Random(random.nextInt())
            )
        }

        concept.tags.firstOrNull()?.let { tag ->
            val tagDistractors = related.flatMap(BiologyModel::tags)
                .filterNot { it.equals(tag, ignoreCase = true) }
            if (tagDistractors.isNotEmpty()) {
                questions += choiceQuestion(
                    prompt = "Which term is most directly associated with ${concept.title}?",
                    correct = tag,
                    distractors = tagDistractors,
                    explanation = "\"$tag\" is a catalogue keyword for ${concept.title}.",
                    random = Random(random.nextInt())
                )
            }
        }

        concept.parts.take(8).forEachIndexed { partIndex, part ->
            questions += choiceQuestion(
                prompt = "Which part is responsible for this role?\n\n${part.description}",
                correct = part.title,
                distractors = concept.parts.map(ModelPart::title) +
                    related.flatMap { model -> model.parts.map(ModelPart::title) },
                explanation = "${part.title}: ${part.description}",
                random = Random(random.nextInt() + partIndex),
                partIndex = partIndex
            )
        }

        related.take(8).forEachIndexed { index, model ->
            factsFor(model).firstOrNull()?.let { fact ->
                questions += identificationQuestion(
                    fact,
                    model,
                    catalog.filterNot { it.id == model.id },
                    Random(random.nextInt() + index * 17)
                )
            }
        }

        repeat(3) { matchIndex ->
            matchingQuestion(concept, related, matchIndex)?.let(questions::add)
        }

        var fillIndex = 0
        while (questions.distinctBy(KnowledgeQuestion::prompt).size < MINIMUM_QUESTION_COUNT) {
            val source = catalog[fillIndex % catalog.size]
            val sourceFacts = factsFor(source)
            val fact = sourceFacts[(fillIndex / catalog.size) % sourceFacts.size]
            val base = identificationQuestion(
                fact,
                source,
                catalog.filterNot { it.id == source.id },
                Random(concept.id.hashCode() + fillIndex * 101)
            )
            questions += base.copy(
                prompt = "Knowledge check ${fillIndex + 1}: ${base.prompt}"
            )
            fillIndex += 1
        }

        return questions
            .distinctBy(KnowledgeQuestion::prompt)
            .take(maxOf(MINIMUM_QUESTION_COUNT, questions.size.coerceAtMost(24)))
    }

    private fun factsFor(model: BiologyModel): List<String> {
        val theory = BiologyTheoryCatalog.forModel(model)
        return buildList {
            addAll(splitSentences(theory.coreTheory))
            add(theory.example.removePrefix("Example:").trim())
            addAll(theory.knowMore)
            model.description.takeIf(String::isNotBlank)?.let(::add)
            addAll(model.parts.map(ModelPart::description))
        }
            .map(String::trim)
            .filter { it.length >= 20 }
            .distinct()
            .ifEmpty { listOf("${model.title} is studied by relating biological structure to function.") }
    }

    private fun splitSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+"))
            .map(String::trim)
            .filter { it.length >= 20 }

    private fun identificationQuestion(
        fact: String,
        answer: BiologyModel,
        distractorModels: List<BiologyModel>,
        random: Random
    ): KnowledgeQuestion = choiceQuestion(
        prompt = "Which concept is described by this statement?\n\n$fact",
        correct = answer.title,
        distractors = distractorModels.map(BiologyModel::title),
        explanation = "${answer.title}: $fact",
        random = random
    )

    private fun correctStatementQuestion(
        concept: BiologyModel,
        correctFact: String,
        related: List<BiologyModel>,
        random: Random
    ): KnowledgeQuestion? {
        val distractors = related.mapNotNull { factsFor(it).firstOrNull() }
        if (distractors.isEmpty()) return null
        return choiceQuestion(
            prompt = "Which statement about ${concept.title} is correct?",
            correct = correctFact,
            distractors = distractors,
            explanation = correctFact,
            random = random
        )
    }

    private fun choiceQuestion(
        prompt: String,
        correct: String,
        distractors: List<String>,
        explanation: String,
        random: Random,
        partIndex: Int = -1
    ): KnowledgeQuestion {
        val selectedDistractors = distractors
            .map(String::trim)
            .filter { it.isNotBlank() && !it.equals(correct, ignoreCase = true) }
            .distinctBy(String::lowercase)
            .shuffled(random)
            .take(3)
        val choices = (selectedDistractors + correct).shuffled(random)
        return KnowledgeQuestion(
            prompt = prompt,
            choices = choices,
            correctIndex = choices.indexOf(correct),
            explanation = explanation,
            partIndex = partIndex
        )
    }

    private fun matchingQuestion(
        concept: BiologyModel,
        related: List<BiologyModel>,
        offset: Int
    ): KnowledgeQuestion? {
        val candidates = (listOf(concept) + related.drop(offset) + related.take(offset))
            .distinctBy(BiologyModel::id)
            .take(4)
        if (candidates.size < 4) return null

        val statements = candidates.map { factsFor(it).first() }
        val shuffledStatements = statements.shuffled(Random(concept.id.hashCode() + offset * 97))
        val letters = listOf("A", "B", "C", "D")
        val left = candidates.mapIndexed { index, model -> "${letters[index]}. ${model.title}" }
        val right = shuffledStatements.mapIndexed { index, statement ->
            "${index + 1}. $statement"
        }
        val correctMapping = candidates.mapIndexed { index, _ ->
            "${letters[index]}–${shuffledStatements.indexOf(statements[index]) + 1}"
        }
        val mappings = listOf(
            correctMapping,
            correctMapping.drop(1) + correctMapping.first(),
            correctMapping.reversed(),
            listOf(correctMapping[1], correctMapping[0], correctMapping[3], correctMapping[2])
        ).map { it.joinToString(", ") }.distinct()
        val choices = mappings.shuffled(Random(concept.id.hashCode() + offset * 193))
        val correct = correctMapping.joinToString(", ")
        return KnowledgeQuestion(
            prompt = buildString {
                append("Match the following:\n\n")
                append(left.joinToString("\n"))
                append("\n\n")
                append(right.joinToString("\n"))
            },
            choices = choices,
            correctIndex = choices.indexOf(correct),
            explanation = candidates.joinToString("\n") { model ->
                "${model.title} — ${factsFor(model).first()}"
            },
            kind = KnowledgeQuestionKind.MATCHING
        )
    }
}
