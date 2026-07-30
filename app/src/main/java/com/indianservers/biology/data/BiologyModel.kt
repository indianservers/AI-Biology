package com.indianservers.biology.data

data class BiologyModel(
    val fileName: String,
    val title: String,
    val shortTitle: String,
    val badge: String,
    val parts: List<ModelPart>,
    val id: String = fileName.substringBeforeLast(".").lowercase().replace(" ", "-"),
    val alternativeNames: List<String> = emptyList(),
    val scientificName: String? = null,
    val description: String = "",
    val categoryId: String = BiologyCategories.ORGANELLES,
    val tags: List<String> = emptyList(),
    val system: String? = null,
    val thumbnailUrl: String? = null,
    val localThumbnailRes: Int? = null,
    val glbUrl: String? = null,
    val manifestUrl: String? = null,
    val packageUrl: String? = null,
    val packageSizeBytes: Long? = null,
    val packageChecksumSha256: String? = null,
    val localGlbPath: String? = null,
    val fileSizeBytes: Long? = null,
    val checksumSha256: String? = null,
    val version: Int = 1,
    val supportsAr: Boolean = false,
    val supportsAnimations: Boolean = false,
    val supportsExplodedView: Boolean = false,
    val supportsSectionView: Boolean = false,
    val supportsPartSelection: Boolean = true,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val isFavourite: Boolean = false,
    val learningProgress: Float = 0f,
    val gradeLevels: List<String> = listOf("Beginner", "Student", "Advanced"),
    val isPremium: Boolean = false,
    val addedAtEpochMs: Long = 0L,
    val viewCount: Int = 0
)

data class ModelPart(
    val id: String,
    val nodeNames: List<String>,
    val title: String,
    val scientificName: String? = null,
    val shortDescription: String,
    val detailedDescription: String? = null,
    val parentPartId: String? = null,
    val audioUrl: String? = null,
    val animationName: String? = null,
    val cameraPreset: CameraPreset? = null,
    val selectable: Boolean = true,
    val position: String,
    val normal: String = "0 0 1",
    val hitNodeNames: List<String> = emptyList()
) {
    val description: String
        get() = shortDescription
}

data class CameraPreset(
    val key: String,
    val shortLabel: String,
    val title: String,
    val orbit: String? = null,
    val target: String? = null
)

enum class ModelDownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    UPDATE_AVAILABLE,
    FAILED
}

data class ModelDownloadRecord(
    val modelId: String,
    val localFilePath: String?,
    val downloadDateEpochMs: Long?,
    val lastOpenedDateEpochMs: Long?,
    val fileSizeBytes: Long,
    val version: Int,
    val checksumSha256: String?,
    val status: ModelDownloadStatus,
    val explicitlySaved: Boolean,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

typealias AnatomyPart = ModelPart
