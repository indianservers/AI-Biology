package com.indianservers.biology.data

data class MicroscopySlide(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val tissue: String?,
    val organ: String?,
    val species: String?,
    val scientificName: String?,
    val stain: String?,
    val magnification: String?,
    val thumbnailUrl: String?,
    val source: MicroscopySource,
    val annotations: List<MicroscopyAnnotation>,
    val attribution: String?,
    val reviewedAt: String?
)

data class MicroscopySource(
    val type: MicroscopySourceType,
    val url: String,
    val width: Int?,
    val height: Int?
)

enum class MicroscopySourceType {
    IMAGE,
    DZI,
    IIIF
}

data class MicroscopyAnnotation(
    val id: String,
    val label: String,
    val scientificName: String?,
    val description: String,
    val challengePrompt: String,
    val x: Double,
    val y: Double,
    val radius: Double
)

data class MicroscopyProgress(
    val slideId: String,
    val bestScore: Int,
    val questionCount: Int,
    val attempts: Int
)

data class MicroscopyCatalogResult(
    val slides: List<MicroscopySlide>,
    val warning: String? = null
)
