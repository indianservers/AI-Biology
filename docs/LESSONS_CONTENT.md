# Lessons content

The Lessons module stores curriculum content in
`app/src/main/java/com/indianservers/AIbiology/data/LessonCatalog.kt`.
This phase intentionally keeps the content separate from the visual design.

## Curriculum structure

The catalogue follows the supplied Grade 6–Postgraduate biology knowledge map:

- 44 numbered curriculum areas
- five learner levels: Grade 6–8, Grade 9–10, Grade 11–12, UG, and PG
- stable IDs for every area and lesson concept
- English (`en`) as the first saved language

An area is the large subject, such as **Cell Biology**. Its `concepts` list contains
the smaller teachable concepts and subconcepts, such as **Cell Theory**,
**Nucleus**, and **Mitochondria**.

## Content saved for every lesson

Each `Lesson` has three progressive reading phases:

1. **Start simple** — a short explanation, a familiar example, and one useful fact.
2. **Understand connections** — how the idea connects to other biological ideas.
3. **Explore deeper** — advanced mechanisms, evidence, limitations, or applications.

The same source material is also saved as `LessonLearningContent` for the future
lesson design:

- `detailedExplanation`
- `easyWayToLearn`
- `realLifeExamples`
- `importantPoints`
- `commonMistake`
- `quickCheckQuestion`
- `quickCheckAnswer`

The easy-learning method uses four repeatable steps: **Say it, See it, Link it,
Test it**. This gives learners with weak reading skills a predictable routine for
every concept.

## Content-writing rules

- Prefer short sentences and common words.
- Explain a new technical word when it first appears.
- Begin with a real object or familiar event whenever possible.
- Move from visible examples to hidden mechanisms.
- Never ask learners to memorize a name without its meaning.
- Keep school-level content accurate while adding an optional advanced layer.
- Avoid shaming labels for learners; describe the support they need instead.

## Next design phase

The mockup can decide whether the saved blocks appear as tabs, cards, accordions,
audio prompts, printable notes, or a combination. No content migration should be
needed because the UI can read the structured fields directly.
