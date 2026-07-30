package com.indianservers.biology.data

data class Infographic(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val tags: List<String>,
    val thumbnailUrl: String?,
    val fileUrl: String,
    val mediaType: String,
    val fileSizeBytes: Long?,
    val checksumSha256: String?,
    val version: Int,
    val gradeLevels: List<String>,
    val sourceTitle: String?,
    val sourceUrl: String?,
    val reviewedAt: String?,
    val localFilePath: String? = null,
    val savedAtEpochMs: Long? = null,
    val status: InfographicDownloadStatus = InfographicDownloadStatus.NOT_SAVED,
    val progress: Float = 0f,
    val errorMessage: String? = null
) {
    val isSaved: Boolean
        get() = !localFilePath.isNullOrBlank() &&
            status in setOf(
                InfographicDownloadStatus.SAVED,
                InfographicDownloadStatus.UPDATE_AVAILABLE,
                InfographicDownloadStatus.FAILED
            )
}

enum class InfographicDownloadStatus {
    NOT_SAVED,
    QUEUED,
    DOWNLOADING,
    SAVED,
    UPDATE_AVAILABLE,
    FAILED
}

data class InfographicCatalogResult(
    val infographics: List<Infographic>,
    val source: InfographicCatalogSource,
    val warning: String? = null
)

enum class InfographicCatalogSource {
    NETWORK,
    DATABASE,
    NONE
}
