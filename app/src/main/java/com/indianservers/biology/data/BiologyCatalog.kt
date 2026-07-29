package com.indianservers.biology.data

object BiologyCategories {
    const val ALL = "All"
    const val CELLS = "Cells"
    const val ORGANELLES = "Organelles"
    const val HUMAN_BODY = "Human Body"
    const val PLANTS = "Plants"
    const val MICROBIOLOGY = "Microbiology"
    const val GENETICS = "Genetics"
    const val ANIMALS = "Animals"
    const val ECOLOGY = "Ecology"

    val all = listOf(
        ALL,
        CELLS,
        ORGANELLES,
        HUMAN_BODY,
        PLANTS,
        MICROBIOLOGY,
        GENETICS,
        ANIMALS,
        ECOLOGY
    )
}

enum class ModelSort(val label: String) {
    RECOMMENDED("Recommended"),
    RECENTLY_ADDED("Recently added"),
    MOST_VIEWED("Most viewed"),
    A_TO_Z("A-Z"),
    RECENTLY_VIEWED("Recently viewed"),
    DOWNLOADED("Downloaded")
}

object BiologyCatalogQuery {
    const val PAGE_SIZE = 20

    fun filter(
        models: List<BiologyModel>,
        query: String,
        category: String,
        sort: ModelSort,
        downloadedIds: Set<String>,
        recentIds: List<String>,
        limit: Int
    ): List<BiologyModel> {
        val normalized = query.trim().lowercase()
        val filtered = models.asSequence()
            .filter { category == BiologyCategories.ALL || it.categoryId == category }
            .filter { model ->
                normalized.isBlank() || searchableText(model).contains(normalized)
            }
            .filter { model -> sort != ModelSort.DOWNLOADED || model.id in downloadedIds }
            .toList()

        val recentOrder = recentIds.withIndex().associate { it.value to it.index }
        return when (sort) {
            ModelSort.RECOMMENDED -> filtered.sortedWith(
                compareByDescending<BiologyModel> { it.learningProgress }
                    .thenByDescending { it.viewCount }
                    .thenBy { it.title }
            )
            ModelSort.RECENTLY_ADDED -> filtered.sortedByDescending(BiologyModel::addedAtEpochMs)
            ModelSort.MOST_VIEWED -> filtered.sortedByDescending(BiologyModel::viewCount)
            ModelSort.A_TO_Z -> filtered.sortedBy(BiologyModel::title)
            ModelSort.RECENTLY_VIEWED ->
                filtered.sortedBy { recentOrder[it.id] ?: Int.MAX_VALUE }
            ModelSort.DOWNLOADED ->
                filtered.sortedBy(BiologyModel::title)
        }.take(limit)
    }

    fun searchableText(model: BiologyModel): String =
        buildList {
            add(model.title)
            add(model.shortTitle)
            addAll(model.alternativeNames)
            model.scientificName?.let(::add)
            add(model.categoryId)
            addAll(model.tags)
            model.system?.let(::add)
            addAll(model.gradeLevels)
        }.joinToString(" ").lowercase()

    fun semanticPartForNode(model: BiologyModel, nodeName: String): ModelPart? {
        val exact = model.parts.filter { part ->
            nodeName in part.nodeNames || nodeName in part.hitNodeNames
        }
        return exact
            .filter(ModelPart::selectable)
            .minByOrNull { it.nodeNames.size + it.hitNodeNames.size }
    }
}
