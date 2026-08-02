package com.indianservers.AIbiology.ui

import android.graphics.BitmapFactory
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.indianservers.AIbiology.R
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.ModelDownloadRecord
import com.indianservers.AIbiology.data.ModelDownloadStatus
import com.indianservers.AIbiology.databinding.ItemModelCardBinding
import java.io.File
import java.util.concurrent.Executors

class BiologyModelAdapter(
    private val isAvailable: (BiologyModel) -> Boolean,
    private val isFavourite: (BiologyModel) -> Boolean,
    private val downloadRecord: (BiologyModel) -> ModelDownloadRecord?,
    private val thumbnailFile: (BiologyModel) -> File?,
    private val requestThumbnail: (BiologyModel, (File?) -> Unit) -> Unit,
    private val onSelected: (BiologyModel) -> Unit,
    private val onDownloadStateSelected: (BiologyModel) -> Unit = onSelected,
    private val onFavourite: (BiologyModel) -> Unit
) : ListAdapter<BiologyModel, BiologyModelAdapter.ModelViewHolder>(DIFF) {

    fun refreshFavourite(modelId: String) {
        currentList.indexOfFirst { it.id == modelId }
            .takeIf { it >= 0 }
            ?.let(::notifyItemChanged)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder =
        ModelViewHolder(
            ItemModelCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ModelViewHolder(
        private val binding: ItemModelCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: BiologyModel) {
            val available = isAvailable(model)
            val favourite = isFavourite(model)
            val record = downloadRecord(model)
            binding.modelCardTitle.text = model.title
            binding.modelCardCategory.text =
                listOfNotNull(model.categoryId, model.scientificName)
                    .joinToString("  |  ")
            val compactPhoneLandscape =
                binding.root.resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE &&
                    binding.root.resources.configuration.smallestScreenWidthDp < 600
            binding.modelCardCategory.visibility =
                if (compactPhoneLandscape) View.GONE else View.VISIBLE
            binding.modelLearningProgress.visibility =
                if (compactPhoneLandscape) View.GONE else View.VISIBLE
            binding.modelPlaceholder.text = model.badge
            binding.badgeAr.visibility = if (model.supportsAr) View.VISIBLE else View.GONE
            binding.badgePremium.visibility = if (model.isPremium) View.VISIBLE else View.GONE
            binding.favouriteButton.setImageResource(
                if (favourite) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )
            binding.favouriteButton.contentDescription =
                if (favourite) "Remove ${model.title} from favourites"
                else "Add ${model.title} to favourites"
            binding.modelLearningProgress.progress =
                (model.learningProgress.coerceIn(0f, 1f) * 100).toInt()
            binding.modelDownloadState.text = stateLabel(model, available, record)
            binding.modelDownloadState.setCompoundDrawablesWithIntrinsicBounds(
                if (binding.modelDownloadState.text == "OPEN") R.drawable.control_open else 0,
                0,
                0,
                0
            )
            binding.modelDownloadState.compoundDrawablePadding =
                if (binding.modelDownloadState.text == "OPEN") {
                    (4 * binding.root.resources.displayMetrics.density).toInt()
                } else {
                    0
                }
            val downloading = record?.status == ModelDownloadStatus.DOWNLOADING ||
                record?.status == ModelDownloadStatus.QUEUED ||
                record?.status == ModelDownloadStatus.PAUSED
            binding.modelDownloadProgress.visibility =
                if (downloading) View.VISIBLE else View.GONE
            binding.modelDownloadProgress.isIndeterminate =
                record?.status == ModelDownloadStatus.QUEUED
            binding.modelDownloadProgress.progress =
                ((record?.progress ?: 0f).coerceIn(0f, 1f) * 100).toInt()
            binding.modelDownloadState.setTextColor(
                binding.root.context.getColor(
                    when {
                        available || record?.status == ModelDownloadStatus.DOWNLOADED ->
                            R.color.model_state_ready
                        record?.status == ModelDownloadStatus.FAILED ->
                            R.color.model_state_error
                        else -> R.color.model_state_pending
                    }
                )
            )
            binding.root.contentDescription =
                "${model.title}. ${model.categoryId}. ${binding.modelDownloadState.text}."
            binding.root.setOnClickListener { onSelected(model) }
            binding.modelDownloadState.setOnClickListener { onDownloadStateSelected(model) }
            binding.modelDownloadState.contentDescription =
                "${binding.modelDownloadState.text} ${model.title}"
            binding.favouriteButton.setOnClickListener { onFavourite(model) }
            if (DeviceProfile.isTelevision(binding.root.context)) {
                binding.modelCardTitle.textSize = 17f
                binding.modelCardCategory.textSize = 12f
                binding.modelDownloadState.textSize = 12f
                TvFocus.apply(binding.root)
                TvFocus.apply(binding.favouriteButton, focusedScale = 1.03f)
            }
            loadThumbnail(model)
        }

        private fun loadThumbnail(model: BiologyModel) {
            binding.modelThumbnail.setImageDrawable(null)
            binding.modelThumbnail.tag = model.id
            binding.modelPlaceholder.visibility = View.VISIBLE
            val file = thumbnailFile(model)?.takeIf(File::isFile)
            if (file == null) {
                requestThumbnail(model) { downloaded ->
                    if (downloaded?.isFile == true && binding.modelThumbnail.tag == model.id) {
                        decodeThumbnail(model, downloaded)
                    }
                }
                return
            }
            decodeThumbnail(model, file)
        }

        private fun decodeThumbnail(model: BiologyModel, file: File) {
            THUMBNAIL_EXECUTOR.execute {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@execute
                binding.modelThumbnail.post {
                    if (binding.modelThumbnail.tag == model.id) {
                        binding.modelThumbnail.setImageBitmap(bitmap)
                        binding.modelPlaceholder.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun stateLabel(
        model: BiologyModel,
        available: Boolean,
        record: ModelDownloadRecord?
    ): String {
        if (available) {
            return "OPEN"
        }
        return when (record?.status) {
            ModelDownloadStatus.QUEUED -> "QUEUED"
            ModelDownloadStatus.DOWNLOADING ->
                "PAUSE  |  ${(record.progress * 100).toInt()}%"
            ModelDownloadStatus.PAUSED ->
                "RESUME  |  ${(record.progress * 100).toInt()}%"
            ModelDownloadStatus.UPDATE_AVAILABLE -> "UPDATE AVAILABLE"
            ModelDownloadStatus.FAILED -> "RETRY DOWNLOAD"
            ModelDownloadStatus.DOWNLOADED -> "OPEN"
            else -> (model.packageSizeBytes ?: model.fileSizeBytes)?.let {
                "DOWNLOAD  |  ${formatBytes(it)}"
            } ?: if (model.glbUrl.isNullOrBlank() && model.packageUrl.isNullOrBlank()) {
                "ONLINE SOURCE PENDING"
            } else {
                "DOWNLOAD MODEL"
            }
        }
    }

    companion object {
        private val THUMBNAIL_EXECUTOR = Executors.newFixedThreadPool(2)
        private val DIFF = object : DiffUtil.ItemCallback<BiologyModel>() {
            override fun areItemsTheSame(oldItem: BiologyModel, newItem: BiologyModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BiologyModel, newItem: BiologyModel) =
                oldItem == newItem
        }

        fun formatBytes(bytes: Long): String =
            when {
                bytes >= 1024L * 1024L * 1024L ->
                    "%.1f GB".format(bytes / (1024f * 1024f * 1024f))
                bytes >= 1024L * 1024L ->
                    "%.1f MB".format(bytes / (1024f * 1024f))
                else -> "%.0f KB".format(bytes / 1024f)
            }
    }

}
